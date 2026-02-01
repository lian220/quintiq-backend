# 인증 및 보안 가이드

> User별 KIS 계정 및 수익률 정보 보안 체계

## 📋 개요

Quantiq Core는 **사용자별 데이터 보호**를 위해 인증/인가 시스템을 구현했습니다.
- ✅ 본인만 자신의 KIS 계정 정보 접근 가능
- ✅ 본인만 자신의 수익률 정보 조회 가능
- ✅ 관리자는 모든 사용자 정보 접근 가능
- 🔄 JWT 인증 준비 완료 (나중에 활성화)

---

## 🔐 현재 보안 상태

### 개발 단계 (현재)

```kotlin
// 모든 요청 허용 (SecurityConfig.kt)
.authorizeHttpRequests { auth ->
    auth.anyRequest().permitAll()  // ✅ 개발 모드
}
```

**동작:**
- 모든 API 요청 허용
- 인증 없이 접근 가능
- 본인 확인 로직은 경고 로그만 출력

**로그 예시:**
```
⚠️ [DEV MODE] Unauthenticated access to user user123
⚠️ [DEV MODE] User user456 accessing user123's data
```

---

### 프로덕션 단계 (향후)

```kotlin
// JWT 인증 활성화 (SecurityConfig.kt의 TODO 주석 해제)
.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/api/v1/users/**").authenticated()  // ✅ 인증 필요
        .anyRequest().authenticated()
}
```

**동작:**
- JWT 토큰 검증
- 본인 확인 엄격하게 적용
- 본인 아니면 `403 Forbidden` 응답

---

## 🛡️ 보안 아키텍처

### 1. Spring Security 구조

```
Client Request
    ↓
SecurityFilterChain
    ↓
JWT Authentication Filter (향후 추가)
    ↓
Controller (@CurrentUser)
    ↓
validateUserAccess() - 본인 확인
    ↓
Service Layer
    ↓
Response
```

### 2. 본인 확인 로직

**Controller에서 검증:**

```kotlin
private fun validateUserAccess(requestedUserId: String, currentUser: UserPrincipal?) {
    // 1. 인증 확인
    if (currentUser == null) {
        throw UnauthorizedException("Authentication required")
    }

    // 2. 본인 확인 (관리자는 예외)
    if (!SecurityUtils.isAdmin() && currentUser.userId != requestedUserId) {
        throw AccessDeniedException("You can only access your own resources")
    }
}
```

**예외 응답:**

```json
// 401 Unauthorized (인증 필요)
{
  "error": "Unauthorized",
  "message": "Authentication required. Please login first.",
  "status": 401,
  "timestamp": 1707123456789
}

// 403 Forbidden (권한 없음)
{
  "error": "Forbidden",
  "message": "You can only access your own KIS account information.",
  "status": 403,
  "timestamp": 1707123456789
}
```

---

## 🔑 JWT 인증 구현 (향후)

### 1. JWT 토큰 발급 API

**엔드포인트:** `POST /api/v1/auth/login`

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "password": "password"
  }'
```

**응답:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "dGhpcyBpcyByZWZyZXNoIHRva2Vu...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

---

### 2. 인증된 API 호출

**Authorization Header 추가:**

```bash
curl -X GET http://localhost:8080/api/v1/users/user123/balance/profit \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**성공 응답:** 200 OK + 수익률 데이터

**실패 응답:**
- `401 Unauthorized` - 토큰 없음 또는 만료
- `403 Forbidden` - 본인이 아닌 데이터 접근 시도

---

### 3. JWT 토큰 검증 흐름

```
1. Client → API 요청 (Authorization: Bearer {token})
2. JwtAuthenticationFilter → 토큰 추출
3. JwtTokenProvider → 토큰 검증 (서명, 만료 시간)
4. UserDetailsService → 사용자 정보 로드
5. SecurityContext → UserPrincipal 저장
6. Controller → @CurrentUser로 주입
7. validateUserAccess() → 본인 확인
8. Service → 비즈니스 로직 실행
```

---

## 🚀 인증 활성화 방법

### Step 1: JWT 라이브러리 추가

**build.gradle.kts:**
```kotlin
dependencies {
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
}
```

---

### Step 2: JWT 설정 추가

**application.yml:**
```yaml
app:
  security:
    jwt:
      secret-key: ${JWT_SECRET_KEY:CHANGE_THIS_TO_STRONG_SECRET_KEY}
      access-token-expiration: 3600000  # 1시간 (ms)
      refresh-token-expiration: 604800000  # 7일 (ms)
```

**.env.prod:**
```bash
JWT_SECRET_KEY=YourVeryLongAndSecureJwtSecretKeyHere12345
```

---

### Step 3: SecurityConfig 활성화

**SecurityConfig.kt:**

```kotlin
// TODO 주석 제거
.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
        .requestMatchers("/api/v1/users/**").authenticated()  // ✅ 활성화
        .anyRequest().authenticated()
}

// JWT 필터 추가
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
```

---

### Step 4: Controller 본인 확인 활성화

**UserKisAccountController.kt, UserBalanceController.kt:**

```kotlin
private fun validateUserAccess(requestedUserId: String, currentUser: UserPrincipal?) {
    // TODO 주석 제거
    if (currentUser == null) {
        throw UnauthorizedException("Authentication required")
    }

    if (!SecurityUtils.isAdmin() && currentUser.userId != requestedUserId) {
        throw AccessDeniedException("You can only access your own resources")
    }
}
```

---

## 📝 필요한 구현 파일 (향후)

### 1. JwtTokenProvider.kt
```kotlin
/**
 * JWT 토큰 생성/검증
 */
@Component
class JwtTokenProvider {
    fun generateAccessToken(userPrincipal: UserPrincipal): String
    fun generateRefreshToken(userPrincipal: UserPrincipal): String
    fun validateToken(token: String): Boolean
    fun getUserIdFromToken(token: String): String
}
```

### 2. JwtAuthenticationFilter.kt
```kotlin
/**
 * JWT 토큰 검증 필터
 */
class JwtAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request, response, filterChain) {
        // 1. Authorization 헤더에서 토큰 추출
        // 2. 토큰 검증
        // 3. SecurityContext에 인증 정보 저장
        // 4. 다음 필터로 전달
    }
}
```

### 3. AuthController.kt
```kotlin
/**
 * 인증 API
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse

    @PostMapping("/logout")
    fun logout(@CurrentUser user: UserPrincipal): ResponseEntity<Unit>
}
```

### 4. UserDetailsServiceImpl.kt
```kotlin
/**
 * Spring Security UserDetailsService 구현
 */
@Service
class UserDetailsServiceImpl : UserDetailsService {
    override fun loadUserByUsername(userId: String): UserDetails {
        // DB에서 사용자 조회
        // UserPrincipal로 변환
    }
}
```

---

## 🧪 테스트 시나리오

### 개발 모드 (현재)

```bash
# 1. 인증 없이 접근 가능
curl http://localhost:8080/api/v1/users/user123/balance/profit

# 2. 다른 사용자 데이터 접근 가능 (경고 로그만)
curl http://localhost:8080/api/v1/users/user456/balance/profit

# ✅ 모두 성공 (200 OK)
```

---

### 프로덕션 모드 (향후)

```bash
# 1. 인증 없이 접근 시도 → 401 Unauthorized
curl http://localhost:8080/api/v1/users/user123/balance/profit

# 2. 로그인 (JWT 토큰 발급)
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","password":"password"}' \
  | jq -r '.accessToken')

# 3. 본인 데이터 접근 → 200 OK
curl http://localhost:8080/api/v1/users/user123/balance/profit \
  -H "Authorization: Bearer $TOKEN"

# 4. 타인 데이터 접근 시도 → 403 Forbidden
curl http://localhost:8080/api/v1/users/user456/balance/profit \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔧 관리자 권한

### 관리자 역할 부여

**UserEntity에 roles 추가:**
```kotlin
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
@Column(name = "role")
val roles: Set<String> = setOf("USER")  // "ADMIN" 추가 시 관리자
```

**관리자 권한 확인:**
```kotlin
if (SecurityUtils.isAdmin()) {
    // 모든 사용자 데이터 접근 가능
}
```

---

## ⚙️ 설정 요약

| 구성 요소 | 현재 상태 | 향후 활성화 |
|----------|----------|------------|
| Spring Security | ✅ 설정됨 | 모든 요청 허용 → 인증 필요로 변경 |
| UserPrincipal | ✅ 구현됨 | 사용 중 |
| @CurrentUser | ✅ 구현됨 | 사용 중 |
| validateUserAccess() | ✅ 구현됨 | 경고 로그 → 예외 발생으로 변경 |
| JWT Filter | ❌ 미구현 | 구현 필요 |
| AuthController | ❌ 미구현 | 구현 필요 |
| JwtTokenProvider | ❌ 미구현 | 구현 필요 |

---

## 🛠️ 트러블슈팅

### Q1. "Access denied" 에러가 발생해요

**원인:** 본인이 아닌 데이터에 접근 시도

**해결:**
- URL의 `{userId}`가 로그인한 사용자와 일치하는지 확인
- 관리자 권한이 필요한 경우 관리자 계정으로 로그인

---

### Q2. JWT 토큰이 만료되었어요

**원인:** Access Token 만료 (기본 1시간)

**해결:**
```bash
# Refresh Token으로 새 Access Token 발급
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"..."}'
```

---

### Q3. 개발 중인데 인증이 번거로워요

**해결:** SecurityConfig.kt에서 일시적으로 인증 비활성화

```kotlin
.authorizeHttpRequests { auth ->
    auth.anyRequest().permitAll()  // 개발 시에만 사용
}
```

⚠️ **주의:** 프로덕션 배포 전 반드시 인증 활성화!

---

## 📚 관련 문서

- [User KIS 계정 가이드](/docs/USER_KIS_ACCOUNT_GUIDE.md)
- [KIS API 레퍼런스](/docs/kis/KIS_OVERSEAS_STOCK_API.md)
- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/)
- [JWT.io](https://jwt.io/)

---

**마지막 업데이트:** 2026-02-01
**작성자:** Quantiq Development Team
