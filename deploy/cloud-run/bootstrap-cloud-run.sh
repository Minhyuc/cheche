#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project)}"
REGION="${REGION:-asia-northeast3}"
REPOSITORY="${REPOSITORY:-cheche}"
RUNTIME_SERVICE_ACCOUNT="${RUNTIME_SERVICE_ACCOUNT:-cheche-runtime@${PROJECT_ID}.iam.gserviceaccount.com}"
GCS_BUCKET="${GCS_BUCKET:-${PROJECT_ID}-inspection-photos}"

if [[ -z "${PROJECT_ID}" || "${PROJECT_ID}" == "(unset)" ]]; then
  echo "Set a Google Cloud project first: gcloud config set project PROJECT_ID" >&2
  exit 1
fi

gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com storage.googleapis.com

if ! gcloud artifacts repositories describe "$REPOSITORY" --location="$REGION" >/dev/null 2>&1; then
  gcloud artifacts repositories create "$REPOSITORY" --repository-format=docker --location="$REGION" --description="CheChe Spring Boot images"
fi

if ! gcloud iam service-accounts describe "$RUNTIME_SERVICE_ACCOUNT" >/dev/null 2>&1; then
  gcloud iam service-accounts create cheche-runtime --display-name="CheChe Cloud Run runtime"
fi

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUNTIME_SERVICE_ACCOUNT}" \
  --role="roles/secretmanager.secretAccessor" \
  --quiet

if ! gcloud storage buckets describe "gs://${GCS_BUCKET}" >/dev/null 2>&1; then
  gcloud storage buckets create "gs://${GCS_BUCKET}" --location="$REGION" --uniform-bucket-level-access
fi

gcloud storage buckets add-iam-policy-binding "gs://${GCS_BUCKET}" \
  --member="serviceAccount:${RUNTIME_SERVICE_ACCOUNT}" \
  --role="roles/storage.objectAdmin"

# MVP photo URLs are returned directly to the frontend. Remove this binding when signed URLs are introduced.
gcloud storage buckets add-iam-policy-binding "gs://${GCS_BUCKET}" \
  --member="allUsers" \
  --role="roles/storage.objectViewer"

echo "Bootstrap complete"
echo "PROJECT_ID=${PROJECT_ID}"
echo "REGION=${REGION}"
echo "GCS_BUCKET=${GCS_BUCKET}"
echo "RUNTIME_SERVICE_ACCOUNT=${RUNTIME_SERVICE_ACCOUNT}"
