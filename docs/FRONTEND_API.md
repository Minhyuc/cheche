# CheChe 프론트엔드 API 연동 명세

프론트엔드는 개별 마이크로서비스가 아닌 Gateway `http://localhost:8080`만 호출합니다.

- 요청/응답 JSON 인코딩: UTF-8
- 날짜 형식: ISO 8601 (`2026-09-09T18:30:00`)
- 인증 헤더: `Authorization: Bearer <accessToken>`
- `X-User-Id`, `X-User-Role`, `X-User-Region`은 Gateway가 생성하는 내부 헤더입니다. 프론트에서 전송하지 않습니다.
- 전체 기계 판독용 명세: [`openapi.yaml`](./openapi.yaml)

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

검색 요청:

```json
{ "query": "강남에서 수영할 수 있는 곳" }
```

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

### 관리자 로그인

`POST /auth/admin/login`

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

## 5. 안전점검 API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/inspections` | 관리자 | 권한 범위 점검 목록 |
| POST | `/api/inspections` | 관리자 | 사진 점검 접수 |
| GET | `/api/inspections/facilities/{facilityId}/history` | 관리자 | 시설별 안전 이력 |
| GET | `/api/inspections/open` | 관리자 | 미조치 목록 |
| GET | `/api/inspections/dashboard` | 관리자 | 점검 집계 |
| PATCH | `/api/inspections/{id}/action` | 관리자 | 조치 상태 변경 |
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

await fetch('/api/inspections', {
  method: 'POST',
  headers: { Authorization: `Bearer ${accessToken}` },
  body: form,
});
```

`FormData` 요청에는 브라우저가 boundary를 생성해야 하므로 `Content-Type`을 직접 지정하지 않습니다.

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

## 6. 권장 프론트엔드 타입

```ts
export type AdminRole = 'REGIONAL_ADMIN' | 'SUPER_USER';
export type AdminStatus = 'ACTIVE' | 'SUSPENDED';
export type FacilityStatus = 'OPERATING' | 'UNDER_INSPECTION' | 'CLOSED';
export type DefectType = 'CRACK' | 'CORROSION' | 'DEFORMATION' | 'SURFACE_DAMAGE' | 'WATER_LEAK' | 'OTHER';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ActionStatus = 'REPORTED' | 'REVIEWING' | 'ACTION_SCHEDULED' | 'RESOLVED';

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
