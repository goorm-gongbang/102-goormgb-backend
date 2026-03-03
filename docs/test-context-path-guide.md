# 테스트 코드에서 context-path 처리 가이드

## 결론부터

**`@WebMvcTest`에서는 `context-path`가 적용되지 않는다.**
테스트 코드의 요청 경로에 context-path prefix를 붙이지 않는다.

```java
// ✅ 올바른 작성법 — 컨트롤러 매핑 경로만 사용
mockMvc.perform(get("/kakao/login-url"))
mockMvc.perform(post("/token/refresh"))
mockMvc.perform(post("/onboarding/preferences"))

// ❌ 잘못된 작성법 — context-path prefix 포함
mockMvc.perform(get("/auth/kakao/login-url"))
mockMvc.perform(post("/auth/token/refresh"))
mockMvc.perform(post("/order/onboarding/preferences"))
```

---

## 왜 그런가?

### `@WebMvcTest` vs `@SpringBootTest`의 차이

|                   | `@WebMvcTest`    | `@SpringBootTest(webEnvironment = RANDOM_PORT)` |
|-------------------|------------------|-------------------------------------------------|
| 서블릿 컨테이너          | 띄우지 않음 (MockMvc) | 실제로 띄움                                          |
| `context-path` 적용 | **무시됨**          | 적용됨                                             |
| 사용 목적             | 컨트롤러 단위 테스트      | 통합 테스트                                          |

`server.servlet.context-path`는 **서블릿 컨테이너(Tomcat)가 요청을 받을 때** 경로 앞에 붙이는 설정이다.
`@WebMvcTest`는 서블릿 컨테이너를 띄우지 않고 `MockMvc`로 직접 `DispatcherServlet`을 호출하기 때문에
`context-path` 설정이 아예 적용되지 않는다.

### 실제 운영 환경의 요청 흐름

```
클라이언트 → API Gateway → Auth-Guard (Tomcat, context-path: /auth)

요청: POST /auth/token/refresh
Tomcat이 /auth를 벗겨냄 → DispatcherServlet은 /token/refresh를 받음
→ AuthController의 @PostMapping("/token/refresh") 매칭
```

### @WebMvcTest의 요청 흐름

```
MockMvc → DispatcherServlet (Tomcat 없음, context-path 미적용)

요청: POST /token/refresh
→ DispatcherServlet이 /token/refresh를 그대로 받음
→ AuthController의 @PostMapping("/token/refresh") 매칭 ✅

요청: POST /auth/token/refresh
→ DispatcherServlet이 /auth/token/refresh를 그대로 받음
→ 매칭되는 컨트롤러 없음 → NoResourceFoundException (500) ❌
```

---

## 모듈별 테스트 작성 규칙

### 각 모듈의 context-path

| 모듈             | context-path      | 컨트롤러 예시                                                           | 테스트 요청 경로                 |
|----------------|-------------------|-------------------------------------------------------------------|---------------------------|
| Auth-Guard     | `/auth`           | `@PostMapping("/token/refresh")`                                  | `/token/refresh`          |
| Auth-Guard     | `/auth`           | `@RequestMapping("/kakao")` + `@GetMapping("/login-url")`         | `/kakao/login-url`        |
| Auth-Guard     | `/auth`           | `@RequestMapping("/dev/auth")` + `@PostMapping("/login")`         | `/dev/auth/login`         |
| Order-Core     | `/order`          | `@RequestMapping("/clubs")` + `@GetMapping("/{clubId}")`          | `/clubs/{clubId}`         |
| Order-Core     | `/order`          | `@RequestMapping("/onboarding")` + `@PostMapping("/preferences")` | `/onboarding/preferences` |
| Queue          | `/queue`          | (향후 추가)                                                           | context-path 제외한 매핑 경로    |
| Seat           | `/seat`           | (향후 추가)                                                           | context-path 제외한 매핑 경로    |
| Recommendation | `/recommendation` | (향후 추가)                                                           | context-path 제외한 매핑 경로    |

### 규칙 요약

1. **`@WebMvcTest`** (컨트롤러 단위 테스트)
    - 요청 경로 = `@RequestMapping` + `@GetMapping/@PostMapping` 값 그대로
    - context-path prefix 절대 붙이지 않음
    - `application-test.yaml`에 `server.servlet.context-path` 설정 불필요

2. **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** (통합 테스트, `TestRestTemplate` 사용)
    - 서블릿 컨테이너가 실제로 뜨므로 context-path가 적용됨
    - 이 경우에만 요청 경로에 context-path prefix를 포함해야 함
    - 현재 프로젝트에서는 이 방식을 사용하지 않음

---

## application-test.yaml에 넣을 필요가 없는 것들

`@WebMvcTest` 기반 테스트만 사용하는 경우 아래 설정은 `application-test.yaml`에 불필요하다:

- `server.servlet.context-path` — MockMvc에서 무시됨
- `server.port` — 서블릿 컨테이너를 띄우지 않으므로 의미 없음

---

## 이전 Auth-Guard 테스트 실패 원인과 수정 내용

### 원인

`AuthControllerTest`와 `KakaoAuthControllerTest`에서 요청 경로에 `/auth` prefix를 포함하여 작성했음.
`@WebMvcTest`에서는 context-path가 적용되지 않으므로 매핑 불일치로 `NoResourceFoundException` 발생.

### 수정 내용

| 파일                        | Before                         | After                     |
|---------------------------|--------------------------------|---------------------------|
| `AuthControllerTest`      | `post("/auth/token/refresh")`  | `post("/token/refresh")`  |
| `AuthControllerTest`      | `post("/auth/logout")`         | `post("/logout")`         |
| `AuthControllerTest`      | `post("/auth/withdraw")`       | `post("/withdraw")`       |
| `KakaoAuthControllerTest` | `get("/auth/kakao/login-url")` | `get("/kakao/login-url")` |
| `KakaoAuthControllerTest` | `post("/auth/kakao/login")`    | `post("/kakao/login")`    |

`DevAuthControllerTest`와 `OnboardingPreferenceControllerTest`는 원래부터 prefix 없이 작성되어 있어 수정 불필요.
