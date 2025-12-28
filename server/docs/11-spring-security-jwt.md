# Spring Security with JWT Authentication

A comprehensive guide to understanding and implementing JWT-based authentication in Spring Boot 3 applications.

---

## Table of Contents

### Part 1: Foundations
1. [What is Authentication?](#what-is-authentication)
2. [Authentication vs Authorization](#authentication-vs-authorization)
3. [The Authentication Pattern](#the-authentication-pattern)
4. [Session-Based vs Token-Based Auth](#session-based-vs-token-based-auth)
5. [Why JWT for REST APIs?](#why-jwt-for-rest-apis)

### Part 2: Understanding JWT
6. [What is a JWT?](#what-is-a-jwt)
7. [JWT Structure Explained](#jwt-structure-explained)
8. [How JWT Authentication Works](#how-jwt-authentication-works)

### Part 3: Spring Security Architecture
9. [How Spring Security Works](#how-spring-security-works)
10. [The Security Filter Chain](#the-security-filter-chain)
11. [Key Spring Security Interfaces](#key-spring-security-interfaces)

### Part 4: Implementation
12. [Components We'll Build](#components-well-build)
13. [Checkpoint 1: Setup & Configuration](#checkpoint-1-setup--configuration)
14. [Checkpoint 2: JWT Service](#checkpoint-2-jwt-service)
15. [Checkpoint 3: User Details Integration](#checkpoint-3-user-details-integration)
16. [Checkpoint 4: JWT Filter & Security Config](#checkpoint-4-jwt-filter--security-config)
17. [Checkpoint 5: Auth Endpoints](#checkpoint-5-auth-endpoints)
18. [Checkpoint 6: Integration & Testing](#checkpoint-6-integration--testing)

### Part 5: Reference
19. [Quick Reference](#quick-reference)
20. [Troubleshooting](#troubleshooting)
21. [Security Best Practices](#security-best-practices)

---

# Part 1: Foundations

## What is Authentication?

**Authentication** answers the question: **"Who are you?"**

It's the process of verifying a user's identity. When you log into any application, you're authenticating yourself - proving that you are who you claim to be.

### Common Authentication Methods

| Method | How It Works | Example |
|--------|--------------|---------|
| **Username/Password** | User provides credentials that match stored records | Most websites |
| **OAuth 2.0 / SSO** | Delegate authentication to a trusted provider | "Login with Google" |
| **API Keys** | Static token identifies the calling application | Stripe, AWS |
| **Certificates** | Cryptographic certificates prove identity | mTLS, smart cards |
| **Biometrics** | Physical characteristics (fingerprint, face) | Phone unlock |

In this guide, we implement **username/password authentication** with **JWT tokens**.

---

## Authentication vs Authorization

These terms are often confused but are fundamentally different:

| Aspect | Authentication (AuthN) | Authorization (AuthZ) |
|--------|------------------------|----------------------|
| **Question** | "Who are you?" | "What can you do?" |
| **When** | First - must happen before authorization | Second - after identity is confirmed |
| **Mechanism** | Credentials (password, token, biometrics) | Permissions, roles, policies |
| **Failure** | 401 Unauthorized | 403 Forbidden |
| **Example** | Logging into your bank account | Accessing only YOUR accounts, not others' |

### A Real-World Analogy

Imagine entering a secure office building:

1. **Authentication**: The security guard checks your ID badge to verify you are John Smith (employee #1234).

2. **Authorization**: Your badge grants access to floors 1-3, but not the executive floor (4). The system knows WHO you are, but restricts WHAT you can access.

### In Our Application

```
POST /api/auth/login  →  Authentication (verify credentials, get token)
GET /api/tasks        →  Authorization (can this user access tasks?)
GET /api/tasks/5      →  Authorization (can this user access THIS task?)
```

> **Note:** This guide focuses on **authentication**. Role-based authorization (RBAC) is a future topic.

---

## The Authentication Pattern

Regardless of technology (JWT, sessions, OAuth, API keys), authentication follows one universal pattern:

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION PATTERN                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. PROVE WHO YOU ARE (once)                                    │
│     ┌──────────┐                      ┌──────────┐              │
│     │  Client  │ ── credentials ────> │  Server  │              │
│     │          │ <── proof ────────── │          │              │
│     └──────────┘                      └──────────┘              │
│                                                                  │
│  2. SHOW PROOF (every request)                                  │
│     ┌──────────┐                      ┌──────────┐              │
│     │  Client  │ ── proof + request ─>│  Server  │              │
│     │          │ <── response ─────── │          │              │
│     └──────────┘                      └──────────┘              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Two steps:**
1. **Prove once** → Get proof of identity
2. **Show proof** → Access granted

### How Different Methods Implement This

| Method | Credentials | Proof | Where Proof Lives |
|--------|-------------|-------|-------------------|
| **Session** | username/password | Session ID | Cookie (server stores user data) |
| **JWT** | username/password | Token | Header (token contains user data) |
| **OAuth** | (delegated to Google, etc.) | Access token | Header |
| **API Key** | (none - key IS the proof) | API key | Header or query param |

### The Server-Side Pattern

Every auth system does the same things:

```
REGISTRATION:
  1. Receive credentials
  2. Validate (unique username, etc.)
  3. Store securely (hash password)
  4. Issue proof

LOGIN:
  1. Receive credentials
  2. Verify against stored
  3. Issue proof

EVERY PROTECTED REQUEST:
  1. Extract proof from request
  2. Validate proof (not expired, not tampered)
  3. Identify user from proof
  4. Allow or deny access
```

### Mapped to Spring Security

| Pattern Step | Spring Security Component |
|--------------|---------------------------|
| Store credentials | `PasswordEncoder` + Database |
| Verify credentials | `AuthenticationManager` |
| Issue proof | `JwtService.generateToken()` |
| Extract proof | `JwtAuthenticationFilter` |
| Validate proof | `JwtService.isTokenValid()` |
| Identify user | `UserDetailsService` |
| Allow/deny | `SecurityFilterChain` rules |

This pattern is the foundation. Everything else is implementation details.

---

## Session-Based vs Token-Based Auth

There are two main approaches to maintaining authentication state:

### Session-Based Authentication (Traditional)

```
┌──────────┐                           ┌──────────┐
│  Client  │                           │  Server  │
└────┬─────┘                           └────┬─────┘
     │                                      │
     │  1. POST /login (credentials)        │
     │─────────────────────────────────────>│
     │                                      │ Creates session
     │                                      │ Stores in memory/DB
     │  2. Set-Cookie: JSESSIONID=abc123    │
     │<─────────────────────────────────────│
     │                                      │
     │  3. GET /api/tasks                   │
     │  Cookie: JSESSIONID=abc123           │
     │─────────────────────────────────────>│
     │                                      │ Looks up session
     │                                      │ Validates user
     │  4. 200 OK (tasks)                   │
     │<─────────────────────────────────────│
```

**How it works:**
1. User logs in with credentials
2. Server creates a session, stores user info server-side, sends session ID as cookie
3. Browser automatically sends cookie with every request
4. Server looks up session to identify user

**Pros:**
- Simple to implement
- Can invalidate sessions instantly (logout)
- Session data stays on server (secure)

**Cons:**
- **Stateful** - server must store session data
- **Scaling issues** - sessions don't share across servers without extra infrastructure (sticky sessions, Redis, etc.)
- **CSRF vulnerability** - cookies sent automatically by browser

### Token-Based Authentication (JWT)

```
┌──────────┐                           ┌──────────┐
│  Client  │                           │  Server  │
└────┬─────┘                           └────┬─────┘
     │                                      │
     │  1. POST /login (credentials)        │
     │─────────────────────────────────────>│
     │                                      │ Validates credentials
     │                                      │ Creates JWT token
     │  2. 200 OK { "token": "eyJ..." }     │
     │<─────────────────────────────────────│
     │                                      │
     │  Stores token                        │
     │  (localStorage/memory)               │
     │                                      │
     │  3. GET /api/tasks                   │
     │  Authorization: Bearer eyJ...        │
     │─────────────────────────────────────>│
     │                                      │ Validates token signature
     │                                      │ Extracts user from token
     │  4. 200 OK (tasks)                   │
     │<─────────────────────────────────────│
```

**How it works:**
1. User logs in with credentials
2. Server validates, creates signed JWT containing user info, returns it
3. Client stores token and sends it in `Authorization` header
4. Server validates token signature and extracts user info

**Pros:**
- **Stateless** - no server-side session storage
- **Scalable** - any server can validate the token
- **Cross-domain** - works across different domains/services
- **Mobile-friendly** - no cookie handling needed

**Cons:**
- Cannot invalidate tokens before expiration (without extra infrastructure)
- Token size larger than session ID
- Must handle token storage securely on client

---

## Why JWT for REST APIs?

For REST APIs, **token-based authentication is the standard** because:

### 1. REST Should Be Stateless

REST principles require that each request contains all information needed to process it. Session-based auth violates this by requiring server-side state.

```
Stateful (Session):   Request + Server Session = User Identity
Stateless (JWT):      Request (with token) = User Identity
```

### 2. Scalability

With sessions, you need shared session storage (Redis, database) or sticky sessions (route users to same server). With JWT:

```
┌─────────┐     ┌─────────────┐     ┌─────────┐
│ Client  │────>│Load Balancer│────>│Server A │  ✓ Any server works!
└─────────┘     └─────────────┘     ├─────────┤
   Token in                         │Server B │  ✓ No session sync needed!
   every request                    ├─────────┤
                                    │Server C │  ✓ Horizontally scalable!
                                    └─────────┘
```

### 3. Microservices & APIs

JWT tokens can be validated by any service that has the secret key:

```
┌────────┐   Token    ┌──────────────┐   Token    ┌──────────────┐
│ Client │──────────>│ API Gateway  │──────────>│ User Service │
└────────┘           └──────────────┘           └──────────────┘
                           │ Token
                           v
                     ┌──────────────┐
                     │ Task Service │  Each service validates independently
                     └──────────────┘
```

### 4. No CSRF Vulnerability

CSRF attacks exploit automatic cookie sending. Since JWT is sent in `Authorization` header (not cookies), CSRF attacks don't work.

---

# Part 2: Understanding JWT

## What is a JWT?

**JWT (JSON Web Token)** is an open standard (RFC 7519) for securely transmitting information between parties as a JSON object. The information is **digitally signed**, so it can be verified and trusted.

### Key Properties

| Property | Description |
|----------|-------------|
| **Self-contained** | Contains all user info needed (no database lookup required) |
| **Signed** | Cryptographically signed to prevent tampering |
| **Compact** | Small size, suitable for URLs and headers |
| **Stateless** | Server doesn't need to store session data |

### What a JWT Looks Like

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwiaWF0IjoxNzAzNjgwMDAwLCJleHAiOjE3MDM3NjY0MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

Three parts separated by dots (`.`):
1. **Header** - Algorithm and token type
2. **Payload** - Claims (user data)
3. **Signature** - Verification hash

---

## JWT Structure Explained

Let's decode each part:

### 1. Header

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Base64Url encoded: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9`

| Field | Description |
|-------|-------------|
| `alg` | Signing algorithm (HS256 = HMAC-SHA256) |
| `typ` | Token type (always "JWT") |

### 2. Payload (Claims)

```json
{
  "sub": "johndoe",
  "iss": "task-manager-api",
  "iat": 1703680000,
  "exp": 1703766400
}
```

Base64Url encoded: `eyJzdWIiOiJqb2huZG9lIiwiaXNzIjoidGFzay1tYW5hZ2VyLWFwaSIsImlhdCI6MTcwMzY4MDAwMCwiZXhwIjoxNzAzNzY2NDAwfQ`

**Standard Claims (Registered):**

| Claim | Name | Description |
|-------|------|-------------|
| `sub` | Subject | Who the token is about (usually username or user ID) |
| `iss` | Issuer | Who created the token (your application) |
| `iat` | Issued At | When token was created (Unix timestamp) |
| `exp` | Expiration | When token expires (Unix timestamp) |
| `aud` | Audience | Intended recipient (optional) |
| `nbf` | Not Before | Token not valid before this time (optional) |

**Custom Claims (add your own):**
```json
{
  "sub": "johndoe",
  "roles": ["USER", "ADMIN"],
  "email": "john@example.com"
}
```

> **Warning:** Payload is **encoded, NOT encrypted**. Anyone can decode it! Never put sensitive data (passwords, secrets) in the payload.

### 3. Signature

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

The signature ensures:
- **Integrity** - Token hasn't been modified
- **Authenticity** - Token was created by someone with the secret key

**How verification works:**
1. Server receives token
2. Takes header + payload, signs with secret key
3. Compares calculated signature with token's signature
4. If match → token is valid and unmodified

```
Token received:    [Header].[Payload].[Signature]
                        ↓        ↓
Server calculates: HMAC(Header.Payload, SECRET) = Expected Signature
                                                        ↓
                                    Compare: Signature == Expected?
                                              ↓
                                    Match = Valid | No Match = Tampered
```

---

## How JWT Authentication Works

Here's the complete flow:

### Registration Flow

```
┌──────────┐                                    ┌──────────┐
│  Client  │                                    │  Server  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ 1. POST /api/auth/register                    │
     │    { username, email, password }              │
     │──────────────────────────────────────────────>│
     │                                               │
     │                                    2. Validate input
     │                                    3. Check username/email unique
     │                                    4. Hash password (BCrypt)
     │                                    5. Save user to database
     │                                    6. Generate JWT token
     │                                               │
     │ 7. 201 Created                                │
     │    { token, user: { id, username, email } }   │
     │<──────────────────────────────────────────────│
     │                                               │
     │ 8. Store token (localStorage/memory)          │
     │                                               │
```

### Login Flow

```
┌──────────┐                                    ┌──────────┐
│  Client  │                                    │  Server  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ 1. POST /api/auth/login                       │
     │    { username, password }                     │
     │──────────────────────────────────────────────>│
     │                                               │
     │                                    2. Find user by username
     │                                    3. Verify password (BCrypt)
     │                                    4. Generate JWT token
     │                                               │
     │ 5. 200 OK                                     │
     │    { token, user: { id, username, email } }   │
     │<──────────────────────────────────────────────│
     │                                               │
     │ 6. Store token                                │
     │                                               │
```

### Authenticated Request Flow

```
┌──────────┐                                    ┌──────────┐
│  Client  │                                    │  Server  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ 1. GET /api/tasks                             │
     │    Authorization: Bearer eyJhbG...            │
     │──────────────────────────────────────────────>│
     │                                               │
     │                            ┌─────────────────────────────────┐
     │                            │ JWT Filter (runs before controller)
     │                            │                                 │
     │                            │ 2. Extract token from header    │
     │                            │ 3. Validate signature           │
     │                            │ 4. Check not expired            │
     │                            │ 5. Extract username from token  │
     │                            │ 6. Load user from database      │
     │                            │ 7. Set SecurityContext          │
     │                            └─────────────────────────────────┘
     │                                               │
     │                                    8. Controller handles request
     │                                    9. Return data
     │                                               │
     │ 10. 200 OK { tasks: [...] }                   │
     │<──────────────────────────────────────────────│
```

---

# Part 3: Spring Security Architecture

## How Spring Security Works

Spring Security is a powerful framework that handles authentication and authorization. Understanding its architecture helps you implement custom auth correctly.

### The Big Picture

When a request comes in, it passes through a chain of **filters** before reaching your controller:

```
HTTP Request
     │
     ▼
┌────────────────────────────────────────────────────────┐
│                  Filter Chain                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │
│  │ Security │─>│   CORS   │─>│   JWT    │─>│  Auth  │ │
│  │ Context  │  │  Filter  │  │  Filter  │  │Provider│ │
│  └──────────┘  └──────────┘  └──────────┘  └────────┘ │
└────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────┐
│ Controller  │  (Your @RestController)
└─────────────┘
```

Each filter has a specific job:
- **SecurityContextPersistenceFilter** - Manages the SecurityContext
- **CorsFilter** - Handles CORS headers
- **JwtAuthenticationFilter** - Our custom filter to validate JWT tokens
- **AuthorizationFilter** - Checks if user has permission to access the resource

### The SecurityContext

The **SecurityContext** holds the currently authenticated user's information. It's stored in a `ThreadLocal`, meaning each request thread has its own context.

```java
// Getting the current authenticated user anywhere in your code
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

The JWT filter's job is to:
1. Validate the token
2. Create an `Authentication` object
3. Store it in the `SecurityContext`

---

## The Security Filter Chain

Spring Security uses a **filter chain pattern**. Filters execute in a specific order, and each can:
- Pass the request to the next filter
- Block the request (return error response)
- Modify the request/response

### Default Filter Order (Simplified)

```
1.  SecurityContextPersistenceFilter  - Load/save SecurityContext
2.  CorsFilter                        - Handle CORS
3.  CsrfFilter                        - CSRF protection (we disable this)
4.  LogoutFilter                      - Handle logout
5.  UsernamePasswordAuthFilter        - Form login (we don't use this)
6.  [Our JwtAuthenticationFilter]     - Custom JWT validation
7.  ExceptionTranslationFilter        - Convert security exceptions to HTTP responses
8.  AuthorizationFilter               - Check permissions
```

### Where Our JWT Filter Fits

We insert our `JwtAuthenticationFilter` before the `UsernamePasswordAuthenticationFilter`:

```java
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

This ensures our JWT validation runs early, before Spring Security tries other auth methods.

---

## Key Spring Security Interfaces

Understanding these interfaces is crucial for implementing custom authentication:

### 1. UserDetails

Represents your user in Spring Security's world.

```java
public interface UserDetails {
    String getUsername();
    String getPassword();
    Collection<? extends GrantedAuthority> getAuthorities();  // roles/permissions
    boolean isAccountNonExpired();
    boolean isAccountNonLocked();
    boolean isCredentialsNonExpired();
    boolean isEnabled();
}
```

**Why needed:** Spring Security doesn't know about your `AppUser` entity. You need an adapter that implements `UserDetails`.

### 2. UserDetailsService

Loads user data from your database.

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

**Why needed:** When validating a token, Spring Security needs to load the user. Your implementation fetches from `AppUserRepository`.

### 3. Authentication

Represents an authenticated user (or authentication attempt).

```java
public interface Authentication {
    Object getPrincipal();      // The user (usually UserDetails)
    Object getCredentials();    // Password (usually null after auth)
    Collection<GrantedAuthority> getAuthorities();  // Roles
    boolean isAuthenticated();
}
```

**Why needed:** This is what gets stored in `SecurityContext` after successful authentication.

### 4. AuthenticationEntryPoint

Handles what happens when an unauthenticated user tries to access a protected resource.

```java
public interface AuthenticationEntryPoint {
    void commence(HttpServletRequest request,
                  HttpServletResponse response,
                  AuthenticationException authException);
}
```

**Why needed:** By default, Spring redirects to a login page. For REST APIs, we want to return a JSON error response.

### How They Connect

```
┌───────────────────────────────────────────────────────────────────┐
│                        JWT Authentication Flow                     │
├───────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Request with JWT Token                                            │
│         │                                                          │
│         ▼                                                          │
│  ┌──────────────────────┐                                         │
│  │ JwtAuthenticationFilter│                                        │
│  │                      │                                         │
│  │  1. Extract token    │                                         │
│  │  2. Extract username ├──────┐                                  │
│  └──────────────────────┘      │                                  │
│                                ▼                                  │
│                    ┌────────────────────────┐                     │
│                    │ UserDetailsService     │                     │
│                    │                        │                     │
│                    │ loadUserByUsername()   │                     │
│                    └───────────┬────────────┘                     │
│                                │                                  │
│                                ▼                                  │
│                    ┌────────────────────────┐                     │
│                    │ UserDetails            │                     │
│                    │ (AppUserDetails)       │                     │
│                    └───────────┬────────────┘                     │
│                                │                                  │
│         ┌──────────────────────┘                                  │
│         ▼                                                          │
│  ┌──────────────────────┐                                         │
│  │ JwtAuthenticationFilter│                                        │
│  │                      │                                         │
│  │  3. Validate token   │                                         │
│  │  4. Create Authentication                                       │
│  │  5. Set SecurityContext                                         │
│  └──────────────────────┘                                         │
│         │                                                          │
│         ▼                                                          │
│  ┌──────────────────────┐                                         │
│  │ SecurityContext      │                                         │
│  │                      │                                         │
│  │ Authentication {     │                                         │
│  │   principal: UserDetails                                        │
│  │   authenticated: true│                                         │
│  │ }                    │                                         │
│  └──────────────────────┘                                         │
│         │                                                          │
│         ▼                                                          │
│  Request continues to Controller                                   │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘
```

---

# Part 4: Implementation

## Components We'll Build

Here's what we're building and how each component fits:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Authentication Components                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  CONFIG                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐                             │
│  │ JwtProperties    │  │ SecurityConfig   │                             │
│  │                  │  │                  │                             │
│  │ • secret         │  │ • Filter chain   │                             │
│  │ • expirationMs   │  │ • CSRF disabled  │                             │
│  │ • issuer         │  │ • Stateless      │                             │
│  └──────────────────┘  │ • Public paths   │                             │
│                        │ • Password encoder                              │
│                        └──────────────────┘                             │
│                                                                          │
│  SECURITY (Core Components)                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │ JwtService       │  │ AppUserDetails   │  │ AppUserDetails   │      │
│  │                  │  │                  │  │ Service          │      │
│  │ • generateToken  │  │ Wraps AppUser    │  │                  │      │
│  │ • validateToken  │  │ as UserDetails   │  │ Loads user from  │      │
│  │ • extractUsername│  │                  │  │ database         │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐                             │
│  │ JwtAuthentication│  │ JwtAuthentication│                             │
│  │ Filter           │  │ EntryPoint       │                             │
│  │                  │  │                  │                             │
│  │ Validates token  │  │ Returns 401 JSON │                             │
│  │ on each request  │  │ for unauth'd     │                             │
│  └──────────────────┘  └──────────────────┘                             │
│                                                                          │
│  AUTH (API Layer)                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │ AuthController   │  │ AuthService      │  │ Auth DTOs        │      │
│  │                  │  │                  │  │                  │      │
│  │ • /auth/register │  │ • register()     │  │ • LoginRequest   │      │
│  │ • /auth/login    │  │ • login()        │  │ • RegisterRequest│      │
│  │ • /auth/me       │  │                  │  │ • AuthResponse   │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Component Summary

| Component | Location | Purpose |
|-----------|----------|---------|
| `JwtProperties` | `config/` | Type-safe JWT configuration binding |
| `SecurityConfig` | `config/` | Security filter chain configuration |
| `JwtService` | `security/` | Generate and validate JWT tokens |
| `AppUserDetails` | `security/` | Adapts AppUser to Spring Security's UserDetails |
| `AppUserDetailsService` | `security/` | Loads users from database for Spring Security |
| `JwtAuthenticationFilter` | `security/` | Intercepts requests, validates JWT |
| `JwtAuthenticationEntryPoint` | `security/` | Returns 401 JSON for unauthenticated requests |
| `AuthController` | `controller/` | REST endpoints for login/register |
| `AuthService` | `service/` | Business logic for authentication |
| `LoginRequestDto` | `dto/auth/` | Login request body |
| `RegisterRequestDto` | `dto/auth/` | Registration request body |
| `AuthResponseDto` | `dto/auth/` | Authentication response with token |

---

## Checkpoint 1: Setup & Configuration

This checkpoint adds dependencies and creates the foundational configuration.

### Step 1.1: Add Spring Security (pom.xml)

```xml
<!-- Spring Security for authentication and authorization -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**What happens when you add this:**
- ALL endpoints become secured by default (return 401)
- Spring generates a random password logged at startup
- You MUST create a `SecurityConfig` to customize behavior

### Step 1.2: Add JWT Library (pom.xml)

We use **jjwt** (Java JWT) by jsonwebtoken.io:

```xml
<!-- JWT (JSON Web Token) for stateless authentication -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
```

**Why 3 dependencies?**

| Dependency | Scope | Purpose |
|------------|-------|---------|
| `jjwt-api` | compile | Interfaces your code depends on |
| `jjwt-impl` | runtime | Implementation (hidden from your code) |
| `jjwt-jackson` | runtime | JSON serialization with Jackson |

This separation follows good design - your code depends only on interfaces, not implementation details.

### Step 1.3: Add JWT Configuration (application.yml)

```yaml
app:
  cors:
    # ... existing cors config ...
  jwt:
    secret: ${JWT_SECRET:dGFza21hbmFnZXItand0LXNlY3JldC1rZXktZm9yLWRldmVsb3BtZW50LW9ubHk=}
    expiration-ms: 86400000  # 24 hours in milliseconds
    issuer: task-manager-api
```

**Configuration explained:**

| Property | Description | Value |
|----------|-------------|-------|
| `secret` | Base64-encoded signing key (min 32 bytes for HS256) | Environment variable with fallback |
| `expiration-ms` | Token lifetime in milliseconds | 86400000 = 24 hours |
| `issuer` | Identifies your application | Used in `iss` claim |

**Environment variable syntax:** `${JWT_SECRET:default-value}`
- Uses `JWT_SECRET` environment variable if set
- Falls back to default value if not set
- **Always set in production!** Default is for development only.

### Step 1.4: Create JwtProperties.java

**Location:** `src/main/java/com/tutorial/taskmanager/config/JwtProperties.java`

```java
package com.tutorial.taskmanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for JWT authentication.
 * Binds to properties under {@code app.jwt} in application.yml.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens (Base64 encoded).
     * Must be at least 256 bits (32 bytes) for HS256 algorithm.
     */
    private String secret;

    /**
     * Token expiration time in milliseconds.
     * Default: 86400000 (24 hours)
     */
    private Long expirationMs;

    /**
     * Issuer claim for JWT tokens.
     * Identifies this application as the token issuer.
     */
    private String issuer;
}
```

**Key concepts:**

| Annotation | Purpose |
|------------|---------|
| `@ConfigurationProperties(prefix = "app.jwt")` | Binds YAML properties under `app.jwt` |
| `@Getter` / `@Setter` | Lombok generates getters/setters (required for binding) |

**Field name mapping:** `expirationMs` → `expiration-ms` (camelCase to kebab-case automatic)

**Registration options:**
1. Add `@Configuration` to this class (self-registering), OR
2. Add `@EnableConfigurationProperties(JwtProperties.class)` to SecurityConfig

### Step 1.5: Create SecurityConfig.java (Permit All - Temporary)

**Location:** `src/main/java/com/tutorial/taskmanager/config/SecurityConfig.java`

This initial version permits all requests while we build out the auth components:

```java
package com.tutorial.taskmanager.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Task Manager application.
 *
 * <p>Initial version permits all requests. JWT authentication
 * will be added in subsequent checkpoints.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * Configures the security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF - not needed for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session - no server-side session storage
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules - permit all for now
            // TODO: Add proper authorization rules in Checkpoint 4
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // Allow H2 console frames (development only)
            .headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

            .build();
    }

    /**
     * Password encoder using BCrypt.
     *
     * <p>BCrypt is the industry standard for password hashing:
     * <ul>
     *   <li>Automatically handles salting</li>
     *   <li>Configurable work factor (default 10 rounds)</li>
     *   <li>Resistant to rainbow table attacks</li>
     * </ul>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Key concepts explained:**

#### Why disable CSRF?

**CSRF (Cross-Site Request Forgery)** attacks trick a user's browser into making unintended requests using the user's session cookie.

```
Attacker's website:
<img src="https://bank.com/transfer?to=attacker&amount=1000" />

If user is logged into bank.com, browser sends cookies automatically!
```

**Why we're safe without it:**
- JWT is sent in `Authorization` header, not cookies
- Headers are NOT sent automatically by the browser
- No cookies = no CSRF vulnerability

#### Why STATELESS session?

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

| Policy | Description |
|--------|-------------|
| `ALWAYS` | Always create a session |
| `IF_REQUIRED` | Create if needed (default) |
| `NEVER` | Never create, but use if exists |
| `STATELESS` | Never create or use sessions |

For JWT auth, we want `STATELESS` because:
- Each request carries its own auth (the token)
- No server-side session storage
- Better scalability

#### Why BCrypt for passwords?

**Never store passwords in plain text!** BCrypt is the industry standard because:

| Feature | Benefit |
|---------|---------|
| **Salting** | Each password gets unique random salt (prevents rainbow tables) |
| **Slow by design** | Work factor makes brute force attacks impractical |
| **Adaptive** | Can increase work factor as hardware improves |

```java
// BCrypt stores salt with the hash
$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBrkHx6g5woFsxDLb.qDNheXdaO
│  │  └─ Salt + Hash
│  └──── Work factor (10 rounds = 2^10 = 1024 iterations)
└─────── BCrypt version
```

### Step 1.6: Fix Controller Tests

When you add Spring Security, `@WebMvcTest` tests will fail because they don't load `SecurityConfig` by default. The fix:

```java
@WebMvcTest(AppUserController.class)
@Import(SecurityConfig.class)  // Add this line
@DisplayName("AppUserController Tests")
class AppUserControllerTest {
    // ...
}
```

Add `@Import(SecurityConfig.class)` to:
- `AppUserControllerTest`
- `TaskControllerTest`
- `ProjectControllerTest`

---

## Checkpoint 2: JWT Service

This checkpoint creates the core JWT operations.

### The Pattern

JwtService only does two things - tokens **OUT** and tokens **IN**:

```
TOKEN OUT (Generate):
  UserDetails → Build Claims → Sign with Secret → Token String

TOKEN IN (Validate/Extract):
  Token String → Verify Signature → Extract Claims → Data
```

| Direction | Method | Input | Output |
|-----------|--------|-------|--------|
| **OUT** | `generateToken()` | UserDetails | Token string |
| **IN** | `extractUsername()` | Token | Username |
| **IN** | `isTokenValid()` | Token + UserDetails | true/false |

Everything else is helper methods supporting these three.

### Step 2.1: Create JwtService.java

**Location:** `src/main/java/com/tutorial/taskmanager/security/JwtService.java`

```java
package com.tutorial.taskmanager.security;

import com.tutorial.taskmanager.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Service for JWT token operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate JWT tokens with user info and expiration</li>
 *   <li>Validate tokens (signature + expiration)</li>
 *   <li>Extract claims (username, expiration, etc.)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generate a JWT token for a user.
     *
     * @param userDetails The user to generate token for
     * @return Signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from token.
     *
     * @param token JWT token
     * @return Username (subject claim)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validate token against user details.
     *
     * @param token JWT token
     * @param userDetails User to validate against
     * @return true if token is valid for user and not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extract a specific claim from token.
     *
     * @param token JWT token
     * @param claimsResolver Function to extract desired claim
     * @return The extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and validate token, returning all claims.
     * Throws exception if signature is invalid.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Get the signing key from Base64-encoded secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**Method breakdown:**

| Method | Purpose |
|--------|---------|
| `generateToken()` | Creates new JWT with subject, issuer, timestamps, signature |
| `extractUsername()` | Gets username from token (the `sub` claim) |
| `isTokenValid()` | Checks username matches AND token not expired |
| `extractClaim()` | Generic method to get any claim from token |
| `extractAllClaims()` | Parses token and validates signature |
| `getSigningKey()` | Decodes Base64 secret to SecretKey |

**JWT building process:**

```java
Jwts.builder()
    .subject("johndoe")                    // sub claim
    .issuer("task-manager-api")            // iss claim
    .issuedAt(new Date())                  // iat claim
    .expiration(new Date(...))             // exp claim
    .signWith(key)                         // Sign with HMAC-SHA256
    .compact();                            // Build to string
```

---

## Checkpoint 3: User Details Integration

This checkpoint connects your `AppUser` entity to Spring Security.

### Step 3.1: Create AppUserDetails.java

**Location:** `src/main/java/com/tutorial/taskmanager/security/AppUserDetails.java`

This class adapts your `AppUser` entity to Spring Security's `UserDetails` interface:

```java
package com.tutorial.taskmanager.security;

import com.tutorial.taskmanager.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Adapter that wraps AppUser entity as Spring Security's UserDetails.
 *
 * <p>Spring Security doesn't know about our AppUser entity.
 * This class bridges the gap by implementing UserDetails.
 */
@RequiredArgsConstructor
public class AppUserDetails implements UserDetails {

    private final AppUser appUser;

    @Override
    public String getUsername() {
        return appUser.getUsername();
    }

    @Override
    public String getPassword() {
        return appUser.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO: Return roles when we implement RBAC
        return Collections.emptyList();
    }

    // All accounts are active (we don't have account status yet)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Get the underlying AppUser entity.
     * Useful when you need entity fields beyond username/password.
     */
    public AppUser getAppUser() {
        return appUser;
    }
}
```

**Why this pattern?**

Spring Security has its own `UserDetails` interface. Rather than making `AppUser` implement it (mixing concerns), we use the **Adapter Pattern**:

```
Spring Security                Your Domain
     │                              │
     ▼                              ▼
┌──────────────┐            ┌──────────────┐
│ UserDetails  │◄───────────│ AppUserDetails│────────►│  AppUser   │
│  interface   │   adapts   │   (adapter)  │  wraps  │  (entity)  │
└──────────────┘            └──────────────┘          └──────────┘
```

Benefits:
- `AppUser` stays clean (no Spring Security dependency)
- Easy to add computed properties (e.g., authorities from roles)
- Follows Single Responsibility Principle

### Step 3.2: Create AppUserDetailsService.java

**Location:** `src/main/java/com/tutorial/taskmanager/security/AppUserDetailsService.java`

```java
package com.tutorial.taskmanager.security;

import com.tutorial.taskmanager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user data from the database for Spring Security.
 *
 * <p>This service is used by:
 * <ul>
 *   <li>JwtAuthenticationFilter - to load user when validating tokens</li>
 *   <li>AuthenticationManager - to load user when verifying credentials</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findByUsername(username)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username
                ));
    }
}
```

**How it's used:**

```
JwtAuthenticationFilter                AppUserDetailsService
        │                                      │
        │ loadUserByUsername("john")           │
        │─────────────────────────────────────>│
        │                                      │
        │                         AppUserRepository.findByUsername("john")
        │                                      │
        │                         Wrap in AppUserDetails
        │                                      │
        │       UserDetails                    │
        │<─────────────────────────────────────│
        │                                      │
        │ Validate token against UserDetails   │
```

---

## Checkpoint 4: JWT Filter & Security Config

This checkpoint adds the filter that validates JWT tokens on every request.

### Step 4.1: Create JwtAuthenticationFilter.java

**Location:** `src/main/java/com/tutorial/taskmanager/security/JwtAuthenticationFilter.java`

```java
package com.tutorial.taskmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter - runs on every request.
 *
 * <p>This filter:
 * <ol>
 *   <li>Extracts JWT from Authorization header</li>
 *   <li>Validates the token signature and expiration</li>
 *   <li>Loads user from database</li>
 *   <li>Sets authentication in SecurityContext</li>
 * </ol>
 *
 * <p>Extends OncePerRequestFilter to guarantee single execution per request.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. Check if it's a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token - continue filter chain (might be public endpoint)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        // 4. Extract username from token
        final String username = jwtService.extractUsername(jwt);

        // 5. If username exists and not already authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 7. Validate token
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 8. Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,  // credentials (password) - not needed after auth
                        userDetails.getAuthorities()
                    );

                // 9. Add request details
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 10. Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 11. Continue filter chain
        filterChain.doFilter(request, response);
    }
}
```

**Filter flow diagram:**

```
Request: GET /api/tasks
Authorization: Bearer eyJhbG...

         │
         ▼
┌────────────────────────────────────┐
│      JwtAuthenticationFilter       │
├────────────────────────────────────┤
│                                    │
│  1. Header: "Bearer eyJhbG..."? ──────► No token? Skip auth, continue
│         │                          │
│         ▼ Yes                      │
│  2. Extract "eyJhbG..."            │
│         │                          │
│         ▼                          │
│  3. Extract username from token    │
│         │                          │
│         ▼                          │
│  4. Load UserDetails from DB       │
│         │                          │
│         ▼                          │
│  5. Validate token                 │
│         │                          │
│         ▼ Valid?                   │
│  6. Set SecurityContext ───────────────► Invalid? Skip auth, continue
│         │                          │     (will get 401 from auth filter)
│         ▼                          │
│  7. Continue to next filter        │
│                                    │
└────────────────────────────────────┘
         │
         ▼
     Controller
```

### Step 4.2: Create JwtAuthenticationEntryPoint.java

**Location:** `src/main/java/com/tutorial/taskmanager/security/JwtAuthenticationEntryPoint.java`

This handles what happens when an unauthenticated user tries to access a protected resource:

```java
package com.tutorial.taskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom entry point for authentication errors.
 *
 * <p>When an unauthenticated user tries to access a protected resource,
 * this returns a JSON 401 response instead of redirecting to a login page.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", "Authentication required to access this resource");
        body.put("path", request.getServletPath());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
```

**Why needed?**

By default, Spring Security redirects to `/login` for unauthenticated requests (designed for web apps with login forms). For REST APIs, we want a JSON response:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required to access this resource",
  "path": "/api/tasks"
}
```

### Step 4.3: Update SecurityConfig.java

Now we add the JWT filter and proper authorization rules:

```java
package com.tutorial.taskmanager.config;

import com.tutorial.taskmanager.security.JwtAuthenticationEntryPoint;
import com.tutorial.taskmanager.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF - not needed for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Custom 401 handler
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthEntryPoint))

            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Allow H2 console frames
            .headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

            .build();
    }

    /**
     * Authentication provider that uses our UserDetailsService and PasswordEncoder.
     *
     * <p>Note: Spring Boot auto-configures this if you have UserDetailsService
     * and PasswordEncoder beans. This explicit bean is useful for debugging.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager bean - used by AuthService for login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Authorization rules explained:**

```java
.authorizeHttpRequests(auth -> auth
    // Anyone can access auth endpoints (login, register)
    .requestMatchers("/api/auth/**").permitAll()

    // Anyone can access Swagger docs
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

    // H2 console (development only)
    .requestMatchers("/h2-console/**").permitAll()

    // EVERYTHING else requires a valid JWT token
    .anyRequest().authenticated()
)
```

**New beans explained:**

| Bean | Purpose |
|------|---------|
| `AuthenticationProvider` | Validates credentials using UserDetailsService + PasswordEncoder |
| `AuthenticationManager` | Central authentication API - used by AuthService for login |

---

## Checkpoint 5: Auth Endpoints

This checkpoint creates the authentication API.

### Step 5.1: Create Auth DTOs

**Location:** `src/main/java/com/tutorial/taskmanager/dto/auth/`

**LoginRequestDto.java:**
```java
package com.tutorial.taskmanager.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
```

**RegisterRequestDto.java:**
```java
package com.tutorial.taskmanager.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

**AuthResponseDto.java:**
```java
package com.tutorial.taskmanager.dto.auth;

import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;  // seconds until expiration

    private AppUserResponseDto user;
}
```

### Step 5.2: Create AuthService.java

**Location:** `src/main/java/com/tutorial/taskmanager/service/AuthService.java`

**Important Design Decision:** AuthService delegates to AppUserService rather than using AppUserRepository directly. This follows the service delegation pattern:

- **Single source of truth** - User creation logic lives in one place (AppUserService)
- **No code duplication** - Validation, encoding handled consistently
- **Password encoding in AppUserService** - AuthService passes raw password, AppUserService encodes it

```java
package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.config.JwtProperties;
import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.auth.AuthResponseDto;
import com.tutorial.taskmanager.dto.auth.LoginRequestDto;
import com.tutorial.taskmanager.dto.auth.RegisterRequestDto;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserService appUserService;
    private final AppUserMapper appUserMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        AppUserService appUserService,
        AppUserMapper appUserMapper,
        JwtService jwtService,
        JwtProperties jwtProperties,
        AuthenticationManager authenticationManager
    ) {
        this.appUserService = appUserService;
        this.appUserMapper = appUserMapper;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user.
     *
     * @param request Registration details
     * @return Auth response with token and user info
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        // Validate uniqueness (via AppUserService)
        if (appUserService.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (appUserService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // Create DTO with raw password (AppUserService will encode it)
        AppUserCreateDto user = AppUserCreateDto.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(request.getPassword())  // Raw - encoding happens in AppUserService
            .build();

        // Delegate to AppUserService (returns entity for token generation)
        AppUser savedUser = appUserService.createAppUserEntity(user);

        // Generate token
        AppUserDetails userDetails = new AppUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, savedUser);
    }

    /**
     * Authenticate user and return token.
     *
     * @param request Login credentials
     * @return Auth response with token and user info
     * @throws BadCredentialsException if credentials are invalid
     */
    public AuthResponseDto login(LoginRequestDto request) {
        // Authenticate (throws BadCredentialsException if invalid)
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword()
        ));

        // Load user via AppUserService (returns entity for token generation)
        AppUser user = appUserService.findEntityByUsername(request.getUsername())
            .orElseThrow();  // Won't throw - auth already validated

        AppUserDetails userDetails = new AppUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    private AuthResponseDto buildAuthResponse(String token, AppUser user) {
        return AuthResponseDto.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresIn(jwtProperties.getExpirationMs() / 1000)  // Convert to seconds
            .user(appUserMapper.toResponseDto(user))
            .build();
    }
}
```

**Why delegate to AppUserService?**

| Approach | Pros | Cons |
|----------|------|------|
| Direct repository access | Simpler, fewer dependencies | Code duplication, encoding in multiple places |
| Delegate to AppUserService | Single source of truth, DRY | Need internal entity methods |

To support delegation, AppUserService needs methods that return entities (not just DTOs):

```java
// In AppUserService - returns entity for internal use
public AppUser createAppUserEntity(AppUserCreateDto dto) { ... }
public Optional<AppUser> findEntityByUsername(String username) { ... }
```

This follows the existing pattern: `getById()` returns DTO (public API), `getEntityById()` returns entity (internal use).

### Step 5.3: Create AuthController.java

**Location:** `src/main/java/com/tutorial/taskmanager/controller/AuthController.java`

```java
package com.tutorial.taskmanager.controller;

import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.auth.AuthResponseDto;
import com.tutorial.taskmanager.dto.auth.LoginRequestDto;
import com.tutorial.taskmanager.dto.auth.RegisterRequestDto;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/auth/register - Create new account</li>
 *   <li>POST /api/auth/login - Authenticate and get token</li>
 *   <li>GET /api/auth/me - Get current user info (requires auth)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppUserMapper appUserMapper;

    /**
     * Register a new user account.
     *
     * @param request Registration details
     * @return 201 Created with token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and return token.
     *
     * @param request Login credentials
     * @return 200 OK with token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user info.
     *
     * @param userDetails Injected by Spring Security
     * @return 200 OK with user info
     */
    @GetMapping("/me")
    public ResponseEntity<AppUserResponseDto> getCurrentUser(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        AppUserResponseDto response = appUserMapper.toResponseDto(userDetails.getAppUser());
        return ResponseEntity.ok(response);
    }
}
```

**Key concepts:**

**@AuthenticationPrincipal - The Clean Way:**
```java
@GetMapping("/me")
public ResponseEntity<AppUserResponseDto> getCurrentUser(
        @AuthenticationPrincipal AppUserDetails userDetails) {
```

Spring MVC **resolves** this parameter from `SecurityContext` - the same object we stored in the JWT filter.

**Important:** This is NOT dependency injection. `AppUserDetails` is not a bean. This is **argument resolution** - Spring MVC populates controller method parameters from various sources:

| Annotation | Source | Mechanism |
|------------|--------|-----------|
| Constructor / `@Autowired` | Bean container | Dependency Injection |
| `@RequestBody` | HTTP request body | Argument Resolution |
| `@PathVariable` | URL path | Argument Resolution |
| `@RequestParam` | Query string | Argument Resolution |
| `@AuthenticationPrincipal` | SecurityContext | Argument Resolution |

**Why is this better than the alternative?**

Without `@AuthenticationPrincipal`, you'd have to do this:
```java
@GetMapping("/me")
public ResponseEntity<AppUserResponseDto> getCurrentUser() {
    // Manual extraction - verbose and requires casting
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    AppUserDetails userDetails = (AppUserDetails) auth.getPrincipal();
    // ...
}
```

**Comparison:**

| Approach | Code | Type-Safe? |
|----------|------|------------|
| `@AuthenticationPrincipal AppUserDetails` | Clean, declarative | Yes |
| `SecurityContextHolder...getPrincipal()` | Verbose, manual cast | No (Object → cast) |

Use `@AuthenticationPrincipal` in controllers. Reserve `SecurityContextHolder` for non-controller code (services, utilities) where you can't use argument resolution.

---

### Login Flow: Step-by-Step Timeline

Let's trace exactly what happens when a user logs in with `username: "john"` and `password: "secret123"`:

**Step 1: HTTP Request hits AuthController**
```
POST /api/auth/login
Body: { "username": "john", "password": "secret123" }
```

**Step 2: AuthController receives LoginRequestDto**
```java
// LoginRequestDto now holds:
// username = "john"
// password = "secret123"
```

**Step 3: AuthController calls AuthService.login()**
```java
authService.login(loginRequest);
```

**Step 4: AuthService creates authentication token**
```java
UsernamePasswordAuthenticationToken token =
    new UsernamePasswordAuthenticationToken("john", "secret123");
// This is just a container - NOT authenticated yet
```

**Step 5: AuthService calls authenticationManager.authenticate(token)**
```java
authenticationManager.authenticate(token);
// Passes username = "john", password = "secret123"
```

**Step 6: AuthenticationManager delegates to DaoAuthenticationProvider**
```
"Here's 'john' and 'secret123' - verify them"
```

**Step 7: DaoAuthenticationProvider calls UserDetailsService**
```java
userDetailsService.loadUserByUsername("john");
// Only username used here - we're finding the user
```

**Step 8: Your AppUserDetailsService queries the database**
```java
appUserRepository.findByUsername("john");
// Returns: AppUser { username="john", password="$2a$10$xyz..." (hashed) }
// Wraps it: new AppUserDetails(appUser)
```

**Step 9: DaoAuthenticationProvider compares passwords**
```java
passwordEncoder.matches("secret123", "$2a$10$xyz...");
// Compares: raw input vs stored hash
// Returns: true if match, false if not
```

**Step 10a: If passwords MATCH**
```java
// Returns authenticated Authentication object
// Flow continues...
```

**Step 10b: If passwords DON'T match**
```java
throw new BadCredentialsException("Bad credentials");
// Flow stops, 401 returned to client
```

**Step 11: Back in AuthService - generate JWT**
```java
String token = jwtService.generateToken(userDetails);
// token = "eyJhbGciOiJIUzI1NiJ9..."
```

**Step 12: AuthService builds response**
```java
AuthResponseDto {
    token: "eyJhbGciOiJIUzI1NiJ9...",
    tokenType: "Bearer",
    expiresIn: 86400,
    user: { id: 1, username: "john", email: "john@example.com" }
}
```

**Step 13: Response sent to client**
```
HTTP 200 OK
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", ... }
```

**Password Journey Summary:**

| Step | Location | Password Value |
|------|----------|----------------|
| 1-5 | Request → AuthService | `"secret123"` (raw) |
| 7 | DB lookup | Not used (only username) |
| 8 | Loaded from DB | `"$2a$10$xyz..."` (hashed) |
| 9 | PasswordEncoder.matches() | Compares raw vs hash |
| 10+ | After validation | Password discarded, never in token |

**Key Insight:** The raw password only exists briefly during authentication. After validation, it's discarded. The JWT token contains the username but **never** the password.

---

## Checkpoint 6: Integration & Testing

### Step 6.1: Update AppUserService.java

Add password encoding when creating/updating users. **Important:** Encode on the entity, not the DTO (avoid mutating input parameters).

**Inject PasswordEncoder:**
```java
private final PasswordEncoder passwordEncoder;

public AppUserService(
    AppUserRepository appUserRepository,
    AppUserMapper appUserMapper,
    PasswordEncoder passwordEncoder  // Add this
) {
    this.appUserRepository = appUserRepository;
    this.appUserMapper = appUserMapper;
    this.passwordEncoder = passwordEncoder;
}
```

**In createAppUserEntity method:**
```java
public AppUser createAppUserEntity(AppUserCreateDto dto) {
    // ... validation ...

    String password = dto.getPassword();  // Capture raw password

    AppUser appUser = appUserMapper.toEntity(dto);
    appUser.setPassword(passwordEncoder.encode(password));  // Encode on entity
    return appUserRepository.save(appUser);
}
```

**In updateAppUser method:**
```java
public AppUserResponseDto updateAppUser(Long id, AppUserUpdateDto dto) {
    // ... validation ...

    String password = dto.getPassword();  // Capture before mapper

    appUserMapper.patchEntityFromDto(dto, existingUser);

    // Handle password separately (if provided)
    if (password != null) {
        existingUser.setPassword(passwordEncoder.encode(password));
    }

    return appUserMapper.toResponseDto(appUserRepository.save(existingUser));
}
```

### Step 6.2: Update AppUserMapper.java

Add `@Mapping(target = "password", ignore = true)` to `patchEntityFromDto` so password is handled manually (with encoding), not by the mapper:

```java
@Mapping(target = "username", ignore = true)
@Mapping(target = "password", ignore = true)  // Handle manually with encoding
@Mapping(target = "tasks", ignore = true)
@Mapping(target = "projects", ignore = true)
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void patchEntityFromDto(AppUserUpdateDto dto, @MappingTarget AppUser entity);
```

### Step 6.3: Update GlobalExceptionHandler.java

Add handlers for authentication exceptions:

```java
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
}

@ExceptionHandler(UsernameNotFoundException.class)
public ResponseEntity<ErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
}
```

### Step 6.4: Testing with cURL

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com"
  }
}

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Get current user (authenticated)
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Access protected endpoint
curl -X GET http://localhost:8080/api/tasks \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Without token (should get 401)
curl -X GET http://localhost:8080/api/tasks
```

---

# Part 5: Reference

## Quick Reference

### JWT Claims

| Claim | Name | Description |
|-------|------|-------------|
| `sub` | Subject | Who the token is about (username) |
| `iss` | Issuer | Who issued the token |
| `iat` | Issued At | When the token was created (Unix timestamp) |
| `exp` | Expiration | When the token expires (Unix timestamp) |

### HTTP Status Codes

| Status | Meaning | When |
|--------|---------|------|
| 200 | OK | Login successful |
| 201 | Created | Registration successful |
| 400 | Bad Request | Validation error (bad input) |
| 401 | Unauthorized | Missing/invalid credentials or token |
| 403 | Forbidden | Valid token but insufficient permissions |

### Endpoint Summary

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/api/auth/register` | POST | Public | Create account |
| `/api/auth/login` | POST | Public | Get token |
| `/api/auth/me` | GET | Auth | Get current user |
| `/api/users/**` | * | Auth | User management |
| `/api/tasks/**` | * | Auth | Task management |
| `/api/projects/**` | * | Auth | Project management |

---

## Troubleshooting

### "All endpoints return 401"

**Cause:** SecurityConfig not loaded or configured wrong.

**Fix:**
1. Check `@EnableWebSecurity` annotation
2. Verify `permitAll()` paths are correct
3. Check filter chain order

### "Invalid token" errors

**Cause:** Token expired, tampered, or wrong secret.

**Fix:**
1. Check `exp` claim hasn't passed
2. Verify same secret used for signing and validation
3. Ensure secret is properly Base64 encoded

### "403 Forbidden" instead of 401

**Cause:** You're getting past authentication but failing authorization.

**Fix:**
1. Check `authorizeHttpRequests` rules
2. Verify user has required roles/authorities
3. For now, if you're getting 403, authentication is working

### Tests fail with 401/403

**Cause:** `@WebMvcTest` doesn't load SecurityConfig.

**Fix:**
```java
@WebMvcTest(YourController.class)
@Import(SecurityConfig.class)  // Add this
class YourControllerTest { }
```

---

## Security Best Practices

### Secret Key

- **Minimum 256 bits (32 bytes)** for HS256
- **Use environment variable** in production
- **Never commit** real secrets to git
- **Rotate periodically** (requires token invalidation strategy)

### Token Storage (Frontend)

| Option | Security | Notes |
|--------|----------|-------|
| Memory | Best | Lost on refresh |
| HttpOnly Cookie | Good | Adds CSRF concern |
| localStorage | Vulnerable | XSS can steal token |
| sessionStorage | Better | XSS still a risk |

**Recommendation:** Store in memory + refresh token in HttpOnly cookie.

### Token Expiration

| Duration | Trade-off |
|----------|-----------|
| Short (15min) | More secure, requires refresh token |
| Medium (1-24h) | Balance of security and UX |
| Long (7+ days) | Convenient but risky |

### Password Requirements

```java
@Size(min = 8, message = "Password must be at least 8 characters")
// Consider adding:
// - Uppercase letter requirement
// - Number requirement
// - Special character requirement
// - Password strength validator
```

### Additional Security Measures

1. **Rate limiting** - Prevent brute force attacks
2. **Account lockout** - Lock after N failed attempts
3. **Password history** - Prevent password reuse
4. **Audit logging** - Log all auth events
5. **Secure headers** - HSTS, X-Content-Type-Options, etc.

---

## What's Next?

This guide covers authentication. Future topics include:

1. **Refresh Tokens** - Extend sessions without re-login
2. **Role-Based Access Control (RBAC)** - Restrict actions by role
3. **OAuth 2.0 / Social Login** - "Login with Google"
4. **Multi-Factor Authentication (MFA)** - Additional security layer
5. **API Rate Limiting** - Prevent abuse
