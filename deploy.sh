#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUBLIC_URL="https://projectmanager.patreek.no"
HEALTH_URL="http://localhost:3001/actuator/health"

cd "$ROOT_DIR"

if [[ ! -d ".git" ]]; then
    echo "This folder is not a git repository: $ROOT_DIR" >&2
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "There are uncommitted changes. Commit, stash, or discard them before deploying." >&2
    git status --short
    exit 1
fi

echo "==> Updating projectmanager"
git pull --ff-only

echo "==> Building and restarting Project Manager"
docker compose up -d --build

echo "==> Container status"
docker compose ps

echo "==> Health check"
for attempt in {1..30}; do
    if curl -fsS "$HEALTH_URL"; then
        echo
        echo "Deployment complete: $PUBLIC_URL"
        exit 0
    fi
    echo "Health check not ready yet, retrying..."
    sleep 2
done

echo "Health check failed after 60 seconds." >&2
docker compose logs --tail=120 backend >&2
exit 1
