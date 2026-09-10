# CheChe

AI 기반 공공 체육시설 관리·안전점검 서비스의 관리자 백엔드입니다.

## 구조

```text
CheChe/
├── CheChe/                    # 도메인 마이크로서비스 멀티 프로젝트
│   ├── login-service/         # 아이디/비밀번호 로그인, JWT 발급
│   ├── admin-service/         # 관리자 온보딩, 지역/권한 관리
│   ├── facility-service/      # 체육시설 및 지역 범위 조회
│   └── inspection-service/    # 사진 점검, 결함, 조치/보고서 이력
└── gateway/                    # 단일 진입점과 라우팅
```

Next-Me와 마찬가지로 Spring Boot/Gradle 기반이며 Gateway와 서비스별 DB를 분리합니다.

## API 문서

- [프론트엔드 연동 가이드](docs/FRONTEND_API.md)
- [OpenAPI 3.0 명세](docs/openapi.yaml)

## 로그인과 JWT

Gateway의 루트 주소에는 `사용자 로그인`과 `관리자 로그인` 탭을 분리한 화면이 제공됩니다.

- 사용자 로그인: `USER` 계정의 자격 증명 확인과 JWT 발급까지만 수행합니다. 이후 사용자 기능은 연결하지 않습니다.
- 관리자 로그인: `ADMIN` 계정만 허용하며 admin-service 동기화, 최초 지역 설정, 시설·점검 관리로 이어집니다.
- 관리자 회원가입: 아이디와 비밀번호만 받습니다. 사용자 계정 회원가입은 구현하지 않았으며 기존 사용자 DB 이관 또는 별도 프로비저닝을 전제로 합니다.

비밀번호는 BCrypt 해시로만 저장하며 평문을 저장하지 않습니다. 관리자 로그인에 성공하면 login-service가 admin-service를 동기화하고 JWT 액세스 토큰과 `initialSetupRequired`를 반환합니다.

JWT에는 `userId`, `username`, `accountType`, 발급자, 발급/만료 시각만 저장합니다. `accountType`은 사용자 토큰으로 관리자 API를 호출하지 못하게 로그인 영역을 구분합니다. 변경 가능한 지역·관리자 권한·계정 상태는 JWT에 넣지 않습니다. Gateway는 보호 API 요청마다 admin-service에서 최신 관리자 정보를 읽은 뒤 아래 내부 헤더를 주입합니다. 따라서 지역 설정, 슈퍼유저 권한 변경, 계정 정지가 재로그인 없이 즉시 반영됩니다.

- `X-User-Id`: 로그인 서비스의 사용자 ID
- `X-User-Role`: `REGIONAL_ADMIN` 또는 `SUPER_USER`
- `X-User-Region`: 현재 관리자 DB에 저장된 지역 코드

Gateway는 클라이언트가 임의로 보낸 `X-User-*` 헤더를 항상 제거합니다. JWT 서명을 검증한 사용자만 관리자 DB의 최신 권한 정보로 헤더를 다시 받을 수 있습니다.

로그인 성공 후 로그인 서비스는 `POST /api/admins/sync`를 호출합니다. 관리자 정보가 없거나 지역이 설정되지 않았다면 응답의 `initialSetupRequired`가 `true`가 되며, 프론트는 지역 입력 화면을 표시합니다.

## 주요 API

- `POST /auth/admin/register` 관리자 아이디·비밀번호 회원가입
- `POST /auth/admin/login` 관리자 로그인, JWT 및 최초 설정 여부 반환
- `POST /auth/user/login` 사용자 로그인 및 JWT 발급만 수행
- `POST /api/admins/sync` 로그인 서비스의 관리자 동기화
- `PUT /api/admins/me/region` 최초 지역 설정
- `GET /api/admins/me` 내 관리자 프로필
- `GET /api/admins` 슈퍼유저 전용 관리자 목록
- `GET/POST /api/facilities` 권한 범위 내 시설 조회/등록
- `POST /api/inspections` 사진 기반 점검 기록 생성
- `PATCH /api/inspections/{id}/action` 조치 상태 갱신
- `GET /api/inspections/facilities/{facilityId}/history` 시설 안전 이력
- `GET /api/inspections/open` 미조치 목록
- `GET /api/inspections/dashboard` 점검 현황 집계

## 실행

MySQL에서 `cheche_login`, `cheche_admin`, `cheche_facility`, `cheche_inspection` 데이터베이스를 만든 뒤 `.env.example`을 `.env`로 복사하여 값을 입력합니다. `.env`는 바깥 프로젝트 폴더 또는 안쪽 `CheChe/` 멀티모듈 폴더에 둘 수 있습니다. 각 서비스와 Gateway는 IntelliJ 및 Gradle의 서로 다른 실행 디렉터리에서도 같은 `.env`를 자동으로 찾습니다. 운영체제 환경변수가 설정되어 있으면 `.env`보다 우선합니다.

```bash
cp CheChe/.env.example CheChe/.env

# .env 파일에 MYSQL_PASSWORD, JWT_SECRET 등을 입력한 뒤 실행
cd CheChe
./gradlew bootRun --parallel

# 별도 터미널: API Gateway와 로그인 화면
cd ../gateway
./gradlew bootRun
```

Gateway는 `http://localhost:8080`에서 실행됩니다. 로그인·관리자·시설·점검 서비스는 각각 8081, 8082, 8083, 8084 포트를 사용합니다.
최초 슈퍼유저는 위 환경변수로 명시적으로 부트스트랩하며, 이후 다른 관리자 권한과 상태를 관리할 수 있습니다.

## 개발용 요청 예시

```bash
curl -X POST http://localhost:8080/auth/admin/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"seoul-admin","password":"password123"}'

curl -X POST http://localhost:8080/auth/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"seoul-admin","password":"password123"}'
```
