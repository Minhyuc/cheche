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

### 국민체육진흥공단 공공데이터 동기화

MVP 적용 범위는 서울특별시 25개 자치구입니다. 지역 관리자가 실행하면 관리자 DB의 담당 지역 코드를 기준으로 해당 자치구만 요청하고, 슈퍼관리자가 실행하면 서울특별시 전체를 요청합니다. 사용자 API에는 새로운 기능을 추가하지 않습니다.

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
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface InspectionDashboard {
  totalInspections: number;
  unresolvedInspections: number;
  resolvedInspections: number;
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
