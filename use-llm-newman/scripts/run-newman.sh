#!/usr/bin/env bash
set -euo pipefail

MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-${MODULE_DIR}/postman/local.environment.json}"
COLLECTION_FILE="${MODULE_DIR}/postman/use-llm-api.collection.json"

if ! command -v node >/dev/null 2>&1; then
  echo "Error: node is not installed."
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "Error: npm is not installed."
  exit 1
fi

cd "${MODULE_DIR}"

if [[ ! -d node_modules ]]; then
  echo "Installing npm dependencies..."
  npm install
fi

mkdir -p reports

echo "Running Newman collection against environment: ${ENV_FILE}"
npx newman run "${COLLECTION_FILE}" \
  -e "${ENV_FILE}" \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export "reports/newman-report.html"

echo "Report generated at ${MODULE_DIR}/reports/newman-report.html"

