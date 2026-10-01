#!/usr/bin/env bash
set -euo pipefail

create_or_replace_secret() {
  local name="$1"
  local value
  read -r -s -p "${name}: " value
  echo
  if gcloud secrets describe "$name" >/dev/null 2>&1; then
    printf '%s' "$value" | gcloud secrets versions add "$name" --data-file=-
  else
    printf '%s' "$value" | gcloud secrets create "$name" --data-file=- --replication-policy=automatic
  fi
  unset value
}

echo "Paste only secret values. Inputs are hidden."
create_or_replace_secret cheche-mysql-password
create_or_replace_secret cheche-jwt-secret
create_or_replace_secret cheche-data-go-kr-key
create_or_replace_secret cheche-seoul-open-api-key
create_or_replace_secret cheche-kspo-open-api-key
