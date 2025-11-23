# Login Workflow Documentation

## Overview
This document describes the complete login authentication workflow for the Kabengosafaris application, including account security checks, rate limiting, account lockout mechanisms, and failed attempt management.

---

## Architecture Components

### Primary Services
- **LoginService** - Orchestrates the login authentication flow
- **LoginRateLimitingService** - Implements token bucket algorithm for brute-force prevention
- **AccountMaintenanceScheduledService** - Background scheduler for account maintenance

### Security Models
- **User** - Entity with security attributes (enabled, accountLocked, failedAttempt, etc.)
- **SecuritySettings** - Database-driven configuration for all security parameters

### Database Configuration
All security settings are database-driven and can be modified at runtime without code changes:
- `accountLockout.maxFailedAttempts` - Default: 5
- `accountLockout.lockoutDurationMinutes` - Default: 30
- `accountLockout.counterResetHours` - Default: 24 (0 = never reset)
- `loginAttempts.maxCapacity` - Default: 5 (token bucket burst capacity)
- `loginAttempts.refillRate` - Default: 5 (tokens to refill)
- `loginAttempts.refillDurationMinutes` - Default: 1

---

## Complete Login Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       USER INITIATES LOGIN ATTEMPT                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 0] RATE LIMITING CHECK                                                │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Service: LoginRateLimitingService.isAllowed(identifier)                     │
│                                                                             │
│ Algorithm: Token Bucket (per username/email)                                │
│ • Bucket starts with maxCapacity tokens                                     │
│ • Each login attempt consumes 1 token                                       │
│ • After refillDurationMinutes, refillRate tokens are added back             │
│                                                                             │
│ Example: maxCapacity=5, refillRate=5, refillDurationMinutes=1               │
│   • User gets 5 instant attempts                                            │
│   • After 1 minute: 5 tokens refill                                         │
│   • After 2 minutes: 10 tokens available (capped at max)                    │
│                                                                             │
│ ┌─ Is allowed?                                                              │
│ │  ├─ YES → Continue to Step 1                                              │
│ │  └─ NO  → Throw LoginException("Too many login attempts...")              │
│ │           (Client: "Please try again later")                              │
│ └───────────────────────────────────────────────────────────────────────────│
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 1] VALIDATE INPUT                                                     │
│ ─────────────────────────────────────────────────────────────────────────── │
│ • Identifier (email or username) is not null/empty                          │
│ • Password is not null/empty                                                │
│                                                                             │
│ ┌─ All required fields present?                                             │
│ │  ├─ YES → Continue to Step 2                                              │
│ │  └─ NO  → Throw LoginException("Email/Username must be provided...")      │
│ └───────────────────────────────────────────────────────────────────────    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 2] FIND USER IN DATABASE                                              │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Method: UserRepository.findByEmail() or findByUsername()                    │
│                                                                             │
│ ┌─ User exists?                                                             │
│ │  ├─ YES → Load User object with all security attributes                   │
│ │  │         (enabled, accountLocked, failedAttempt, etc.)                  │
│ │  └─ NO  → Throw LoginException("Invalid email or password")               │
│ │           (Generic message to prevent user enumeration attacks)           │
│ └───────────────────────────────────────────────────────────────────────    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 3] CHECK ACCOUNT ENABLED STATUS                                       │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Method: User.isEnabled()                                                    │
│ Database Field: users.enabled                                               │
│                                                                             │
│ ┌─ Is account enabled?                                                      │
│ │  ├─ YES → Continue to Step 4                                              │
│ │  └─ NO  → Throw LoginException("Your account is disabled or email...")    │
│ │           (Client: "Contact support or verify email")                     │
│ └───────────────────────────────────────────────────────────────────────    │
│                                                                             │
│ USE CASES FOR DISABLED ACCOUNTS:                                            │
│ • Email not verified yet (registration incomplete)                          │
│ • Administrator disabled account (account suspension)                       │
│ • User requested account deactivation                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 4] RESET FAILED ATTEMPT COUNTER (if expired)                          │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Method: LoginService.resetCounterIfExpired(user)                            │
│                                                                             │
│ Checks if counterResetHours has expired since last failed attempt:          │
│ • Get counterResetHours from database (default: 24)                         │
│ • If 0: counter never auto-resets (manual reset only)                       │
│ • If > 0: check if lastFailedAttemptTime + counterResetHours < now          │
│                                                                             │
│ ┌─ Counter should be reset?                                                 │
│ │  ├─ YES → Reset failedAttempt to 0 in database, and                       |
| |  |  lastFailedAttemptTime to null                                         │
│ │  │         (User gets fresh attempts)                                     │
│ │  └─ NO  → Continue with current counter value                             │
│ └───────────────────────────────────────────────────────────────────────────│
│                                                                             │
│ EXAMPLE TIMELINE:                                                           │
│ • 10:00 AM - User fails 3 login attempts → failedAttempt=3                  │
│ • 10:00 AM - lastFailedAttemptTime = 2024-11-20 10:00:00                    │
│ • 10:15 AM - User tries to login                                            │
│ •            Check: 10:15 + 24h > 10:00? NO → Keep counter at 3             │
│ • 11:00 PM (next day) - User tries to login                                 │
│ •            Check: 11:00 + 24h > 10:00? YES → Reset to 0                   │
│ •            User now has 5 fresh attempts                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 5] CHECK ACCOUNT LOCKED STATUS                                        │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Method: User.isAccountNonLocked()                                           │
│ Database Field: users.accountLocked                                         │
│                                                                             │
│ ┌─ Is account locked?                                                       │
│ │  ├─ NO  → Continue to Step 6                                              │
│ │  └─ YES → Check if lock period has expired (Step 5a)                      │
│ └───────────────────────────────────────────────────────────────────────    │
│                                                                             │
│ ┌─ STEP 5a: CHECK LOCK EXPIRATION                                           │
│ │ Method: LoginService.unlockWhenTimeExpired(user)                          │
│ │                                                                           │
│ │ • Get lockoutDurationMinutes from database (default: 30)                  │
│ │ • Calculate: unlockTime = accountLockedTime + lockoutDurationMinutes      │
│ │ • If now > unlockTime: unlock the account automatically                   │
│ │                                                                           │
│ │ EXAMPLE SCENARIO 1: Lock period expired                                   │
│ │ • 10:00 AM - User locked after 5 failed attempts                          │
│ │ •            accountLockedTime = 2024-11-20 10:00:00                      │
│ │ • 10:30 AM - User attempts login                                          │
│ │ •            Check: 10:30 > 10:00 + 30min? YES                            │
│ │ •            Action: accountLocked = false, unlock account                │
│ │ •            Reset failedAttempt to 0                                     │
│ │ •            User continues to Step 6 for authentication                  │
│ │                                                                           │
│ │ EXAMPLE SCENARIO 2: Lock period NOT expired                               │
│ │ • 10:00 AM - User locked after 5 failed attempts                          │
│ │ •            accountLockedTime = 2024-11-20 10:00:00                      │
│ │ • 10:15 AM - User attempts login                                          │
│ │ •            Check: 10:15 > 10:00 + 30min? NO                             │
│ │ •            Action: Throw exception, reject login                        │
│ │ •            Message: "Your account is locked. Try again later."          │
│ └───────────────────────────────────────────────────────────────────────    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 6] CHECK PASSWORD EXPIRATION                                          │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Method: User.isCredentialsNonExpired()                                      │
│ Database Field: users.passwordExpiryDate                                    │
│                                                                             │
│ • If passwordExpiryDate is NULL: password never expires                     │
│ • If passwordExpiryDate is set: check if now > passwordExpiryDate           │
│                                                                             │
│ ┌─ Has password expired?                                                    │
│ │  ├─ NO  → Continue to Step 7 (authentication)                             │ 
│ │  └─ YES → Throw LoginException("Your password has expired since...")      │
│ │           (Client: "Please reset your password")                          │
│ └───────────────────────────────────────────────────────────────────────    │
│                                                                             │
│ EXAMPLE:                                                                    │
│ • passwordExpiryDate = 2024-11-15 00:00:00                                  │
│ • Today = 2024-11-20 14:30:00                                               │
│ • Check: 2024-11-20 > 2024-11-15? YES → Expired                             │
│ • Result: Reject login, force password reset                                │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ [STEP 7] AUTHENTICATE CREDENTIALS                                           │
│ ─────────────────────────────────────────────────────────────────────────── │
│ Framework: Spring Security AuthenticationManager                            │
│ Method: authenticationManager.authenticate()                                │
│                                                                             │
│ • Creates UsernamePasswordAuthenticationToken(username, password)           │
│ • Delegates to DaoAuthenticationProvider                                    │
│ • Provider calls UserDetailsService to load user                            │
│ • Compares provided password with stored hash (BCrypt)                      │
│ • Calls User.isAccountNonLocked(), isEnabled(), isCredentialsNonExpired()   │
│                                                                             │
│ ┌─ Are credentials valid AND all checks pass?                               │
│ │  ├─ YES → Continue to Step 8a (Success)                                   │
│ │  └─ NO  → Jump to Step 8b (Failed)                                        │
│ └───────────────────────────────────────────────────────────────────────    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
        ┌───────────────────────────┴───────────────────────────┐
        ↓                                                       ↓
┌─────────────────────────────────┐  ┌──────────────────────────────────────┐
│ [STEP 8a] SUCCESSFUL LOGIN      │  │ [STEP 8b] FAILED AUTHENTICATION      │
│ ─────────────────────────────── │  │ ──────────────────────────────────── │
│                                 │  │                                      │
│ 1. Reset Failed Attempts:       │  │ 1. Get maxFailedAttempts from DB:    │
│    failedAttempt = 0            │  │    (default: 5)                      │
│    lastFailedAttemptTime = null │  │                                      │
│                                 │  │ 2. Increment Counter:                │
│ 2. Generate JWT Tokens:         │  │    ┌─ failedAttempt >= (5-1)?        │
│    • Access Token               │  │    │  ├─ YES (≥ 4) → Lock account    │
│    • Refresh Token              │  │    │  │   Set accountLocked = true   │
│    • Set expiration times       │  │    │  │   Set accountLockedTime      │
│    • Sign with secret key       │  │    │  │   Save to database           │
│    • Add claims (userId, role)  │  │    │  │   Throw exception            │
│                                 │  │    │  │                              │
│ 3. Return LoginResponse:        │  │    │  └─ NO (< 4) → Increment only   │
│    {                            │  │    │      failedAttempt++            │
│      accessToken: "...",        │  │    │      lastFailedAttemptTime      │
│      refreshToken: "...",       │  │    │      = now                      │
│      accessTokenExpiresIn: ms,  │  │    │      Save to database           │
│      refreshTokenExpiresIn: ms, │  │    │      Throw exception            │
│      ...                        │  │    └─────────────────────────────────│
│    }                            │  │                                      │ 
│                                 │  │ LOCK ACCOUNT EXAMPLE:                │ 
│ 4. Audit Logging:               │  │ • Current: failedAttempt = 4         │ 
│    - Log successful login       │  │ • This attempt: failedAttempt = 5    │ 
│    - Last login timestamp       │  │ • Check: 5 >= 4? YES                 │ 
│    - IP address (if tracked)    │  │ • Action: Lock account               │ 
│                                 │  │ • Message: "Account locked"          │ 
│ SECURITY NOTES:                 │  │                                      │
│ • Tokens are stateless          │  │ RESET COUNTER EXAMPLE:               │
│ • Cannot be revoked (TTL-based) │  │ • Current: failedAttempt = 2         │
│ • Should be stored securely     │  │ • This attempt: failedAttempt = 3    │
│ • Should be sent in headers     │  │ • Check: 3 >= 4? NO                  │
│                                 │  │ • Action: Just increment             │
└─────────────────────────────────┘  │ • Message: "Invalid credentials"     │
                                     │                                      │
                                     │ TRACKING TIMESTAMP:                  │
                                     │ • Each failed attempt logs time      │
                                     │ • Used by counterResetHours logic    │
                                     │ • After 24h: counter resets to 0     │
                                     │                                      │
                                     └──────────────────────────────────────┘
```

---

## Detailed Component Workflows

### 1. LoginRateLimitingService (Token Bucket Algorithm)

**Purpose**: Prevent brute-force attacks by limiting login attempts per user/email.

**How It Works**:
```
Bucket State Over Time (maxCapacity=5, refillRate=5, refillDurationMinutes=1)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Time: 10:00:00 - User created/first login
Tokens: ████████░░ (5/5 capacity) ← Starts with full bucket

Time: 10:00:10 - User attempts login #1
Tokens: ███████░░░ (4/5) ← One token consumed

Time: 10:00:15 - User attempts login #2
Tokens: ██████░░░░ (3/5) ← One token consumed

Time: 10:00:20 - User attempts login #3
Tokens: █████░░░░░ (2/5) ← One token consumed

Time: 10:00:25 - User attempts login #4
Tokens: ████░░░░░░ (1/5) ← One token consumed

Time: 10:00:30 - User attempts login #5
Tokens: ███░░░░░░░ (0/5) ← One token consumed, RATE LIMITED

Time: 10:00:35 - User attempts login #6
Result: DENIED ← No tokens available
Message: "Too many login attempts. Please try again later."

Time: 10:01:00 - Refill period expires, refillRate tokens added
Tokens: ████████░░ (5/5) ← Refilled with 5 tokens, capped at max

Time: 10:01:05 - User attempts login #7
Tokens: ███████░░░ (4/5) ← One token consumed, NOW ALLOWED
```

**Database Settings**:
- `loginAttempts.maxCapacity`: 5 (initial burst capacity)
- `loginAttempts.refillRate`: 5 (tokens per refill)
- `loginAttempts.refillDurationMinutes`: 1 (refill interval)

**Implementation Details**:
```java
// From LoginRateLimitingService
public boolean isAllowed(String username) {
    Bucket bucket = buckets.computeIfAbsent(username, this::createNewBucket);
    boolean allowed = bucket.tryConsume(1);  // Try to consume 1 token
    return allowed;
}
```

---

### 2. LoginService Authentication Flow

**Primary Method**: `login(LoginRequest loginRequest)`

**Security Checks Order**:
1. **Rate Limiting** (Step 0)
2. **Input Validation** (Step 1)
3. **User Lookup** (Step 2)
4. **Account Enabled** (Step 3)
5. **Counter Reset** (Step 4)
6. **Account Lock Check** (Step 5)
7. **Password Expiration** (Step 6)
8. **Credential Authentication** (Step 7)
9. **Success or Failure Handling** (Step 8a/8b)

**Code References**:
- [LoginService.java:38-134](LoginService.java#L38-L134) - Main login orchestration
- [LoginService.java:138-158](LoginService.java#L138-L158) - resetCounterIfExpired()
- [LoginService.java:152-172](LoginService.java#L152-L172) - unlockWhenTimeExpired()

---

### 3. AccountMaintenanceScheduledService (Background Tasks)

**Purpose**: Automatically unlock accounts and reset counters without user interaction.

#### Task 1: Auto-Unlock Expired Accounts
```
SCHEDULED TASK: unlockExpiredAccounts()
Run frequency: Every 5 minutes
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

10:00 AM - Schedule triggered
├─ Query: SELECT * FROM users WHERE accountLocked = true
├─ Found: 3 locked accounts
│
├─ Account 1: john_doe
│  ├─ accountLockedTime: 10:05 AM
│  ├─ lockoutDurationMinutes: 30 (from database)
│  ├─ unlockTime: 10:05 AM + 30 min = 10:35 AM
│  ├─ Check: 10:00 AM > 10:35 AM? NO
│  └─ Action: Keep locked (21 minutes remaining)
│
├─ Account 2: jane_smith
│  ├─ accountLockedTime: 9:15 AM
│  ├─ lockoutDurationMinutes: 30 (from database)
│  ├─ unlockTime: 9:15 AM + 30 min = 9:45 AM
│  ├─ Check: 10:00 AM > 9:45 AM? YES
│  └─ Action: UNLOCK
│     ├─ Set accountLocked = false
│     ├─ Set accountLockedTime = null
│     ├─ Set failedAttempt = 0
│     ├─ Set lastFailedAttemptTime = null
│     └─ Log: "Account automatically unlocked for user: jane_smith"
│
└─ Account 3: bob_wilson
   ├─ accountLockedTime: 9:00 AM
   ├─ lockoutDurationMinutes: 30 (from database)
   ├─ unlockTime: 9:00 AM + 30 min = 9:30 AM
   ├─ Check: 10:00 AM > 9:30 AM? YES
   └─ Action: UNLOCK
      ├─ Set accountLocked = false
      ├─ Set accountLockedTime = null
      ├─ Set failedAttempt = 0
      ├─ Set lastFailedAttemptTime = null
      └─ Log: "Account automatically unlocked for user: bob_wilson"

10:05 AM - Schedule triggered again
├─ Query: SELECT * FROM users WHERE accountLocked = true
├─ Found: 1 locked account (john_doe - 19 minutes remaining)
└─ No actions taken
```

#### Task 2: Reset Failed Attempt Counters
```
SCHEDULED TASK: resetExpiredFailedAttemptCounters()
Run frequency: Every 10 minutes
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

10:00 AM - Schedule triggered
├─ Get counterResetHours from database: 24
├─ If 0: Skip (counter never auto-resets)
├─ If > 0: Continue
│
├─ Query: SELECT * FROM users WHERE failedAttempt > 0
├─ Found: 5 users with failed attempts
│
├─ User 1: alice_wonder
│  ├─ failedAttempt: 3
│  ├─ lastFailedAttemptTime: Yesterday 9:00 AM
│  ├─ resetDeadline: Yesterday 9:00 AM + 24h = Today 9:00 AM
│  ├─ Check: 10:00 AM > 9:00 AM? YES
│  └─ Action: RESET
│     ├─ Set failedAttempt = 0
│     ├─ Set lastFailedAttemptTime = null
│     └─ Log: "Failed attempt counter reset for user: alice_wonder"
│
├─ User 2: charlie_brown
│  ├─ failedAttempt: 2
│  ├─ lastFailedAttemptTime: Today 10:30 AM
│  ├─ resetDeadline: Today 10:30 AM + 24h = Tomorrow 10:30 AM
│  ├─ Check: 10:00 AM > Tomorrow 10:30 AM? NO
│  └─ Action: Keep (23 hours 30 minutes remaining)
│
└─ ... (continue for remaining users)

IMPACT ON NEXT LOGIN:
• alice_wonder can now login with fresh 5 attempts
• charlie_brown still has 2 failed attempts tracked (needs 2 more to lock)
```

---

## Failed Attempt Lifecycle

```
TIMELINE: User Fails Multiple Login Attempts
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

DAY 1 - 10:00 AM
├─ Attempt #1 (wrong password)
│  ├─ failedAttempt: 0 → 1
│  ├─ lastFailedAttemptTime: 10:00 AM
│  ├─ accountLocked: false
│  └─ Status: User can retry
│
├─ Attempt #2 (wrong password) @ 10:05 AM
│  ├─ failedAttempt: 1 → 2
│  ├─ lastFailedAttemptTime: 10:05 AM (updated)
│  ├─ accountLocked: false
│  └─ Status: User can retry
│
├─ Attempt #3 (wrong password) @ 10:10 AM
│  ├─ failedAttempt: 2 → 3
│  ├─ lastFailedAttemptTime: 10:10 AM (updated)
│  ├─ accountLocked: false
│  └─ Status: User can retry
│
├─ Attempt #4 (wrong password) @ 10:15 AM
│  ├─ failedAttempt: 3 → 4
│  ├─ lastFailedAttemptTime: 10:15 AM (updated)
│  ├─ accountLocked: false
│  └─ Status: User can retry (1 more attempt before lock)
│
└─ Attempt #5 (wrong password) @ 10:20 AM
   ├─ Check: failedAttempt (4) >= maxFailedAttempts - 1 (4)? YES
   ├─ Action: LOCK ACCOUNT
   │  ├─ failedAttempt: 4 (remains as-is)
   │  ├─ accountLocked: true
   │  ├─ accountLockedTime: 10:20 AM
   │  └─ lastFailedAttemptTime: 10:20 AM (updated)
   └─ Status: ACCOUNT LOCKED ❌

DAY 1 - 10:30 AM (User tries again)
├─ Rate Limiting: Allowed (has tokens)
├─ Step 3: Account enabled? Yes
├─ Step 4: Counter reset? No (only 10 min elapsed, need 24h)
├─ Step 5: Account locked? Yes
│  ├─ Check: 10:30 AM > 10:20 AM + 30 min? NO
│  └─ Action: Reject (10 more minutes until auto-unlock)
└─ Status: Still locked ❌

DAY 1 - 10:50 AM (Scheduled task runs)
├─ Task: Check locked accounts
├─ Found: This user (locked since 10:20 AM)
├─ Check: 10:50 AM > 10:20 AM + 30 min? YES
├─ Action: AUTO-UNLOCK
│  ├─ accountLocked: false
│  ├─ accountLockedTime: null
│  ├─ failedAttempt: 0 (reset)
│  └─ lastFailedAttemptTime: null (cleared)
└─ Status: UNLOCKED ✓

DAY 1 - 11:00 AM (User tries again after auto-unlock)
├─ Rate Limiting: Allowed
├─ Step 3: Account enabled? Yes
├─ Step 4: Counter reset? Not needed (already 0)
├─ Step 5: Account locked? No
├─ Step 6: Password expired? No
├─ Step 7: Credentials valid? YES
└─ Status: LOGIN SUCCESS ✓

ALTERNATIVE: Manual Counter Reset After 24 Hours
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

DAY 2 - 10:30 AM (24+ hours later, scheduled task runs)
├─ Task: Check users with failedAttempt > 0
├─ If user never tried again after lock:
│  ├─ lastFailedAttemptTime: Still 10:20 AM (Day 1)
│  ├─ Check: 10:30 AM (Day 2) > 10:20 AM (Day 1) + 24h? YES
│  ├─ Action: RESET COUNTER
│  │  ├─ failedAttempt: X → 0
│  │  └─ lastFailedAttemptTime: null
│  └─ User now has 5 fresh attempts on next login
└─ Status: Counter expired ✓
```

---

## Security Best Practices Implemented

### 1. **Rate Limiting (Token Bucket)**
- **Purpose**: Prevent brute-force attacks
- **Method**: Token bucket algorithm per username/email
- **Configuration**: Database-driven, adjustable without restart
- **Fail-Safe**: Allows login on rate limiter failures (fail-open)

### 2. **Account Lockout**
- **Purpose**: Lock account after multiple failed attempts
- **Trigger**: Configurable via `accountLockout.maxFailedAttempts` (default: 5)
- **Duration**: Configurable via `accountLockout.lockoutDurationMinutes` (default: 30)
- **Auto-Unlock**: Scheduled task unlocks after duration expires

### 3. **Failed Attempt Tracking**
- **Timestamp**: `lastFailedAttemptTime` tracks WHEN attempts fail
- **Counter Reset**: Automatically reset after `counterResetHours` (default: 24)
- **Fresh Attempts**: Users can retry after inactivity period
- **Flexibility**: Set to 0 to disable auto-reset (permanent manual reset only)

### 4. **Account Status Validation**
- **UserDetails Implementation**: Spring Security validates all checks
- **Enabled Status**: Prevents login to disabled/unverified accounts
- **Password Expiration**: Enforces password rotation policy
- **Account Expiration**: Ready for future implementation

### 5. **Generic Error Messages**
- **User Enumeration Prevention**: "Invalid email or password" doesn't reveal if account exists
- **Security**: Prevents attackers from discovering valid usernames/emails

### 6. **Stateless JWT Authentication**
- **No Session State**: Tokens are cryptographically signed
- **TTL-Based**: Tokens expire based on configured duration
- **Cannot Be Revoked**: Logout is client-side only (delete token)
- **Secure Transport**: Should be sent in Authorization header

---

## Configuration Reference

All settings are stored in `security_settings` table and can be modified via admin API:

```
┌──────────────────────────────────────────────────────────────────┐
│                    SECURITY SETTINGS                             │
├────────────────────────┬──────────────────────────┬──────────────┤
│ Setting Key            │ Default Value            │ Category     │
├────────────────────────┼──────────────────────────┼──────────────┤
│ accountLockout.        │ 5                        │ ACCOUNT_     │
│ maxFailedAttempts      │                          │ LOCKOUT      │
├────────────────────────┼──────────────────────────┼──────────────┤
│ accountLockout.        │ 30 (minutes)             │ ACCOUNT_     │
│ lockoutDurationMinutes │                          │ LOCKOUT      │
├────────────────────────┼──────────────────────────┼──────────────┤
│ accountLockout.        │ 24 (hours, 0=never)      │ ACCOUNT_     │
│ counterResetHours      │                          │ LOCKOUT      │
├────────────────────────┼──────────────────────────┼──────────────┤
│ accountLockout.        │ true (ENABLE/DISABLE)    │ ACCOUNT_     │
│ enabled                │                          │ LOCKOUT      │
├────────────────────────┼──────────────────────────┼──────────────┤
│ loginAttempts.         │ 5 (tokens)               │ RATE_LIMIT   │
│ maxCapacity            │                          │              │
├────────────────────────┼──────────────────────────┼──────────────┤
│ loginAttempts.         │ 5 (tokens)               │ RATE_LIMIT   │
│ refillRate             │                          │              │
├────────────────────────┼──────────────────────────┼──────────────┤
│ loginAttempts.         │ 1 (minute)               │ RATE_LIMIT   │
│ refillDurationMinutes  │                          │              │
├────────────────────────┼──────────────────────────┼──────────────┤
│ loginAttempts.         │ true (ENABLE/DISABLE)    │ RATE_LIMIT   │
│ enabled                │                          │              │
└────────────────────────┴──────────────────────────┴──────────────┘
```

### Enable/Disable Policy Controls

**Account Lockout Policy (`accountLockout.enabled`)**
- **Enabled (true)**: Failed attempt counter is tracked; accounts lock after maxFailedAttempts
- **Disabled (false)**: Failed attempts are NOT tracked; locking is bypassed
- Can be toggled at runtime without restart
- Takes effect on next login attempt

**Login Rate Limiting Policy (`loginAttempts.enabled`)**
- **Enabled (true)**: Token bucket rate limiting is enforced; excess attempts are rejected
- **Disabled (false)**: All login attempts are allowed (no rate limiting)
- Can be toggled at runtime without restart
- Takes effect on next login attempt
- Note: Disabling does NOT clear existing token buckets

---

## Policy Enable/Disable Implementation

### Account Lockout Policy Control

The Account Lockout policy is enforced by checking `accountLockout.enabled` setting at three critical points:

**1. During Login - Account Lock Check (Step 5)**
```java
// LoginService.java:80-88
if (securitySettingsGetterServices.getAccountLockoutEnabled() && !user.isAccountNonLocked()) {
    if (unlockWhenTimeExpired(user)) {
        resetFailedAttempts(user);
    } else {
        throw new LoginException("Your account is locked...");
    }
}
```
- If disabled: locked accounts are NOT checked; login proceeds
- If enabled: locked accounts prevent login

**2. During Failed Authentication (Step 8b)**
```java
// LoginService.java:127-137
if (securitySettingsGetterServices.getAccountLockoutEnabled()) {
    int maxFailedAttempts = securitySettingsGetterServices.getMaxFailedAttempts();
    if (user.getFailedAttempt() >= maxFailedAttempts - 1) {
        lock(user);
    } else {
        increaseFailedAttempts(user);
    }
}
```
- If disabled: failed attempts are NOT tracked; no locking occurs
- If enabled: counter increments; account locks when threshold reached

**3. Scheduled Maintenance Tasks**
```java
// AccountMaintenanceScheduledService.java:46-50
if (!securitySettingsGetterServices.getAccountLockoutEnabled()) {
    log.debug("Account lockout policy is disabled, skipping unlockExpiredAccounts");
    return;
}
```
- If disabled: background tasks skip processing locked accounts
- If enabled: tasks automatically unlock expired locks and reset counters

---

### Login Rate Limiting Policy Control

The Login Rate Limiting policy is enforced by checking `loginAttempts.enabled` setting at the beginning of login:

**During Login - Rate Limit Check (Step 0)**
```java
// LoginService.java:50-61
if (securitySettingsGetterServices.getLoginRateLimitEnabled()) {
    if (!loginRateLimitingService.isAllowed(identifier)) {
        throw new LoginException("Too many login attempts. Please try again later.");
    }
}
```
- If disabled: all login attempts are allowed (no token consumption)
- If enabled: token bucket algorithm enforces rate limiting

**Important Notes:**
- Token bucket configuration changes (maxCapacity, refillRate, refillDurationMinutes) take effect on NEW bucket creation
- Existing token buckets are NOT cleared when settings change
- Disabling rate limiting does NOT refund consumed tokens
- Next login attempt after disabling rate limiting will either succeed or create a new bucket

---

## Logging and Monitoring

### Key Log Points

1. **Rate Limiting**
   ```
   WARN: Login rate limit exceeded for username: {username}
   DEBUG: Created rate limit bucket for username: {username} with capacity: {capacity}
   ```

2. **Account Lockout**
   ```
   INFO: Account automatically unlocked for user: {username}
   ERROR: Error checking lockout duration for user: {username}
   ```

3. **Counter Reset**
   ```
   INFO: Failed attempt counter reset for user: {username}
   DEBUG: Counter reset disabled (counterResetHours = 0)
   ERROR: Error checking counterResetHours for user: {username}
   ```

4. **Authentication**
   ```
   INFO: Security setting initialized: {key} = {value} (category: {category})
   WARN: Failed to initialize security setting: {key}
   ```

### Monitoring Recommendations

- **Alert on**: Multiple account lockouts in short time
- **Track**: Failed login attempt patterns by IP/user
- **Monitor**: Rate limiter bucket refill rates
- **Audit**: Account unlock events (manual vs automatic)

---

## Exception Handling

All authentication failures result in `LoginException` with user-friendly messages:

```java
throw new LoginException(message);
```

**Available Exceptions**:
- Rate limit exceeded
- Invalid credentials
- Account disabled
- Account locked
- Password expired
- Credentials expired

---

## Related Classes

- [LoginService.java](LoginService.java) - Main authentication orchestrator
- [LoginRateLimitingService.java](LoginRateLimitingService.java) - Token bucket implementation
- [AccountMaintenanceScheduledService.java](../../Services/AccountMaintenanceScheduledService.java) - Background maintenance
- [User.java](../../User.java) - Entity with security attributes
- [UserRepository.java](../../UserRepository.java) - Database queries
- [SecuritySettingsService.java](../../../../Security/SecuritySettingsService.java) - Settings provider
- [KabengosafarisApplication.java](../../../../KabengosafarisApplication.java) - Application startup with @EnableScheduling

---

## Testing Scenarios

### Scenario 1: Successful Login
```
1. User provides valid email and password
2. All checks pass (enabled, unlocked, password valid)
3. Spring Security authenticates credentials
4. JWT tokens generated and returned
5. Result: ✓ Login successful
```

### Scenario 2: Account Lockout and Auto-Unlock
```
1. User fails 5 attempts (maxFailedAttempts)
2. Account is locked for 30 minutes (lockoutDurationMinutes)
3. 30+ minutes pass
4. Scheduled task auto-unlocks account
5. User can login again with fresh 5 attempts
6. Result: ✓ Auto-unlock works correctly
```

### Scenario 3: Counter Reset After Inactivity
```
1. User fails 3 attempts @ 10:00 AM
2. User doesn't attempt login for 24+ hours
3. Scheduled task resets counter @ 10:05 AM next day
4. User has 5 fresh attempts on next login
5. Result: ✓ Counter reset works correctly
```

### Scenario 4: Rate Limiting
```
1. User makes 5 login attempts rapidly
2. Token bucket depleted (0 tokens)
3. 6th attempt immediately rejected
4. 1 minute passes, refillRate tokens added back
5. User can attempt login again
6. Result: ✓ Rate limiting works correctly
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-11-20 | Initial documentation |
| | | - Complete login workflow flow |
| | | - Rate limiting explanation |
| | | - Account lockout mechanisms |
| | | - Scheduled maintenance tasks |
| | | - Configuration reference |
| | | - Testing scenarios |

---

## References

- [Spring Security Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/index.html)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- [OWASP: Brute Force Attack](https://owasp.org/www-community/attacks/Brute_force_attack)
- [OWASP: Account Lockout](https://owasp.org/www-community/controls/Account_Lockout)

