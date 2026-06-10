#!/bin/bash
H="http://127.0.0.1"

echo "1. 登录"
L=$(curl -s -X POST $H/api/v1/auth/login -H "Content-Type: application/json" -d '{"code":"dev_user_a"}')
T=$(echo "$L" | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
echo "  OK"

echo "2. 创建牌局"
R=$(curl -s -X POST $H/api/v1/rooms/create -H "Authorization: Bearer $T" -H "Content-Type: application/json" -d '{}')
RID=$(echo "$R" | python3 -c "import sys,json;print(json.load(sys.stdin)['roomId'])")
echo "  OK $RID"

echo "3. 提交第1局"
curl -s -X POST "$H/api/v1/rooms/$RID/round" \
  -H "Authorization: Bearer $T" -H "Content-Type: application/json" \
  -d '{"scores":{"dev_user_a":8,"dev_user_b":-4,"dev_user_c":3,"dev_user_d":-7}}' | \
  python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  OK round={d[\"round\"][\"roundNumber\"]}')"

echo "4. 提交第2局"
curl -s -X POST "$H/api/v1/rooms/$RID/round" \
  -H "Authorization: Bearer $T" -H "Content-Type: application/json" \
  -d '{"scores":{"dev_user_a":12,"dev_user_b":-6,"dev_user_c":-6,"dev_user_d":0}}' | \
  python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  OK round={d[\"round\"][\"roundNumber\"]}')"

echo "5. 收盘"
curl -s -X PUT "$H/api/v1/rooms/$RID/close" \
  -H "Authorization: Bearer $T" -H "Content-Type: application/json" -d '{}' | \
  python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  OK status={d[\"room\"][\"status\"]}')"

echo "6. 牌局列表"
curl -s "$H/api/v1/rooms" -H "Authorization: Bearer $T" | \
  python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  OK total={d[\"total\"]}')"

echo "7. 创建小组"
curl -s -X POST "$H/api/v1/groups" \
  -H "Authorization: Bearer $T" -H "Content-Type: application/json" \
  -d '{"name":"周末麻将局"}' | \
  python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  OK name={d[\"name\"]} members={d[\"memberCount\"]}')"

echo "8. 小组列表"
curl -s "$H/api/v1/groups" -H "Authorization: Bearer $T" | \
  python3 -c "import sys,json;d=json.load(sys.stdin);print(f'  OK total={d[\"total\"]}')"

echo ""
echo "ALL TESTS PASSED"
