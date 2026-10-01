#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MULTI_SERVICE_ROOT="${REPOSITORY_ROOT}/CheChe"
GATEWAY_ROOT="${REPOSITORY_ROOT}/gateway"

if [[ ! -f "${SCRIPT_DIR}/cloud-run.env" ]]; then
  echo "Copy cloud-run.env.example to cloud-run.env and enter the non-secret TiDB values." >&2
  exit 1
fi
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/cloud-run.env"

PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project)}"
REGION="${REGION:-asia-northeast3}"
REPOSITORY="${REPOSITORY:-cheche}"
RUNTIME_SERVICE_ACCOUNT="${RUNTIME_SERVICE_ACCOUNT:-cheche-runtime@${PROJECT_ID}.iam.gserviceaccount.com}"
IMAGE_PREFIX="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}"

required=(TIDB_HOST TIDB_PORT TIDB_USERNAME GCS_BUCKET)
for value in "${required[@]}"; do
  if [[ -z "${!value:-}" || "${!value}" == *replace-me* || "${!value}" == *example* ]]; then
    echo "Set ${value} in deploy/cloud-run/cloud-run.env" >&2
    exit 1
  fi
done

db_url() {
  local database="$1"
  printf 'jdbc:mysql://%s:%s/%s?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3&serverTimezone=Asia/Seoul&characterEncoding=UTF-8' \
    "$TIDB_HOST" "$TIDB_PORT" "$database"
}

build_service() {
  local module="$1"
  local image="${IMAGE_PREFIX}/${module}:v1"
  gcloud builds submit "$MULTI_SERVICE_ROOT" --config="$MULTI_SERVICE_ROOT/cloudbuild.yaml" \
    --substitutions="_SERVICE=${module},_IMAGE=${image}" >&2
  printf '%s' "$image"
}

deploy_service() {
  local name="$1"
  local image="$2"
  shift 2
  gcloud run deploy "$name" \
    --image="$image" \
    --region="$REGION" \
    --service-account="$RUNTIME_SERVICE_ACCOUNT" \
    --allow-unauthenticated \
    --memory=512Mi \
    --cpu=1 \
    --min-instances=0 \
    --max-instances=1 \
    --timeout=120 \
    "$@"
}

service_url() {
  gcloud run services describe "$1" --region="$REGION" --format='value(status.url)'
}

ADMIN_IMAGE="$(build_service admin-service)"
deploy_service cheche-admin "$ADMIN_IMAGE" \
  --update-env-vars="ADMIN_DB_URL=$(db_url cheche_admin),MYSQL_USERNAME=${TIDB_USERNAME}" \
  --update-secrets="MYSQL_PASSWORD=cheche-mysql-password:latest"
ADMIN_URL="$(service_url cheche-admin)"

FACILITY_IMAGE="$(build_service facility-service)"
deploy_service cheche-facility "$FACILITY_IMAGE" \
  --update-env-vars="FACILITY_DB_URL=$(db_url cheche_facility),MYSQL_USERNAME=${TIDB_USERNAME}" \
  --update-secrets="MYSQL_PASSWORD=cheche-mysql-password:latest,DATA_GO_KR_SERVICE_KEY=cheche-data-go-kr-key:latest,SEOUL_OPEN_API_KEY=cheche-seoul-open-api-key:latest,KSPO_OPEN_API_KEY=cheche-kspo-open-api-key:latest"
FACILITY_URL="$(service_url cheche-facility)"

INSPECTION_IMAGE="$(build_service inspection-service)"
deploy_service cheche-inspection "$INSPECTION_IMAGE" \
  --update-env-vars="INSPECTION_DB_URL=$(db_url cheche_inspection),MYSQL_USERNAME=${TIDB_USERNAME},FACILITY_SERVICE_URI=${FACILITY_URL},CHECHE_STORAGE_TYPE=gcs,CHECHE_GCS_BUCKET=${GCS_BUCKET}" \
  --update-secrets="MYSQL_PASSWORD=cheche-mysql-password:latest"
INSPECTION_URL="$(service_url cheche-inspection)"

LOGIN_IMAGE="$(build_service login-service)"
deploy_service cheche-login "$LOGIN_IMAGE" \
  --update-env-vars="LOGIN_DB_URL=$(db_url cheche_login),MYSQL_USERNAME=${TIDB_USERNAME},ADMIN_SERVICE_URI=${ADMIN_URL}" \
  --update-secrets="MYSQL_PASSWORD=cheche-mysql-password:latest,JWT_SECRET=cheche-jwt-secret:latest"
LOGIN_URL="$(service_url cheche-login)"

GATEWAY_IMAGE="${IMAGE_PREFIX}/gateway:v1"
gcloud builds submit "$GATEWAY_ROOT" --config="$GATEWAY_ROOT/cloudbuild.yaml" \
  --substitutions="_IMAGE=${GATEWAY_IMAGE}"
deploy_service cheche-gateway "$GATEWAY_IMAGE" \
  --update-env-vars="LOGIN_SERVICE_URI=${LOGIN_URL},ADMIN_SERVICE_URI=${ADMIN_URL},FACILITY_SERVICE_URI=${FACILITY_URL},INSPECTION_SERVICE_URI=${INSPECTION_URL}" \
  --update-secrets="JWT_SECRET=cheche-jwt-secret:latest"

GATEWAY_URL="$(service_url cheche-gateway)"
echo
echo "Deployment complete"
echo "Gateway URL: ${GATEWAY_URL}"
echo "Health check: ${GATEWAY_URL}/actuator/health"
