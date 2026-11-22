#!/usr/bin/env bash
# Creates three related authentication incidents from three different employees (Workflow 3).
# Usage: ./docs/demo-workflow3.sh [base-url]   (default http://localhost:8080)
set -euo pipefail
BASE="${1:-http://localhost:8080}"
login() { curl -s "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"Password@123\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/'; }
report() { # token title serviceId
  curl -s -o /dev/null -w "%{http_code}  $2\n" "$BASE/api/incidents" -H 'Content-Type: application/json' -H "Authorization: Bearer $1" \
    -d "{\"title\":\"$2\",\"description\":\"$2. Started a few minutes ago, many colleagues affected, sign-in keeps looping.\",\"serviceId\":$3,\"impact\":\"HIGH\",\"urgency\":\"CRITICAL\",\"affectedUsers\":300}"
}
report "$(login karan@itsm.local)" "Cannot login to email - SSO error" 7
report "$(login pooja@itsm.local)" "VPN authentication failing for everyone" 8
report "$(login siddharth@itsm.local)" "Payment portal login page rejects credentials" 6
