# GramVikash Complete API Testing Guide

## File Location
📄 **`COMPLETE_TEST_DATA.json`** — Contains all test data organized by feature

---

## 1️⃣ FARMER AUTHENTICATION APIs

### Register New Farmer
```bash
POST /api/farmers/register
Content-Type: application/json

{
  "phoneNumber": "9876543210",
  "userName": "farmer_radha",
  "password": "SecurePass@123",
  "fullName": "Radha Kumar",
  "dob": "1981-05-15",              ← LocalDate format
  "language": "TELUGU",
  "districtId": 1,
  "mandalId": 1
}
```
**Expected:** 201 Created with farmerId

---

### Login & Get Token
```bash
POST /api/farmers/login
{
  "userName": "farmer_radha",
  "password": "SecurePass@123"
}
```
**Expected:** Token + userId + "Login successful"

---

### Get Farmer Profile
```bash
GET /api/farmers/profile/farmer_radha
Headers: Authorization: Bearer <token>
```
**Expected:**
- `dob`: "1981-05-15"
- `age`: Computed from dob (currently 43)
- `stateName`: Derived from district → state
- `districtName`, `mandalName`: From relationships

---

### Change Password
```bash
POST /api/farmers/change-password
Headers: Authorization: Bearer <token>

{
  "userName": "farmer_radha",
  "oldPassword": "SecurePass@123",
  "newPassword": "NewPassword@999"
}
```

---

## 2️⃣ SCHEME CREATION APIs

### Create Scheme (5 Examples)

#### a) **PM-KISAN** (Central + Income-Based + Land-Based)
- AND operator for basic requirements
- Rules: age >= 18, landSize <= 2 hectares, income <= ₹150,000
- Multilingual FAQs (EN, TE, HI)

#### b) **Soil Health Card** (Central + Minimal Requirements)
- age >= 18, landSize >= 0.5 hectares
- Simplest eligibility

#### c) **Rythu Bandhu** (State-Specific + Telangana)
- state = "Telangana" (string equality)
- Demonstrates state-based filtering

#### d) **PMFBY** (Crop Insurance + OR Logic)
- AND group: farmer requirements (age, land)
- OR group: cropType IN [Paddy, Wheat, Maize, Cotton, Sugarcane, Groundnut, Soybean]
- Demonstrates multi-group eligibility

#### e) **PMAY-G** (Housing + Income Based)
- income <= ₹200,000
- Demonstrates different income thresholds

---

## 3️⃣ USER KNOWN FIELDS APIs

### Save/Update Farmer's Profile Data
```bash
POST /api/schemes/user-fields
Headers: Authorization: Bearer <token>

{
  "farmerId": 1,
  "fields": [
    {"fieldName": "landSize", "value": "1.5", "fieldType": "NUMBER"},
    {"fieldName": "income", "value": "120000", "fieldType": "NUMBER"},
    {"fieldName": "cropType", "value": "Paddy", "fieldType": "STRING"}
  ]
}
```

**Supported Fields:**
- `landSize` (NUMBER)
- `income` (NUMBER)
- `cropType` (STRING) — can be comma-separated: "Paddy,Wheat"
- Any custom field name

---

## 4️⃣ ELIGIBILITY CHECK API (CORE ENGINE)

### DISCOVER Mode (Partial Information)
```bash
POST /api/schemes/check-eligibility
Headers: Authorization: Bearer <token>

{
  "farmerId": 1,
  "mode": "DISCOVER",
  "additionalFields": {
    "landSize": "1.5",
    "income": "120000"
  }
}
```

**Behavior:**
- ✅ Skips rules for missing fields
- ✅ Shows schemes farmer **might** qualify for
- ✅ Returns `missingFields` array

**Response:**
```json
{
  "mode": "DISCOVER",
  "eligibleSchemes": [...],
  "almostEligibleSchemes": [...],
  "ineligibleSchemes": [...]
}
```

---

### VERIFY Mode (Complete Profile)
```bash
POST /api/schemes/check-eligibility
{
  "farmerId": 1,
  "mode": "VERIFY",
  "additionalFields": {
    "landSize": "1.5",
    "income": "120000",
    "cropType": "Paddy",
    "state": "Telangana"
  }
}
```

**Behavior:**
- ✅ Evaluates **all rules strictly**
- ✅ Missing field = rule failure
- ✅ Returns complete eligibility assessment

---

### "Almost Eligible" Scenario
```
Farmer data:
- landSize: 1.5 ✅ (< 2)
- income: 160,000 ❌ (> 150,000 limit)

Result:
- PM-KISAN appears in "almostEligibleSchemes"
- reasonMessage: "You could also qualify for PM-KISAN 
                  if your annual income is below ₹1.5L"
```

---

### Field Extraction Hierarchy
```
Farmer eligibility data comes from (in order):
1. Farmer Profile: age (computed from dob), state (via district)
2. UserKnownFields: landSize, income, cropType, etc.
3. Request additionalFields: overrides both above
```

---

## 5️⃣ BROWSE SCHEMES APIs

### Get All Schemes
```bash
GET /api/schemes/browse
```
**Returns:** All active schemes as cards

---

### Filter by State
```bash
GET /api/schemes/browse?state=CENTRAL
```
Returns only central schemes (state = null)

```bash
GET /api/schemes/browse?state=Telangana
```
Returns only Telangana schemes

---

### Filter by Category
```bash
GET /api/schemes/browse?category=AGRICULTURE_SUBSIDY
```

**Available Categories:**
- AGRICULTURE_SUBSIDY
- SOIL_TESTING
- CROP_INSURANCE
- HOUSING

---

### Combined Filters
```bash
GET /api/schemes/browse?state=CENTRAL&category=CROP_INSURANCE
```

---

## 6️⃣ SCHEME DETAIL & MULTILINGUAL FAQ

### Get English FAQs
```bash
GET /api/schemes/1?language=EN
```

### Get Telugu FAQs
```bash
GET /api/schemes/1?language=TE
```

### Get Hindi FAQs
```bash
GET /api/schemes/1?language=HI
```

**Response Fields:**
```json
{
  "id": 1,
  "schemeName": "PM-KISAN Samman Nidhi",
  "schemeCode": "PMKISAN-001",
  "description": "...",
  "benefitDetails": "...",
  "eligibilityGroups": [{...}],
  "faqs": [
    {
      "id": 1,
      "question": "...",
      "answer": "...",
      "language": "EN",
      "displayOrder": 1
    }
  ]
}
```

---

## 7️⃣ COMPLETE TESTING FLOWS

### Complete New Farmer Journey
```
1. POST /api/farmers/register
   → Get farmerId

2. POST /api/farmers/login
   → Get JWT token

3. POST /api/schemes/user-fields
   → Save land, income, crops

4. POST /api/schemes/check-eligibility?mode=DISCOVER
   → Quick eligibility check

5. GET /api/schemes/browse?category=AGRICULTURE_SUBSIDY
   → Browse matching schemes

6. GET /api/schemes/1?language=TE
   → View scheme detail in Telugu
```

---

### Eligibility Scenario: Income Change
```
Farmer: landSize = 1.5, income = 120,000 initially

1. Save updated income: 160,000
2. Check eligibility again
3. Result: PM-KISAN moves from "eligible" → "almostEligible"
4. Reason: "You could also qualify for PM-KISAN 
            if your annual income is below ₹1.5L"
```

---

## 8️⃣ EDGE CASES

### Age Boundary
- Farmer born exactly 18 years ago → ELIGIBLE
- Farmer age 17 → INELIGIBLE

### Land Limit
```
PM-KISAN: landSize <= 2

- 1.9 hectares ✅
- 2.0 hectares ✅ (inclusive)
- 2.1 hectares ❌
```

### Income Limit
```
PM-KISAN: income <= 150,000

- ₹150,000 ✅ (inclusive)
- ₹150,001 ❌
```

### Crop IN Operator
```
Rule: cropType IN ["Paddy,Wheat,Maize,Cotton,Sugarcane,Groundnut,Soybean"]

Farmer data:
- "Paddy" ✅
- "Paddy,Wheat" ✅ (comma-separated, one matches)
- "Sunflower" ❌ (not in list)
```

### VERIFY Mode Missing Field
```
Mode: VERIFY
Rule requires: income
Farmer data: {"landSize": "1.5"}  (no income)

Result: Rule FAILS (missing field treated as failure in VERIFY)
```

---

## Key Implementation Details

### Rule Operators
| Operator | Type | Example |
|----------|------|---------|
| `=` | All | age = 25 |
| `>` | NUMBER | income > 100000 |
| `>=` | NUMBER | landSize >= 2 |
| `<` | NUMBER | age < 65 |
| `<=` | NUMBER | income <= 150000 |
| `!=` | NUMBER, STRING | status != DECEASED |
| `IN` | STRING | cropType IN [Paddy,Wheat,Maize] |

### Group Operators
| Operator | Behavior |
|----------|----------|
| `AND` | All rules must pass (missing field = skip in DISCOVER, fail in VERIFY) |
| `OR` | At least one rule must pass |

### Field Types
- `NUMBER` — Parsed as double, operators: =, >, <, >=, <=, !=
- `STRING` — Case-insensitive, operators: =, !=, IN
- `BOOLEAN` — Parsed as boolean, operators: =, !=

---

## Testing Checklist

- [ ] Register multiple farmers with different DOBs
- [ ] Login and validate token
- [ ] Change password
- [ ] Create all 5 sample schemes
- [ ] Save user known fields
- [ ] Test DISCOVER mode with partial data
- [ ] Test VERIFY mode with complete data
- [ ] Verify "almost eligible" scenarios
- [ ] Browse by state (Central vs Telangana)
- [ ] Browse by category
- [ ] Get scheme detail in EN/TE/HI
- [ ] Test age boundary (exactly 18)
- [ ] Test land boundary (exactly 2 hectares)
- [ ] Test income boundary (exactly ₹150,000)
- [ ] Test crop matching (IN operator)
- [ ] Test missing fields in VERIFY mode

---

## Example curl Commands

### Register
```bash
curl -X POST http://localhost:8080/api/farmers/register \
  -H "Content-Type: application/json" \
  -d @- << 'EOF'
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
EOF
```

### Login
```bash
curl -X POST http://localhost:8080/api/farmers/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"farmer_radha","password":"SecurePass@123"}'
```

### Check Eligibility
```bash
curl -X POST http://localhost:8080/api/schemes/check-eligibility \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d @- << 'EOF'
{
  "farmerId": 1,
  "mode": "VERIFY",
  "additionalFields": {
    "landSize": "1.5",
    "income": "120000",
    "cropType": "Paddy"
  }
}
EOF
```

### Browse Central Schemes
```bash
curl http://localhost:8080/api/schemes/browse?state=CENTRAL
```

### Get Scheme with Telugu FAQs
```bash
curl http://localhost:8080/api/schemes/1?language=TE
```

---

## Notes

- **DOB Format:** YYYY-MM-DD (ISO 8601)
- **Age Calculation:** Computed from DOB on-the-fly in profile response based on `Period.between(dob, LocalDate.now())`
- **State Hierarchy:** State → Districts → Mandals
- **Default Language:** EN if not specified
- **All dates stored as:** LocalDate (no time component)
