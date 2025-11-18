# couponpop-security-module

CouponPop MSA 프로젝트의 모든 마이크로서비스에서 공통으로 사용되는 **보안 및 인증/인가 라이브러리**입니다.
Spring Security와 JWT(JSON Web Token)를 기반으로 구현되었으며, Redis를 이용한 토큰 블랙리스트 처리, 시스템 간 통신 인증 등의 기능을 제공합니다.

이 모듈은 **GitHub Packages**를 통해 배포 및 관리됩니다.

---

## 1. 주요 기능 (Features)

* **[Spring Security 자동 구성]** 라이브러리 의존성 추가만으로 JWT 기반의 `SecurityFilterChain`이 자동으로 구성됩니다.
* **[JWT 인증/인가]**
    * `Bearer` 토큰 파싱 및 검증
    * `Authorization` 헤더가 없거나 유효하지 않은 경우 표준 에러 응답 반환 (401/403)
* **[화이트리스트 관리]** 인증이 필요 없는 경로(예: 로그인, 회원가입, 헬스 체크)를 `application.yml` 설정을 통해 유연하게 관리합니다.
* **[편의 어노테이션]** `@CurrentMember`를 통해 컨트롤러에서 인증된 사용자 정보를 객체(`AuthMember`)로 바로 주입받을 수 있습니다.
* **[토큰 블랙리스트]** 로그아웃/회원탈퇴 시 Redis를 사용하여 해당 토큰의 접근을 차단합니다.
* **[시스템 토큰]** 마이크로서비스 간 내부 통신(Feign Client) 시 사용할 전용 시스템 토큰 발급 및 검증 기능을 제공합니다.

---

## 2. 사전 준비 (Prerequisites)

이 라이브러리를 사용하기 위해서는 다음 항목들이 준비되어야 합니다.

1.  **GitHub PAT (Personal Access Token)**
    * GitHub Packages에서 라이브러리를 다운로드하기 위해 `read:packages` 권한이 있는 토큰이 필요합니다.
2.  **Redis**
    * JWT 블랙리스트 기능을 위해 Redis 서버가 실행 중이어야 합니다.

---

## 3. 설치 및 설정 (Installation & Configuration)

### 3-1. 환경 변수 설정 (`.env`)

각 마이크로서비스 프로젝트(예: `api-gateway`, `member-service`)의 루트 디렉토리에 `.env` 파일을 생성하고 아래 내용을 추가합니다.

```dotenv
# [GitHub Packages] 라이브러리 다운로드 인증용
GITHUB_ACTOR={본인의 GitHub 아이디}
GITHUB_TOKEN={발급받은 PAT (ghp_...)}


# [JWT] 토큰 서명용 비밀키 (모든 서비스 동일)
JWT_SECRET_KEY={Base64 인코딩된 32byte 이상 시크릿 키}
```

### 3-2. build.gradle 설정
프로젝트의 build.gradle 파일에 아래 내용을 추가하여 GitHub Packages 저장소를 등록하고 의존성을 주입합니다.

```Groovy
// 1. .env 파일 로드 함수 정의
def loadGithubCredentials() {
    def credentials = [:]
    def envFile = file('.env')
    def foundKeys = 0

    if (envFile.exists()) {
        envFile.eachLine { line ->
            if (foundKeys == 2) return
            if (!line.startsWith('#') && line.contains('=')) {
                def (key, value) = line.split('=', 2)*.trim()
                if (key in ['GITHUB_ACTOR', 'GITHUB_TOKEN']) {
                    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1)
                    }
                    credentials[key] = value
                    foundKeys++
                }
            }
        }
    }
    return credentials
}

repositories {
    mavenCentral()

    // 2. GitHub Packages 저장소 추가
    def githubCredentials = loadGithubCredentials()
    def githubUsername = System.getenv("GITHUB_ACTOR") ?: githubCredentials.GITHUB_ACTOR ?: "unknown"
    def githubToken = System.getenv("GITHUB_TOKEN") ?: githubCredentials.GITHUB_TOKEN ?: "unknown"

    maven {
        url = uri("[https://maven.pkg.github.com/CouponPop/couponpop-security-module](https://maven.pkg.github.com/CouponPop/couponpop-security-module)")
        credentials {
            username = githubUsername
            password = githubToken
        }
    }
    
    // (Core 모듈도 필요한 경우 함께 추가)
    maven {
        url = uri("[https://maven.pkg.github.com/CouponPop/couponpop-core-module](https://maven.pkg.github.com/CouponPop/couponpop-core-module)")
        credentials {
            username = githubUsername
            password = githubToken
        }
    }
}

dependencies {
    // 3. Security 모듈 의존성 추가 (최신 버전 확인 필요)
    implementation 'com.couponpop:couponpop-security:0.0.4-SNAPSHOT'
    // Core 모듈 (필요 시)
    implementation 'com.couponpop:couponpop-core:0.0.21-SNAPSHOT'
}
```

### 3-3. application.yml 설정

jwt 설정과 redis 설정을 추가합니다.

```YAML
# JWT 및 Security 설정
jwt:
  secret:
    key: ${JWT_SECRET_KEY}
    white-list: # 인증 없이 접근 가능한 경로 목록
      - /api/v1/auth/login
      - /api/v1/auth/signup
      - /actuator/health
      - /actuator/prometheus
      - /docs/**
      - /swagger-ui/**

# Redis 설정 (토큰 블랙리스트 저장소)
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
```

## 4. 사용 가이드 (Usage Guide)

### 4-1. 현재 로그인한 사용자 정보 가져오기 (@CurrentMember)
컨트롤러 메서드 파라미터에 @CurrentMember를 사용하여 인증된 사용자 정보(AuthMember)를 주입받을 수 있습니다.
```Java
import com.couponpop.security.annotation.CurrentMember;
import com.couponpop.security.dto.AuthMember;

@GetMapping("/me")
public ResponseEntity<MemberResponse> getMyInfo(@CurrentMember AuthMember authMember) {
    Long memberId = authMember.id();
    String username = authMember.username();
    String role = authMember.memberType(); // OWNER, CUSTOMER etc.
    
    // ... 비즈니스 로직 ...
}
```
### 4-2. 로그아웃 처리 (토큰 블랙리스트)
TokenBlacklistService를 사용하여 특정 토큰을 만료(블랙리스트 처리)시킬 수 있습니다.
```Java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProvider jwtProvider;

    public void logout(String accessToken) {
        // 토큰의 남은 유효 시간만큼 Redis에 블랙리스트로 등록
        long expiration = jwtProvider.getExpirationMillis(accessToken);
        tokenBlacklistService.blacklistToken(accessToken, expiration);
    }
}
```

### 4-3. 시스템 간 통신 토큰 (System Token)
MSA 환경에서 서비스 간 통신 시 사용할 시스템 전용 토큰을 발급받습니다. (주로 Feign Interceptor에서 사용)
```Java
@Component
@RequiredArgsConstructor
public class SystemFeignInterceptor implements RequestInterceptor {
    private final SystemTokenProvider systemTokenProvider;

    @Override
    public void apply(RequestTemplate template) {
        // 자동으로 캐싱되고 갱신되는 시스템 토큰 획득
        String systemToken = systemTokenProvider.getToken();
        template.header("Authorization", "Bearer " + systemToken);
    }
}
```

## 5. 기술 스택 (Tech Stack)
* Language: Java 17
* Framework: Spring Boot 3.x, Spring Security 6.x
* Auth: JWT (JJWT Library 0.13.0)
* Storage: Redis (Token Blacklist)
* Build Tool: Gradle
* Registry: GitHub Packages
