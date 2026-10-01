# Cloud Run + TiDB 배포 가이드

이 문서는 2026년 10월 MVP 기간 동안 CheChe 백엔드를 Google Cloud Run과 TiDB Cloud Starter에 배포하는 절차다. Gateway만 프론트엔드의 API 주소로 사용한다.

## 배포 구성

```text
Frontend
  └─ cheche-gateway (Cloud Run)
       ├─ cheche-login (Cloud Run)     → cheche_login
       ├─ cheche-admin (Cloud Run)     → cheche_admin
       ├─ cheche-facility (Cloud Run)  → cheche_facility
       └─ cheche-inspection (Cloud Run)→ cheche_inspection + GCS photo bucket
                                               ↓
                                         TiDB Cloud Starter
```

모든 Cloud Run 서비스는 `asia-northeast3`(서울)에 배포한다. 현재 MVP는 서비스 간 호출을 단순화하기 위해 서비스를 공개로 배포한다. 프론트엔드는 반드시 Gateway URL만 사용한다.

## 사전 준비

1. Google Cloud 프로젝트 `cheche-mvp`에서 다음 API를 활성화한다.

   ```bash
   gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com storage.googleapis.com
   ```

2. Artifact Registry `cheche`가 `asia-northeast3`에 있어야 한다.

3. TiDB Cloud Starter에서 다음 DB를 생성한다.

   ```sql
   CREATE DATABASE cheche_login;
   CREATE DATABASE cheche_admin;
   CREATE DATABASE cheche_facility;
   CREATE DATABASE cheche_inspection;
   ```

4. TiDB Connect 화면에서 `HOST`, `PORT`, `USERNAME`, 생성한 `PASSWORD`를 확보한다. 비밀번호는 Git이나 문서에 기록하지 않는다.

## Cloud Shell에서 실행

GitHub 최신 main 브랜치를 Cloud Shell로 가져온다.

```bash
git clone https://github.com/Minhyuc/cheche.git
cd cheche
```

Cloud Run 서비스 계정과 사진 버킷을 준비한다. 버킷 이름은 프로젝트 ID를 포함하므로 전역적으로 고유하다.

```bash
chmod +x deploy/cloud-run/*.sh
./deploy/cloud-run/bootstrap-cloud-run.sh
```

MVP에서 사진은 프론트가 직접 표시할 수 있도록 공개 읽기 권한으로 저장된다. `bootstrap-cloud-run.sh`가 이 권한을 설정한다. 시연 종료 뒤 버킷을 삭제하거나 `allUsers` 권한을 제거한다.

비밀값은 입력 시 화면에 보이지 않으며 Secret Manager에만 저장된다.

```bash
./deploy/cloud-run/create-secrets.sh
```

입력 순서:

1. TiDB 비밀번호
2. 32자 이상 JWT 시크릿
3. data.go.kr 전국체육시설 API 키
4. 서울 열린데이터광장 API 키
5. KSPO 운영시설 API 키

공공 API 키가 없으면 해당 프롬프트에서 Enter를 눌러도 서비스 배포는 가능하지만, 해당 외부 데이터 동기화 기능은 동작하지 않는다.

배포 환경 파일을 만든다.

```bash
cp deploy/cloud-run/cloud-run.env.example deploy/cloud-run/cloud-run.env
nano deploy/cloud-run/cloud-run.env
```

아래 항목만 TiDB Connect 화면의 실제 값으로 바꾼다.

```dotenv
TIDB_HOST=gateway01.ap-northeast-1.prod.aws.tidbcloud.com
TIDB_PORT=4000
TIDB_USERNAME=TiDB_사용자명
GCS_BUCKET=cheche-mvp-inspection-photos
```

`TIDB_HOST` 또는 `TIDB_USERNAME`에 `replace-me`, `example` 값이 남아 있으면 배포 스크립트가 중단된다.

## 배포

```bash
./deploy/cloud-run/deploy.sh
```

이 스크립트는 다음을 수행한다.

1. 서비스별 JAR를 Cloud Build에서 Docker 이미지로 빌드
2. `cheche-admin` → `cheche-facility` → `cheche-inspection` → `cheche-login` → `cheche-gateway` 순으로 배포
3. 이전 서비스의 Cloud Run URL을 다음 서비스의 환경변수로 자동 주입
4. Gateway URL과 Health Check URL 출력

Cloud Run은 컨테이너에 `PORT`를 주입한다. 모든 서비스의 `application.yml`은 `${PORT:기본포트}`를 사용하므로 로컬 실행 포트도 유지된다.

## 배포 확인

```bash
GATEWAY_URL="$(gcloud run services describe cheche-gateway --region=asia-northeast3 --format='value(status.url)')"
curl "${GATEWAY_URL}/actuator/health"
curl "${GATEWAY_URL}/api/user/facilities?region=서울특별시"
```

프론트엔드의 API base URL에는 출력된 Gateway URL만 사용한다.

## 슈퍼관리자

`login-service`는 첫 실행 때 DB에 다음 MVP 계정을 자동 생성한다.

```text
username: superadmin
password: superadmin
```

관리자 로그인 시 `admin-service`가 해당 사용자를 `SUPER_USER`로 동기화한다. `CHECHE_SUPER_USER_ID` 환경변수는 설정하지 않는다.

## 사진 저장

Cloud Run의 로컬 파일 시스템은 영속적이지 않다. 배포 스크립트는 Inspection Service에 다음 설정을 전달한다.

```text
CHECHE_STORAGE_TYPE=gcs
CHECHE_GCS_BUCKET=cheche-mvp-inspection-photos
```

따라서 개선 요청과 점검 사진은 Google Cloud Storage에 저장되고, 컨테이너 재시작 후에도 유지된다.

## 10월 16일 이후 정리

```bash
for service in cheche-gateway cheche-login cheche-admin cheche-facility cheche-inspection; do
  gcloud run services delete "$service" --region=asia-northeast3 --quiet
done

gcloud storage rm --recursive "gs://cheche-mvp-inspection-photos"
gcloud artifacts docker images delete \
  asia-northeast3-docker.pkg.dev/cheche-mvp/cheche/gateway:v1 \
  --quiet
```

필요하면 Cloud Console에서 TiDB Cloud Starter 인스턴스도 삭제한다.
