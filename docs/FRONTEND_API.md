# CheChe 프론트엔드 API 연동 명세

프론트엔드는 개별 마이크로서비스가 아닌 Gateway `http://localhost:8080`만 호출합니다.

- 요청/응답 JSON 인코딩: UTF-8
- 날짜 형식: ISO 8601 (`2026-09-09T18:30:00`)
- 인증 헤더: `Authorization: Bearer <accessToken>`
- `X-User-Id`, `X-User-Role`, `X-User-Region`은 Gateway가 생성하는 내부 헤더입니다. 프론트에서 전송하지 않습니다.
- 전체 기계 판독용 명세: [`openapi.yaml`](./openapi.yaml)

## 관리자 프론트엔드 빠른 연동

### 환경변수와 공통 요청 함수

Next.js 기준 환경변수 예시입니다.

```dotenv
NEXT_PUBLIC_CHECHE_API_BASE_URL=http://localhost:8080
```

```ts
const API_BASE_URL =
  process.env.NEXT_PUBLIC_CHECHE_API_BASE_URL ?? 'http://localhost:8080';

export async function adminApi<T>(
  path: string,
  accessToken: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${accessToken}`);
  if (!(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem('checheAdminToken');
    window.location.href = '/admin/login';
    throw new Error('로그인이 만료되었습니다.');
  }
  if (response.status === 428) {
    window.location.href = '/admin/setup-region';
    throw new Error('담당 지역 설정이 필요합니다.');
  }
  if (!response.ok) throw new Error(`CheChe API 오류: ${response.status}`);

  return response.json() as Promise<T>;
}
```

클라이언트는 `X-User-Id`, `X-User-Role`, `X-User-Region`을 만들지 않습니다. Gateway가 JWT와 최신 관리자 DB 정보를 확인한 후 내부 요청에만 추가합니다.

### 권장 관리자 화면과 호출 순서

| 화면 | 선행 조건 | 호출 API |
|---|---|---|
| `/admin/login` | 없음 | `POST /auth/admin/login` |
| `/admin/setup-region` | `initialSetupRequired=true` | `GET /api/admins/regions`, `PUT /api/admins/me/region` |
| `/admin` | 관리자 JWT | `GET /api/admins/me`, `GET /api/inspections/dashboard` |
| `/admin/facilities` | 지역 설정 완료 | `GET /api/facilities`, `POST /api/facilities/public-data/sync` |
| `/admin/facilities/{id}` | 시설 선택 | `GET /api/facilities/{id}`, `GET /api/inspections/facilities/{id}/history` |
| `/admin/inspections/new` | 시설 선택 | `POST /api/inspections` |
| `/admin/inspections` | 지역 설정 완료 | `GET /api/inspections`, `GET /api/inspections/open` |
| `/admin/admins` | `SUPER_USER` | `GET /api/admins`, `PATCH /api/admins/{id}/authority` |

관리자 로그인 이후 권장 흐름은 다음과 같습니다.

1. `accessToken`을 관리자 전용 저장소에 보관합니다.
2. `initialSetupRequired=true`이면 서울 자치구 선택 화면으로 이동합니다.
3. 지역 설정 후 `GET /api/admins/me`를 다시 호출합니다. JWT를 재발급할 필요는 없습니다.
4. 시설 화면에서 공공데이터 동기화를 한 번 실행한 후 시설 목록을 다시 조회합니다.
5. 시설 상세에서 사진 점검을 등록하고 점검 이력과 조치 상태를 관리합니다.

## 1. 로그인 화면 분기

### 사용자 로그인

`POST /auth/user/login`

사용자 회원가입은 `POST /auth/user/register`, 로그인은 `POST /auth/user/login`을 사용합니다.
로그인 응답의 `initialSetupRequired`가 `true`이면 지역 설정 화면으로 이동합니다.

```json
{
  "userId": 20,
  "username": "sports-user",
  "accountType": "USER",
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresInSeconds": 3600,
  "regionCode": null,
  "regionName": null,
  "initialSetupRequired": true,
  "message": "지역 설정이 필요합니다."
}
```

### 사용자 지역 설정

- `GET /api/users/regions`: 선택 가능한 서울 25개 자치구
- `GET /api/users/me`: 내 프로필
- `PUT /api/users/me/region`: 내 지역 설정·변경

```json
{ "regionCode": "11680" }
```

지역 설정 전 `/api/user/**`를 호출하면 `428 Precondition Required`가 반환됩니다.
지역 설정 후 Gateway가 `X-User-Region`을 생성하므로 프론트에서 지역 헤더를 전송하지 않습니다.

### 사용자 시설 탐색

- `GET /api/user/facilities/home`: 설정 지역의 추천 시설
- `POST /api/user/facilities/search`: 설정 지역 자연어 검색
- `GET /api/user/facilities/{id}`: 시설 상세
- `GET /api/user/facilities/{id}/usage-guide`: 예약·이용 안내

Figma 사용자 화면 연동을 위해 홈 응답에는 다음 필드가 추가됩니다.

- `aiExamplePrompt`: AI 검색창 예시 문구
- `quickSports`: 축구, 배드민턴, 수영, 농구 빠른 메뉴
- `recommendations`: 사용자 지역 추천 시설
- `kspoFacilities`: 국민체육진흥공단 공식 운영시설

시설 카드에는 `usageFee`, `nextAvailableTime`, `favorite`, `tags`가 포함됩니다.
정확한 위치 좌표가 확보되지 않은 시설의 `distanceKm`는 임의 값 대신 `null`입니다.
홈·검색·상세·즐겨찾기 요청에 `latitude`, `longitude` 쿼리를 함께 보내면 저장된 시설 좌표를 기준으로
`distanceKm`를 계산하며, 홈과 즐겨찾기는 가까운 순으로 정렬됩니다. 사진이 없으면
`/images/facility-default.svg`가 반환됩니다.

검색 요청:

```json
{ "query": "강남에서 수영할 수 있는 곳" }
```

검색 응답의 `conditions`에는 Figma의 **AI 정리 키워드** 영역에 사용할 `region`, `sport`,
`time`, `reservationAvailableOnly`가 포함됩니다. `assistantMessage`는 AI 대화 말풍선,
`recommendedFacility`는 조건에 가장 잘 맞는 추천 시설 카드에 사용합니다.

### 사용자 시설 즐겨찾기

- `GET /api/user/facilities/favorites`: 내 관심 시설 목록
- `POST /api/user/facilities/{id}/favorite`: 관심 시설 등록
- `DELETE /api/user/facilities/{id}/favorite`: 관심 시설 해제

시설 상세 응답에는 `favorite`, 평일·주말 운영시간, `usageFee`, 원문 요금 안내(`feeInfo`),
`capacity`, `applicationMethod`, `closedDays`, `imageUrl`, `latitude`, `longitude`,
`availableFacilities`, `amenities`, `reservationOptionsPath`가 포함됩니다.

`usageFee`는 1인 예약 요금(원)입니다. 시설 공공데이터 또는 관리자가 등록한 요금을 우선 사용하고,
원본 요금이 확인되지 않은 시설은 MVP 정책 요금 `3333`을 반환합니다. `feeInfo`는 제공기관의
원문 요금 안내이며, `feeInfo=null`이면 화면에 “MVP 기본 요금”으로 표시해 주세요. `0`은 무료로
확인된 시설에만 사용합니다.

시설 검색 결과는 CheChe DB와 서울 열린데이터광장의 `ListPublicReservationSport` 결과를 합쳐 반환합니다.
공공 API 항목은 `source`가 `SEOUL_OPEN_API`이며 `externalId`, `phone`, `imageUrl`,
`openingTime`, `closingTime`, `statusLabel`을 포함합니다. 동일 지역·시설명·종목 데이터는 한 건으로 합칩니다.

전체 서울 데이터를 사용하려면 실행 환경에 `SEOUL_OPEN_API_KEY`를 설정합니다.
키가 없을 때 사용하는 `sample` 키는 일부 테스트 데이터만 반환합니다. 공공 API 장애 시에는
CheChe DB 결과만 반환하므로 사용자 검색 전체가 실패하지 않습니다.

국민체육진흥공단의 `스포츠가치센터 운영시설 이용회차 정보`도 함께 조회합니다.
KSPO 항목은 `source`가 `KSPO_OPEN_API`이며 시설별 회차 정보를 묶어 운영 시작·종료시간과
회차 최대 수용인원을 제공합니다. 홈 응답의 `kspoFacilities`에서 공식 시설을 별도 표시하며,
자연어 검색 결과에도 포함됩니다.

공공데이터포털에서 해당 API 활용신청 후 발급된 **일반 인증키(Decoding)** 를
`KSPO_OPEN_API_KEY`에 설정해야 실제 데이터가 표시됩니다. 키가 없거나 API 장애가 발생하면
KSPO 목록만 생략하고 CheChe DB와 서울시 검색은 계속 동작합니다.

### 시설 개선 요청

`POST /api/user/reports`는 `multipart/form-data` 요청입니다.

| 필드 | 필수 | 설명 |
|---|---:|---|
| `facilityId` | O | 이용한 시설 ID |
| `category` | O | `DETERIORATION`, `IMPROVEMENT`, `REPAIR`, `OTHER` |
| `locationDescription` | O | 시설 내 발견 위치, 최대 240자 |
| `comment` | O | 개선·수리 요청 내용, 최대 2,000자 |
| `photo` | O | 이미지 파일, 최대 15MB |

- `GET /api/user/reports`: 내가 작성한 요청 목록
- `GET /api/user/reports/{id}`: 내가 작성한 요청 상세

처리 상태는 `RECEIVED`, `REVIEWING`, `REPAIR_SCHEDULED`, `COMPLETED`, `REJECTED`입니다.
사용자는 본인이 작성한 요청만 볼 수 있습니다.
운영 종료(`CLOSED`) 시설도 과거 이용 중 발견한 문제를 신고할 수 있습니다. 다만 사용자 지역과
시설 지역이 다르거나 시설이 없으면 각각 `403`, `404`를 반환합니다. 시설 서비스 장애일 때만
`502`를 반환합니다. 프론트는 오류 응답의 `message`를 사용자 안내 문구로 표시할 수 있습니다.

### 사용자 시설 예약

- `GET /api/user/reservations/availability?facilityId=1&date=2026-10-01`: 예약 가능한 정각 시간 조회
- `GET /api/user/reservations/options?facilityId=1&date=2026-10-01`: Figma 예약 화면용 날짜·시간·요금 옵션
- `GET /api/user/reservations/checkout?facilityId=1`: 내부 예약·제공기관 예약 주소와 결제 지원 상태
- `POST /api/user/reservations`: 1시간 단위 예약 생성
- `GET /api/user/reservations`: 내 예약 목록
- `GET /api/user/reservations/{id}`: 내 예약 상세
- `PATCH /api/user/reservations/{id}/cancel`: 시작 전 예약 취소

예약 생성 요청:

```json
{
  "facilityId": 1,
  "reservationDate": "2026-10-01",
  "startTime": "19:00:00",
  "participantCount": 2
}
```

예약은 운영 중인 CheChe 등록 시설에서만 가능하며, 사용자가 설정한 지역의 시설이어야 합니다.
공식 데이터에 운영시간·요금·수용인원이 있으면 해당 값을 사용합니다. 요금이 확인되지 않은 시설은
MVP 정책 요금 1인 `3,333원`을 시설 상세, 예약 옵션, 결제 확인, 실제 예약에 동일하게 적용합니다.
정각 기준 1시간 단위로 예약할 수 있습니다. 시설 수용인원에서 같은 시간대의 확정 예약 인원을
차감하며 잔여 인원을 초과한 예약은 거절합니다. 시설 행 잠금으로 동시 예약도 순차 처리합니다.

`options` 응답은 오늘부터 5일의 날짜 선택지와 시설 운영시간 안의 예약 시간대를 반환합니다.
시간 상태는 `AVAILABLE`, `RESERVED`, `CLOSED`입니다. 공식 요금이 없으면 최상위와 각 시간대의
`pricePerPerson`은 MVP 정책값 `3333`입니다.
각 시간 항목에는 `capacity`, `reservedParticipants`, `remainingCapacity`가 포함됩니다.
예약 생성 응답에는 `pricePerPerson`과 `totalFee`가 포함됩니다.

예약 옵션 응답의 정확한 필드명:

| 위치 | 필드명 | 타입 | 설명 |
|---|---|---|---|
| 최상위 | `facilityId` | `number` | 시설 ID |
| 최상위 | `facilityName` | `string` | 시설명 |
| 최상위 | `facilityType` | `string` | 시설 종목·유형 |
| 최상위 | `selectedDate` | `YYYY-MM-DD` | 현재 선택 날짜 |
| 최상위 | `pricePerPerson` | `number` | 1인 예약 요금(원). 원본 미확인 시 `3333` |
| 최상위 | `minParticipants` | `number` | 최소 이용 인원 |
| 최상위 | `maxParticipants` | `number` | 최대 이용 인원 |
| 최상위 | `dates` | `ReservationDateOption[]` | 날짜 선택 목록 |
| 최상위 | `timeSlots` | `ReservationTimeSlot[]` | 선택 날짜의 시간 목록 |
| `dates[]` | `date` | `YYYY-MM-DD` | 날짜 값 |
| `dates[]` | `dayOfWeek` | `string` | 요일 한글명 |
| `dates[]` | `dayLabel` | `string` | 화면 표시용 날짜 라벨 |
| `dates[]` | `available` | `boolean` | 예약 가능한 시간이 하나 이상 있는지 여부 |
| `timeSlots[]` | `startTime` | `HH:mm:ss` | 시작 시간 |
| `timeSlots[]` | `endTime` | `HH:mm:ss` | 종료 시간 |
| `timeSlots[]` | `status` | `string` | `AVAILABLE`, `RESERVED`, `CLOSED` |
| `timeSlots[]` | `statusLabel` | `string` | 화면 표시용 상태 문구 |
| `timeSlots[]` | `pricePerPerson` | `number` | 해당 시간대 1인 요금. 원본 미확인 시 `3333` |
| `timeSlots[]` | `capacity` | `number` | 전체 수용 인원 |
| `timeSlots[]` | `reservedParticipants` | `number` | 예약 완료 인원 |
| `timeSlots[]` | `remainingCapacity` | `number` | 현재 예약 가능 인원 |

```json
{
  "facilityId": 1,
  "facilityName": "올림픽공원 체육센터",
  "facilityType": "배드민턴장",
  "selectedDate": "2026-10-02",
  "pricePerPerson": 5000,
  "minParticipants": 1,
  "maxParticipants": 20,
  "dates": [
    {
      "date": "2026-10-02",
      "dayOfWeek": "금",
      "dayLabel": "02",
      "available": true
    }
  ],
  "timeSlots": [
    {
      "startTime": "19:00:00",
      "endTime": "20:00:00",
      "status": "AVAILABLE",
      "statusLabel": "잔여 4명",
      "pricePerPerson": 5000,
      "capacity": 10,
      "reservedParticipants": 6,
      "remainingCapacity": 4
    }
  ]
}
```

간단한 시간 조회 API인 `availability`의 필드명은 `facilityId`, `reservationDate`, `availableStartTimes`입니다.

`checkout`은 공공데이터에 제공기관 홈페이지가 있으면 `externalReservationUrl`을 반환합니다.
실결제는 PG사 상점키가 설정되기 전까지 `onlinePaymentAvailable=false`입니다.
`checkout.pricePerPerson`도 같은 요금 정책을 사용하며 원본 미확인 시 `3333`입니다.

### 관리자 로그인

`POST /auth/admin/login`

MVP 고정 슈퍼관리자 계정:

```text
아이디: superadmin
비밀번호: superadmin
```

login-service가 시작될 때 계정이 자동 생성되며 관리자 로그인 시 `role=SUPER_USER`,
`initialSetupRequired=false`, `regionCode=null`로 반환됩니다. 지역 선택 화면으로 이동시키지 말고
슈퍼관리자 대시보드로 바로 이동합니다. 이 계정은 MVP 전용이므로 운영 배포 전 제거해야 합니다.

```json
{
  "username": "seoul-admin",
  "password": "password123"
}
```

성공 응답:

```json
{
  "userId": 1,
  "username": "seoul-admin",
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresInSeconds": 3600,
  "role": "REGIONAL_ADMIN",
  "regionCode": null,
  "regionName": null,
  "initialSetupRequired": true
}
```

프론트 분기:

```ts
const login = await loginAdmin(username, password);

if (login.initialSetupRequired) {
  // 최초 지역 설정 화면
  router.replace('/admin/setup-region');
} else {
  // 관리자 대시보드
  router.replace('/admin');
}
```

`accessToken`에는 `userId`, `username`, `accountType`만 들어갑니다. 지역, 관리자 역할, 정지 상태는 Gateway가 관리자 DB에서 요청할 때마다 다시 조회하므로 프론트에서 토큰을 재발급할 필요가 없습니다.

## 2. 인증 API

| Method | Path | 인증 | 설명 |
|---|---|---:|---|
| POST | `/auth/admin/register` | 불필요 | 관리자 회원가입 |
| POST | `/auth/admin/login` | 불필요 | 관리자 로그인 및 최초 설정 여부 반환 |
| POST | `/auth/user/login` | 불필요 | 사용자 로그인만 수행 |

아이디는 4~50자의 영문, 숫자, `.`, `_`, `-`만 사용할 수 있습니다. 비밀번호는 8~72자입니다.

## 3. 관리자 API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/admins/me` | 관리자 | 현재 관리자 프로필 |
| GET | `/api/admins/regions` | 관리자 | 선택 가능한 서울 25개 자치구 |
| PUT | `/api/admins/me/region` | 지역 관리자 | 최초 담당 지역 설정 |
| GET | `/api/admins` | 슈퍼관리자 | 모든 관리자 조회 |
| PATCH | `/api/admins/{id}/authority` | 슈퍼관리자 | 역할, 상태, 담당 지역 변경 |

최초 지역 설정:

```http
PUT /api/admins/me/region
Authorization: Bearer eyJ...
Content-Type: application/json
```

```json
{
  "regionCode": "11680",
  "regionName": "서울특별시 강남구"
}
```

지역 관리자는 최초 설정만 직접 할 수 있습니다. 이미 설정된 지역의 변경은 슈퍼관리자가 수행합니다.
지역 선택 화면은 `/api/admins/regions` 응답을 사용합니다. 서버는 `regionCode`를 기준으로 공식 지역명을 저장하므로 임의의 서울 외 지역은 등록할 수 없습니다.

슈퍼관리자 권한 변경 요청:

```json
{
  "role": "REGIONAL_ADMIN",
  "status": "ACTIVE",
  "regionCode": "11680",
  "regionName": "서울특별시 강남구"
}
```

역할: `REGIONAL_ADMIN`, `SUPER_USER`
상태: `ACTIVE`, `SUSPENDED`

## 4. 체육시설 API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/facilities` | 관리자 | 권한 범위 시설 목록 |
| GET | `/api/facilities/{id}` | 관리자 | 시설 상세 |
| POST | `/api/facilities` | 관리자 | 시설 등록 |
| PUT | `/api/facilities/{id}` | 관리자 | 시설 수정 |
| POST | `/api/facilities/public-data/sync` | 관리자 | 공단의 서울 공공체육시설 정보를 동기화 |

지역 관리자는 본인의 `regionCode`와 동일한 시설만 조회·등록·수정할 수 있습니다. 슈퍼관리자는 전 지역을 조회할 수 있습니다.

시설 저장 요청:

```json
{
  "name": "강남구민체육관",
  "type": "다목적체육관",
  "regionCode": "11680",
  "regionName": "서울특별시 강남구",
  "address": "서울특별시 강남구 체육관로 1",
  "phone": "02-0000-0000",
  "status": "OPERATING",
  "publicNotice": "정상 운영 중"
}
```

시설 상태: `OPERATING`, `UNDER_INSPECTION`, `CLOSED`

### 국민체육진흥공단·공공시설 개방정보 동기화

MVP 적용 범위는 서울특별시 25개 자치구입니다. 지역 관리자가 실행하면 관리자 DB의 담당 지역 코드를 기준으로 해당 자치구만 요청하고, 슈퍼관리자가 실행하면 서울특별시 전체를 요청합니다.
공단 시설 목록과 공공데이터포털의 `전국공공시설개방정보표준데이터`를 결합해 시설을 생성·갱신합니다.
표준데이터의 운영시간, 요금, 수용인원, 부대시설, 신청방법, 사진, 위도·경도는 `facilities` 테이블에 저장되며 사용자 시설 상세와 예약 옵션에 반영됩니다.
공단 API 인증키가 없더라도 표준데이터 동기화는 계속 실행됩니다.

```http
POST /api/facilities/public-data/sync
Authorization: Bearer eyJ...
```

```json
{
  "provider": "국민체육진흥공단",
  "regionCode": "11",
  "regionName": "서울특별시",
  "scannedCount": 520,
  "matchedCount": 520,
  "createdCount": 520,
  "updatedCount": 0
}
```

위 응답은 슈퍼관리자 호출 예시이며 실제 건수는 공단 데이터에 따라 달라집니다. 지역 관리자는 자신의 자치구 코드와 이름을 응답으로 받습니다. 동일 외부 시설은 중복 생성하지 않고 이름·주소·전화번호·홈페이지 URL만 갱신하며, 관리자가 설정한 운영 상태와 공개 안내는 유지됩니다. `DATA_GO_KR_SERVICE_KEY`가 없거나 제공기관 호출이 실패하면 `503 Service Unavailable`을 반환합니다.

## 5. 안전점검 API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/inspections` | 관리자 | 권한 범위 점검 목록 |
| POST | `/api/inspections` | 관리자 | 사진 점검 접수 |
| GET | `/api/inspections/facilities/{facilityId}/history` | 관리자 | 시설별 안전 이력 |
| GET | `/api/inspections/open` | 관리자 | 미조치 목록 |
| GET | `/api/inspections/dashboard` | 관리자 | 점검 집계 |
| PATCH | `/api/inspections/{id}/action` | 관리자 | 조치 상태 변경 |
| PATCH | `/api/inspections/{id}/confirmation` | 관리자 | AI 분석 수정 및 최종 확정 |
| GET | `/api/inspections/super/regions/safety` | 슈퍼관리자 | 지역별 안전 점수 집계 |
| GET | `/api/inspections/super/recurring-defects?minimumOccurrences=2` | 슈퍼관리자 | 시설·결함 유형별 반복 결함 집계 |
| GET | `/api/inspections/{id}/report` | 관리자 | 텍스트 보고서 다운로드 |
| GET | `/api/inspections/public/facilities/{facilityId}/status` | 공개 | 이용자 공개용 조치 현황 |

사진 점검은 `multipart/form-data`입니다.

```ts
const form = new FormData();
form.append('facilityId', String(facility.id));
form.append('facilityName', facility.name);
form.append('facilityRegionCode', facility.regionCode);
form.append('facilityRegionName', facility.regionName);
form.append('locationDescription', '2층 관중석 서쪽 벽면');
form.append('note', '지난 점검보다 길이가 늘어난 것으로 보임');
form.append('suspectedDefect', '균열');
form.append('photo', file);

const inspection = await adminApi<Inspection>(
  '/api/inspections',
  accessToken,
  {
  method: 'POST',
  body: form,
  },
);
```

`FormData` 요청에는 브라우저가 boundary를 생성해야 하므로 `Content-Type`을 직접 지정하지 않습니다.

등록 응답의 `photoUrl`은 Gateway 기준 상대 경로입니다. 관리자 화면에서는 다음처럼 표시합니다.

```tsx
const photoSrc = new URL(inspection.photoUrl, API_BASE_URL).toString();

<img src={photoSrc} alt={`${inspection.facilityName} 점검 사진`} />
```

현재 사진 URL은 별도 인증 없이 조회할 수 있으며 로컬 `uploads` 디렉터리에 저장됩니다. 운영 환경에서는 객체 저장소와 접근 제어 방식으로 교체하는 것을 권장합니다.

점검 등록 응답 예시:

```json
{
  "id": 31,
  "facilityId": 12,
  "facilityName": "강남구민체육관",
  "regionCode": "11680",
  "regionName": "서울특별시 강남구",
  "reporterUserId": 1,
  "photoUrl": "/inspection-photos/uuid-crack.jpg",
  "locationDescription": "2층 관중석 서쪽 벽면",
  "note": "지난 점검보다 길이가 늘어남",
  "defectType": "CRACK",
  "severity": "MEDIUM",
  "confidence": 0.72,
  "checklist": ["균열 길이와 폭 측정", "주변 누수 여부 확인"],
  "similarCases": [],
  "reportSummary": "벽면 균열 의심 부위가 접수되었습니다.",
  "actionStatus": "REPORTED",
  "actionNote": null,
  "resolvedAt": null,
  "createdAt": "2026-09-17T14:30:00",
  "updatedAt": "2026-09-17T14:30:00"
}
```

결함 유형: `CRACK`, `CORROSION`, `DEFORMATION`, `SURFACE_DAMAGE`, `WATER_LEAK`, `OTHER`
위험도: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
조치 상태: `REPORTED`, `REVIEWING`, `ACTION_SCHEDULED`, `RESOLVED`

조치 상태 변경:

```json
{
  "status": "ACTION_SCHEDULED",
  "actionNote": "2026-09-15 보수 공사 예정"
}
```

결함 분석 최종 확정 요청 예시:

```json
{
  "defectType": "CRACK",
  "severity": "HIGH",
  "locationDescription": "배드민턴장 A 서쪽 벽면",
  "detail": "균열 길이 32cm 확인",
  "actionRequired": true,
  "actionDueDate": "2026-10-15"
}
```

`actionRequired`가 `true`이면 오늘 이후의 `actionDueDate`가 필수이며 상태가 `ACTION_SCHEDULED`로 바뀝니다. `false`이면 조치 불필요 확정으로 보고 `RESOLVED`가 됩니다. 지역 안전 점수는 미해결 결함마다 `LOW 2`, `MEDIUM 5`, `HIGH 10`, `CRITICAL 20`점을 차감하며 최저 점수는 0점입니다.

프론트 호출 예시:

```ts
const confirmed = await adminApi<Inspection>(
  `/api/inspections/${inspectionId}/confirmation`,
  accessToken,
  {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      defectType: 'CRACK',
      severity: 'HIGH',
      locationDescription: '배드민턴장 A 서쪽 벽면',
      detail: '균열 길이 32cm 확인',
      actionRequired: true,
      actionDueDate: '2026-10-15',
    }),
  },
);
```

확정 응답에는 다음 필드가 추가됩니다.

```json
{
  "confirmed": true,
  "confirmedAt": "2026-10-01T14:20:00",
  "confirmedByUserId": 7,
  "confirmedDetail": "균열 길이 32cm 확인",
  "actionRequired": true,
  "actionDueDate": "2026-10-15",
  "actionStatus": "ACTION_SCHEDULED"
}
```

슈퍼관리자 지역별 안전 점수 응답 예시:

```json
[
  {
    "regionCode": "11680",
    "regionName": "서울특별시 강남구",
    "facilityCount": 24,
    "totalInspections": 31,
    "openInspections": 6,
    "resolvedInspections": 25,
    "highRiskOpenInspections": 2,
    "safetyScore": 72
  }
]
```

반복 결함 응답은 같은 시설의 같은 `defectType`을 묶으며 발생 횟수 내림차순으로 정렬됩니다.

```json
[
  {
    "facilityId": 12,
    "facilityName": "강남구민체육관",
    "regionCode": "11680",
    "regionName": "서울특별시 강남구",
    "defectType": "CRACK",
    "occurrenceCount": 4,
    "openCount": 2,
    "highestSeverity": "HIGH",
    "lastDetectedAt": "2026-09-28T10:30:00"
  }
]
```

`minimumOccurrences`는 `2~100` 범위이며 생략하면 `2`입니다. 두 집계 API는 `SUPER_USER`만 호출할 수 있고 지역 관리자가 호출하면 `403`을 반환합니다.

## 6. 권장 프론트엔드 타입

```ts
export type AdminRole = 'REGIONAL_ADMIN' | 'SUPER_USER';
export type AdminStatus = 'ACTIVE' | 'SUSPENDED';
export type FacilityStatus = 'OPERATING' | 'UNDER_INSPECTION' | 'CLOSED';
export type DefectType = 'CRACK' | 'CORROSION' | 'DEFORMATION' | 'SURFACE_DAMAGE' | 'WATER_LEAK' | 'OTHER';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ActionStatus = 'REPORTED' | 'REVIEWING' | 'ACTION_SCHEDULED' | 'RESOLVED';
export type ReservationStatus = 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
export type ReservationTimeSlotStatus = 'AVAILABLE' | 'RESERVED' | 'CLOSED';

export interface AdminLoginResponse {
  userId: number;
  username: string;
  tokenType: 'Bearer';
  accessToken: string;
  expiresInSeconds: number;
  role: AdminRole;
  regionCode: string | null;
  regionName: string | null;
  initialSetupRequired: boolean;
}

export interface RegionOption {
  regionCode: string;
  regionName: string;
}

export interface Facility {
  id: number;
  name: string;
  type: string;
  regionCode: string;
  regionName: string;
  address: string;
  phone: string | null;
  status: FacilityStatus;
  managerUserId: number;
  publicNotice: string | null;
  source: 'KSPO_NATIONAL_FACILITY' | null;
  externalId: string | null;
  sourceUrl: string | null;
  updatedAt: string;
}

export interface PublicFacilitySyncResult {
  provider: string;
  regionCode: string;
  regionName: string;
  scannedCount: number;
  matchedCount: number;
  createdCount: number;
  updatedCount: number;
}

export interface ReservationDateOption {
  date: string;
  dayOfWeek: string;
  dayLabel: string;
  available: boolean;
}

export interface ReservationTimeSlot {
  startTime: string;
  endTime: string;
  status: ReservationTimeSlotStatus;
  statusLabel: string;
  pricePerPerson: number;
  capacity: number;
  reservedParticipants: number;
  remainingCapacity: number;
}

export interface ReservationOptions {
  facilityId: number;
  facilityName: string;
  facilityType: string;
  selectedDate: string;
  pricePerPerson: number;
  minParticipants: number;
  maxParticipants: number;
  dates: ReservationDateOption[];
  timeSlots: ReservationTimeSlot[];
}

export interface ReservationAvailability {
  facilityId: number;
  reservationDate: string;
  availableStartTimes: string[];
}

export interface Reservation {
  id: number;
  facilityId: number;
  facilityName: string;
  regionName: string;
  reservationDate: string;
  startTime: string;
  endTime: string;
  participantCount: number;
  pricePerPerson: number;
  totalFee: number;
  status: ReservationStatus;
  statusLabel: string;
  createdAt: string;
  cancelledAt: string | null;
}

export interface Inspection {
  id: number;
  facilityId: number;
  facilityName: string;
  regionCode: string;
  regionName: string;
  reporterUserId: number;
  photoUrl: string;
  locationDescription: string;
  note: string | null;
  defectType: DefectType;
  severity: Severity;
  confidence: number;
  checklist: string[];
  similarCases: string[];
  reportSummary: string;
  actionStatus: ActionStatus;
  actionNote: string | null;
  confirmed: boolean;
  confirmedAt: string | null;
  confirmedByUserId: number | null;
  confirmedDetail: string | null;
  actionRequired: boolean | null;
  actionDueDate: string | null;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface InspectionDashboard {
  totalInspections: number;
  unresolvedInspections: number;
  resolvedInspections: number;
}

export interface RegionalSafetySummary {
  regionCode: string;
  regionName: string;
  facilityCount: number;
  totalInspections: number;
  openInspections: number;
  resolvedInspections: number;
  highRiskOpenInspections: number;
  safetyScore: number;
}

export interface RecurringDefect {
  facilityId: number;
  facilityName: string;
  regionCode: string;
  regionName: string;
  defectType: DefectType;
  occurrenceCount: number;
  openCount: number;
  highestSeverity: Severity;
  lastDetectedAt: string | null;
}
```

## 7. 상태 코드 처리

| 상태 | 프론트 처리 |
|---:|---|
| 400 | 입력값 확인 |
| 401 | 토큰 제거 후 로그인 화면 이동 |
| 403 | 계정 유형 또는 권한 부족 안내 |
| 404 | 대상 데이터 없음 |
| 409 | 중복 아이디 또는 이미 완료된 지역 설정 |
| 428 | 담당 지역 최초 설정 화면 이동 |
| 503 | 관리자 서비스 일시 장애 안내 |

Gateway의 `401`, `403`, `503` 응답은 현재 응답 본문이 없을 수 있으므로 HTTP 상태 코드 기준으로 처리합니다.
