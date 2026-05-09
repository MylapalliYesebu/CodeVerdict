#!/bin/bash

# Configuration
API_URL="http://localhost:8080/api"
DB_USER="cv_user"
DB_NAME="codeverdict_dev"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Starting CodeVerdict API Testing Workflow...${NC}\n"

# 1. Health Check
echo "1. Testing Health Check..."
curl -s -X GET "$API_URL/health" | jq .
echo ""

# 2. Signup
echo "2. Registering a new user..."
USERNAME="testadmin$RANDOM"
EMAIL="${USERNAME}@codeverdict.com"
curl -s -X POST "$API_URL/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$EMAIL\",\"password\":\"password123\"}" | jq .
echo ""

# Make the user an ADMIN in the database so we can test Problem creation
echo "-> Granting ADMIN role to $EMAIL in the database..."
PGPASSWORD="cv_local_pass" psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c "UPDATE users SET role='ADMIN' WHERE email='$EMAIL';" > /dev/null 2>&1
echo -e "${GREEN}Done.${NC}\n"

# 3. Login
echo "3. Logging in..."
LOGIN_RES=$(curl -s -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\"}")
echo "$LOGIN_RES" | jq .

# Extract the JWT token
TOKEN=$(echo "$LOGIN_RES" | jq -r .token)
echo -e "-> Extracted Token: ${YELLOW}$TOKEN${NC}\n"

# 4. Create a Problem
echo "4. Creating a new problem (Requires ADMIN)..."
PROB_RES=$(curl -s -X POST "$API_URL/problems" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Add Two Numbers",
    "description": "Given two numbers a and b, return their sum.",
    "difficulty": "EASY",
    "testCases": [
      {"input": "2 3", "expectedOutput": "5", "isPublic": true},
      {"input": "-1 5", "expectedOutput": "4", "isPublic": false}
    ]
  }')
echo "$PROB_RES" | jq .

# Extract the Problem ID
PROB_ID=$(echo "$PROB_RES" | jq -r .id)
echo ""

# 5. Get all Problems
echo "5. Fetching all problems..."
curl -s -X GET "$API_URL/problems?page=1&limit=10" | jq .
echo ""

# 6. Get the specific Problem
if [ "$PROB_ID" != "null" ]; then
  echo "6. Fetching problem $PROB_ID details..."
  curl -s -X GET "$API_URL/problems/$PROB_ID" | jq .
  echo ""

  # 7. Submit a solution
  echo "7. Submitting a Java solution for problem $PROB_ID..."
  SUB_RES=$(curl -s -X POST "$API_URL/submit" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"problemId\": $PROB_ID,
      \"language\": \"java\",
      \"sourceCode\": \"import java.util.Scanner; public class Main { public static void main(String[] args) { Scanner sc = new Scanner(System.in); int a = sc.nextInt(); int b = sc.nextInt(); System.out.print(a + b); } }\"
    }")
  echo "$SUB_RES" | jq .

  SUB_ID=$(echo "$SUB_RES" | jq -r .submissionId)
  echo ""

  # Wait a bit for judge to process
  echo "-> Waiting 2 seconds for judge to process submission..."
  sleep 2
  echo ""

  # 8. Check submission status
  echo "8. Checking submission status for ID $SUB_ID..."
  curl -s -X GET "$API_URL/submissions/$SUB_ID" -H "Authorization: Bearer $TOKEN" | jq .
  echo ""
fi

# 9. Get Leaderboard
echo "9. Fetching Leaderboard..."
curl -s -X GET "$API_URL/leaderboard" | jq .
echo ""

# 10. Logout
echo "10. Logging out..."
curl -s -X POST "$API_URL/auth/logout" -H "Authorization: Bearer $TOKEN" | jq .
echo -e "\n${GREEN}API Testing Workflow Complete!${NC}"
