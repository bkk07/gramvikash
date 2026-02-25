# API Quick Reference

## Base URL
```
http://localhost:8080
```

---

## 🔐 Authentication Endpoints

### POST /api/farmers/register
Create new farmer account
```json
{
  "phoneNumber": "9876543210",
  "userName": "farmer_radha",
  "password": "SecurePass@123",
  "fullName": "Radha Kumar",
  "dob": "1981-05-15",
  "language": "TELUGU",
  "districtId": 1,
  "mandalId": 1
}
```
**Response:** `{farmerId, userName, message}`

---

### POST /api/farmers/login
Get JWT authentication token
```json
{
  "userName": "farmer_radha",
  "password": "SecurePass@123"
}
```
**Response:** `{token, userName, message}`

---

### GET /api/farmers/profile/{userName}
Get farmer profile with computed age
```
Headers: Authorization: Bearer <token>
```
**Response:** `{id, phoneNumber, userName, fullName, dob, age, stateName, districtName, mandalName, language, isActive}`

---

### POST /api/farmers/change-password
Update password
```json
{
  "userName": "farmer_radha",
  "oldPassword": "SecurePass@123",
  "newPassword": "NewPassword@999"
}
```
**Headers:** `Authorization: Bearer <token>`

---

## 📋 Scheme Management Endpoints

### POST /api/schemes/create
Create new scheme with eligibility rules
```json
{
  "schemeName": "PM-KISAN Samman Nidhi",
  "schemeCode": "PMKISAN-001",
  "description": "Scheme description...",
  "benefitDetails": "₹6,000 per year in 3 installments...",
  "category": "AGRICULTURE_SUBSIDY",
  "state": null,
  "eligibilityGroups": [
    {
      "groupName": "BASIC_REQUIREMENTS",
      "groupOperator": "AND",
      "rules": [
        {"fieldName": "age", "operator": ">=", "value": "18", "fieldType": "NUMBER"},
        {"fieldName": "landSize", "operator": "<=", "value": "2", "fieldType": "NUMBER"},
        {"fieldName": "income", "operator": "<=", "value": "150000", "fieldType": "NUMBER"}
      ]
    }
  ],
  "faqs": [
    {"question": "Who is eligible?", "answer": "All farmers with land <= 2 hectares", "language": "EN", "displayOrder": 1}
  ]
}
```
**Headers:** `Authorization: Bearer <token>`

---

### GET /api/schemes/browse
Browse all schemes with optional filters
```
GET /api/schemes/browse
GET /api/schemes/browse?state=CENTRAL
GET /api/schemes/browse?state=Telangana
GET /api/schemes/browse?category=AGRICULTURE_SUBSIDY
GET /api/schemes/browse?state=CENTRAL&category=CROP_INSURANCE
```

---

### GET /api/schemes/{schemeId}
Get full scheme detail with FAQs
```
GET /api/schemes/1
GET /api/schemes/1?language=EN
GET /api/schemes/1?language=TE
GET /api/schemes/1?language=HI
```

---

## 👨‍🌾 Farmer Data Management

### POST /api/schemes/user-fields
Save/update farmer's known fields
```json
{
  "farmerId": 1,
  "fields": [
    {"fieldName": "landSize", "value": "1.5", "fieldType": "NUMBER"},
    {"fieldName": "income", "value": "120000", "fieldType": "NUMBER"},
    {"fieldName": "cropType", "value": "Paddy", "fieldType": "STRING"}
  ]
}
```
**Headers:** `Authorization: Bearer <token>`

---

## ✅ Eligibility Check (CORE ENGINE)

### POST /api/schemes/check-eligibility
Check farmer eligibility for all schemes
```json
{
  "farmerId": 1,
  "mode": "DISCOVER",
  "additionalFields": {
    "landSize": "1.5",
    "income": "120000",
    "cropType": "Paddy",
    "state": "Telangana"
  }
}
```
**Headers:** `Authorization: Bearer <token>`

**Mode Options:**
- `DISCOVER` — Skip missing fields, show potential matches
- `VERIFY` — Strict evaluation, missing field = fail

**Response Structure:**
```json
{
  "mode": "VERIFY",
  "totalSchemesEvaluated": 5,
  "eligibleSchemes": [
    {
      "schemeId": 1,
      "schemeName": "PM-KISAN Samman Nidhi",
      "schemeCode": "PMKISAN-001",
      "category": "AGRICULTURE_SUBSIDY",
      "benefitSummary": "₹6,000 per year...",
      "eligible": true,
      "failedRuleCount": 0,
      "totalRules": 3,
      "reasonMessage": "You are fully eligible for this scheme.",
      "missingFields": []
    }
  ],
  "almostEligibleSchemes": [
    {
      "schemeId": 2,
      "schemeName": "Other Scheme",
      "eligible": false,
      "failedRuleCount": 1,
      "reasonMessage": "You could also qualify for ... if your annual income is below ₹1.5L"
    }
  ],
  "ineligibleSchemes": [...]
}
```

---

## 🎯 Field Data Types & Operators

### NUMBER Type
```
Operators: =, >, <, >=, <=, !=
Example: {"fieldName": "age", "operator": ">=", "value": "18", "fieldType": "NUMBER"}
Example: {"fieldName": "landSize", "operator": "<=", "value": "2", "fieldType": "NUMBER"}
```

### STRING Type
```
Operators: =, !=, IN
Example: {"fieldName": "state", "operator": "=", "value": "Telangana", "fieldType": "STRING"}
Example: {"fieldName": "cropType", "operator": "IN", "value": "Paddy,Wheat,Maize", "fieldType": "STRING"}
```

### BOOLEAN Type
```
Operators: =, !=
Example: {"fieldName": "minority", "operator": "=", "value": "true", "fieldType": "BOOLEAN"}
```

---

## 🔗 Group Operators

### AND Operator
```
All rules MUST pass
If any rule fails → group fails
Missing field in DISCOVER: skip, in VERIFY: fail
```

### OR Operator
```
At least ONE rule must pass
If any rule passes → group passes
```

---

## 📊 Sample Scheme Eligibility Rules

### PM-KISAN (age + land + income)
```json
{
  "groupName": "BASIC_REQUIREMENTS",
  "groupOperator": "AND",
  "rules": [
    {"fieldName": "age", "operator": ">=", "value": "18", "fieldType": "NUMBER"},
    {"fieldName": "landSize", "operator": "<=", "value": "2", "fieldType": "NUMBER"},
    {"fieldName": "income", "operator": "<=", "value": "150000", "fieldType": "NUMBER"}
  ]
}
```

### PMFBY (farmer requirements AND crop eligibility)
```json
{
  "groupName": "FARMER_REQUIREMENTS",
  "groupOperator": "AND",
  "rules": [
    {"fieldName": "age", "operator": ">=", "value": "18", "fieldType": "NUMBER"},
    {"fieldName": "landSize", "operator": ">=", "value": "0.5", "fieldType": "NUMBER"}
  ]
},
{
  "groupName": "CROP_ELIGIBILITY",
  "groupOperator": "OR",
  "rules": [
    {"fieldName": "cropType", "operator": "IN", "value": "Paddy,Wheat,Maize,Cotton,Sugarcane,Groundnut,Soybean", "fieldType": "STRING"}
  ]
}
```

### Rythu Bandhu (state-specific)
```json
{
  "groupName": "STATE_AND_LAND",
  "groupOperator": "AND",
  "rules": [
    {"fieldName": "state", "operator": "=", "value": "Telangana", "fieldType": "STRING"},
    {"fieldName": "landSize", "operator": ">=", "value": "1", "fieldType": "NUMBER"},
    {"fieldName": "age", "operator": ">=", "value": "18", "fieldType": "NUMBER"}
  ]
}
```

---

## 🌍 Language Support

### FAQs Available In:
- **EN** - English
- **TE** - Telugu
- **HI** - Hindi

### Fetch FAQ in Language:
```
GET /api/schemes/1?language=TE
GET /api/schemes/1?language=HI
GET /api/schemes/1?language=EN (or use default)
```

---

## 📱 Complete User Journey Example

### Step 1: Register
```bash
curl -X POST http://localhost:8080/api/farmers/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "9876543210",
    "userName": "farmer_radha",
    "password": "SecurePass@123",
    "fullName": "Radha Kumar",
    "dob": "1981-05-15",
    "language": "TELUGU",
    "districtId": 1,
    "mandalId": 1
  }'
```
**Returns:** `{farmerId: 1, userName: "farmer_radha", message: "Registration successful"}`

---

### Step 2: Login
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/farmers/login \
  -H "Content-Type: application/json" \
  -d '{"userName": "farmer_radha", "password": "SecurePass@123"}' \
  | jq -r '.data.token')
echo $TOKEN
```

---

### Step 3: Save Known Fields
```bash
curl -X POST http://localhost:8080/api/schemes/user-fields \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "farmerId": 1,
    "fields": [
      {"fieldName": "landSize", "value": "1.5", "fieldType": "NUMBER"},
      {"fieldName": "income", "value": "120000", "fieldType": "NUMBER"},
      {"fieldName": "cropType", "value": "Paddy", "fieldType": "STRING"}
    ]
  }'
```

---

### Step 4: Check Eligibility (DISCOVER Mode)
```bash
curl -X POST http://localhost:8080/api/schemes/check-eligibility \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "farmerId": 1,
    "mode": "DISCOVER",
    "additionalFields": {
      "landSize": "1.5",
      "income": "120000"
    }
  }'
```

---

### Step 5: Check Eligibility (VERIFY Mode)
```bash
curl -X POST http://localhost:8080/api/schemes/check-eligibility \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "farmerId": 1,
    "mode": "VERIFY",
    "additionalFields": {
      "landSize": "1.5",
      "income": "120000",
      "cropType": "Paddy"
    }
  }'
```

---

### Step 6: Browse Schemes
```bash
# All schemes
curl http://localhost:8080/api/schemes/browse

# Only Central government schemes
curl http://localhost:8080/api/schemes/browse?state=CENTRAL

# Only Telangana state schemes
curl http://localhost:8080/api/schemes/browse?state=Telangana

# Only Agriculture category
curl http://localhost:8080/api/schemes/browse?category=AGRICULTURE_SUBSIDY
```

---

### Step 7: View Scheme Detail
```bash
# English
curl http://localhost:8080/api/schemes/1?language=EN

# Telugu
curl http://localhost:8080/api/schemes/1?language=TE

# Hindi
curl http://localhost:8080/api/schemes/1?language=HI
```

---

## Common Scenarios

### Scenario 1: Farmer Nearly Qualifies (Almost Eligible)
```
Farmer: landSize=1.5 ✅, income=160000 ❌
Result: PM-KISAN in almostEligibleSchemes
Message: "You could also qualify for PM-KISAN if 
          your annual income is below ₹1.5L"
```

### Scenario 2: State-Specific Scheme
```
Farmer from Telangana saves: state=Telangana
Checks eligibility
Result: Rythu Bandhu appears in eligible schemes
        (only farmers from Telangana can access it)
```

### Scenario 3: Crop-Based Scheme
```
Farmer grows: cropType="Paddy,Wheat"
PMFBY rule: cropType IN [Paddy,Wheat,Maize,...]
Result: ✅ ELIGIBLE (both Paddy and Wheat in list)
```

### Scenario 4: Missing Field in DISCOVER vs VERIFY
```
DISCOVER mode (partial data):
- Provided: landSize=1.5
- Missing: income
- Result: Rules skip missing field, show potential matches

VERIFY mode (complete data):
- Provided: landSize=1.5
- Missing: income
- Result: Rules fail due to missing required field
```

---

## Error Handling

### 400 Bad Request
```json
{
  "status": 400,
  "message": "Registration failed",
  "error": "Username already exists"
}
```

### 401 Unauthorized
```json
{
  "status": 401,
  "message": "Login failed",
  "error": "Invalid password"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "message": "Scheme not found",
  "error": "Scheme not found with id: 999"
}
```

---

## Response Status Codes

| Code | Condition |
|------|-----------|
| 200 | Success (GET, POST update) |
| 201 | Created (POST register) |
| 400 | Bad request / validation failed |
| 401 | Unauthorized / invalid token |
| 404 | Resource not found |
| 500 | Server error |

---

## Field Extraction Order

Farmer eligibility data comes from (in priority):

1. **Farmer Profile** (auto-derived):
   - `age` = computed from `dob` using Period.between()
   - `state` = derived from district → state relationship

2. **UserKnownFields** (from database):
   - `landSize`, `income`, `cropType`, etc.

3. **Request additionalFields** (override all above):
   - Any field provided in the API request

---

## Testing Rules of Thumb

✅ Always save user fields before checking eligibility  
✅ Use DISCOVER mode for quick checks with partial data  
✅ Use VERIFY mode for complete eligibility assessment  
✅ Check "almost eligible" scenarios to fine-tune rules  
✅ Test boundary values (exactly at operator limits)  
✅ Test multiple crops with comma-separated values  
✅ Verify language filters in FAQ responses  
