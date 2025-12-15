package org.example.unibooker.domain.auth.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.auth.oauth.client.GoogleOAuthClient;
import org.example.unibooker.domain.auth.oauth.client.KakaoOAuthClient;
import org.example.unibooker.domain.auth.oauth.client.NaverOAuthClient;
import org.example.unibooker.domain.auth.oauth.client.OAuthClient;
import org.example.unibooker.domain.auth.oauth.model.dto.OAuthDto;
import org.example.unibooker.domain.auth.oauth.model.dto.OAuthUserInfo;
import org.example.unibooker.domain.auth.oauth.model.entity.OAuthProvider;
import org.example.unibooker.domain.company.model.CompanyStatus;
import org.example.unibooker.domain.company.model.entity.Companies;
import org.example.unibooker.domain.company.repository.CompanyRepository;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.domain.user.model.UserStatus;
import org.example.unibooker.domain.user.model.entity.UserSocialAccounts;
import org.example.unibooker.domain.user.model.entity.Users;
import org.example.unibooker.domain.user.repository.UserRepository;
import org.example.unibooker.domain.user.repository.UserSocialAccountRepository;
import org.example.unibooker.domain.user.service.TokenStorageService;
import org.example.unibooker.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth 인증 서비스
 * - OAuth 인증 URL 생성
 * - 콜백 처리 (토큰 교환, 사용자 조회/생성)
 * - JWT 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final NaverOAuthClient naverOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final CompanyRepository companyRepository;

    private final JwtUtil jwtUtil;
    private final TokenStorageService tokenStorageService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${front.base-url}")
    private String frontBaseUrl;

    private static final String OAUTH_TEMP_TOKEN_PREFIX = "oauth:temp:";
    private static final long TEMP_TOKEN_TTL_MINUTES = 10;

    /**
     * OAuth 인증 URL 생성
     */
    public String getAuthorizationUrl(String provider, String companySlug) {
        // 기업 존재 및 상태 확인
        Companies company = companyRepository.findByCompanySlug(companySlug)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BaseException(BaseResponseStatus.COMPANY_NOT_APPROVED);
        }

        OAuthClient client = getOAuthClient(provider);
        return client.getAuthorizationUrl(companySlug);
    }

    /**
     * OAuth 콜백 처리
     * @return 프론트 리다이렉트 URL
     */
    @Transactional
    public String handleCallback(String provider, String code, String state) {
        String companySlug = state;

        try {
            // 1. 기업 조회
            Companies company = companyRepository.findByCompanySlug(companySlug)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

            // 2. OAuth 토큰 및 사용자 정보 조회
            OAuthClient client = getOAuthClient(provider);
            String accessToken = client.getAccessToken(code);
            OAuthUserInfo userInfo = client.getUserInfo(accessToken);

            log.info("OAuth 사용자 정보 - provider: {}, email: {}, name: {}",
                    userInfo.getProvider(), userInfo.getEmail(), userInfo.getName());

            // 3. 기존 소셜 계정 조회
            Optional<UserSocialAccounts> existingSocialAccount = socialAccountRepository
                    .findByProviderAndProviderIdAndCompanyId(
                            userInfo.getProvider(),
                            userInfo.getProviderId(),
                            company.getId()
                    );

            if (existingSocialAccount.isPresent()) {
                // 기존 OAuth 사용자 → 바로 로그인
                Users user = existingSocialAccount.get().getUser();
                return handleExistingUser(user, company);
            }

            // 4. 이메일로 기존 일반 사용자 조회
            Optional<Users> existingUser = userRepository.findByEmailAndCompany_IdAndRoleAndStatusNot(
                    userInfo.getEmail(),
                    company.getId(),
                    UserRole.USER,
                    UserStatus.DELETED
            );

            if (existingUser.isPresent()) {
                // 기존 일반 사용자 → OAuth 연동 후 로그인
                Users user = existingUser.get();
                linkSocialAccount(user, userInfo);
                return handleExistingUser(user, company);
            }

            // 5. 신규 사용자 → 약관 동의 페이지로 이동
            return handleNewUser(userInfo, company);

        } catch (BaseException e) {
            log.error("OAuth 콜백 처리 실패: {}", e.getMessage());
            return buildErrorRedirectUrl(companySlug, e.getStatus().getMessage());
        } catch (Exception e) {
            log.error("OAuth 콜백 처리 중 오류 발생", e);
            return buildErrorRedirectUrl(companySlug, "OAuth 인증에 실패했습니다.");
        }
    }

    /**
     * 약관 동의 후 가입 완료
     */
    @Transactional
    public OAuthDto.LoginResponse completeSignup(OAuthDto.CompleteRequest request) {
        // 1. 약관 동의 확인
        if (!request.isTermsAgreed() || !request.isPrivacyAgreed()) {
            throw new BaseException(BaseResponseStatus.OAUTH_AGREEMENT_REQUIRED);
        }

        // 2. 임시 토큰에서 사용자 정보 조회
        String key = OAUTH_TEMP_TOKEN_PREFIX + request.getToken();
        String tempDataJson = redisTemplate.opsForValue().get(key);

        if (tempDataJson == null) {
            throw new BaseException(BaseResponseStatus.OAUTH_TEMP_TOKEN_EXPIRED);
        }

        OAuthDto.TempUserInfo tempUserInfo;
        try {
            tempUserInfo = objectMapper.readValue(tempDataJson, OAuthDto.TempUserInfo.class);
        } catch (JsonProcessingException e) {
            throw new BaseException(BaseResponseStatus.OAUTH_TEMP_TOKEN_EXPIRED);
        }

        // 3. 기업 조회
        Companies company = companyRepository.findById(tempUserInfo.getCompanyId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        // 4. 사용자 생성 (password null)
        Users newUser = Users.builder()
                .email(tempUserInfo.getEmail())
                .password(null)
                .name(tempUserInfo.getName())
                .company(company)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .isFirstLogin(false)
                .build();

        userRepository.save(newUser);

        // 5. 소셜 계정 연동
        OAuthProvider provider = OAuthProvider.from(tempUserInfo.getProvider());
        UserSocialAccounts socialAccount = UserSocialAccounts.create(
                newUser,
                provider,
                tempUserInfo.getProviderId()
        );
        socialAccountRepository.save(socialAccount);

        // 6. 임시 토큰 삭제
        redisTemplate.delete(key);

        log.info("OAuth 회원가입 완료 - userId: {}, provider: {}", newUser.getId(), provider);

        return OAuthDto.LoginResponse.builder()
                .userId(newUser.getId())
                .name(newUser.getName())
                .email(newUser.getEmail())
                .role(newUser.getRole().name())
                .companyId(company.getId())
                .companySlug(company.getCompanySlug())
                .isNewUser(true)
                .build();
    }

    /**
     * 이메일 입력 처리 (카카오 등)
     */
    @Transactional
    public OAuthDto.EmailResponse submitEmail(OAuthDto.EmailRequest request) {
        // 1. 임시 토큰에서 정보 조회
        String key = OAUTH_TEMP_TOKEN_PREFIX + request.getToken();
        String tempDataJson = redisTemplate.opsForValue().get(key);

        if (tempDataJson == null) {
            throw new BaseException(BaseResponseStatus.OAUTH_TEMP_TOKEN_EXPIRED);
        }

        OAuthDto.TempUserInfo tempUserInfo;
        try {
            tempUserInfo = objectMapper.readValue(tempDataJson, OAuthDto.TempUserInfo.class);
        } catch (JsonProcessingException e) {
            throw new BaseException(BaseResponseStatus.OAUTH_TEMP_TOKEN_EXPIRED);
        }

        // 2. 기업 조회
        Companies company = companyRepository.findById(tempUserInfo.getCompanyId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        String email = request.getEmail();

        // 3. 이메일로 기존 사용자 조회
        Optional<Users> existingUser = userRepository.findByEmailAndCompany_IdAndRoleAndStatusNot(
                email,
                company.getId(),
                UserRole.USER,
                UserStatus.DELETED
        );

        // 4. 기존 토큰 삭제
        redisTemplate.delete(key);

        if (existingUser.isPresent()) {
            // 기존 사용자 → 소셜 계정 연동 후 로그인
            Users user = existingUser.get();

            OAuthProvider provider = OAuthProvider.from(tempUserInfo.getProvider());
            UserSocialAccounts socialAccount = UserSocialAccounts.create(
                    user,
                    provider,
                    tempUserInfo.getProviderId()
            );
            socialAccountRepository.save(socialAccount);

            log.info("OAuth 이메일 입력 - 기존 사용자 연동, userId: {}", user.getId());

            return OAuthDto.EmailResponse.builder()
                    .status("success")
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .companySlug(company.getCompanySlug())
                    .build();
        }

        // 5. 신규 사용자 → 약관 동의 필요
        String newToken = UUID.randomUUID().toString();

        OAuthDto.TempUserInfo newTempUserInfo = OAuthDto.TempUserInfo.builder()
                .provider(tempUserInfo.getProvider())
                .providerId(tempUserInfo.getProviderId())
                .email(email)  // 입력받은 이메일 저장
                .name(tempUserInfo.getName())
                .companyId(company.getId())
                .companySlug(company.getCompanySlug())
                .build();

        try {
            String newTempDataJson = objectMapper.writeValueAsString(newTempUserInfo);
            String newKey = OAUTH_TEMP_TOKEN_PREFIX + newToken;
            redisTemplate.opsForValue().set(newKey, newTempDataJson, TEMP_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("OAuth 이메일 입력 - 신규 사용자, 약관 동의 필요");

        return OAuthDto.EmailResponse.builder()
                .status("new")
                .name(tempUserInfo.getName())
                .email(email)
                .companySlug(company.getCompanySlug())
                .token(newToken)
                .build();
    }

    /**
     * 기존 사용자 로그인 처리
     */
    private String handleExistingUser(Users user, Companies company) {
        // 계정 상태 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BaseException(BaseResponseStatus.INVALID_USER_STATUS);
        }

        log.info("OAuth 로그인 성공 - userId: {}, email: {}", user.getId(), user.getEmail());

        // JWT는 Controller에서 Cookie로 설정
        return buildSuccessRedirectUrl(company.getCompanySlug(), user.getId());
    }

    /**
     * 신규 사용자 처리 (약관 동의 필요)
     */
    private String handleNewUser(OAuthUserInfo userInfo, Companies company) {
        // 카카오 등 이메일 미제공 시 이메일 입력 페이지로 이동
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            return handleEmailRequired(userInfo, company);
        }

        // 임시 토큰 생성
        String tempToken = UUID.randomUUID().toString();

        // 임시 사용자 정보 저장
        OAuthDto.TempUserInfo tempUserInfo = OAuthDto.TempUserInfo.builder()
                .provider(userInfo.getProvider().name())
                .providerId(userInfo.getProviderId())
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .companyId(company.getId())
                .companySlug(company.getCompanySlug())
                .build();

        try {
            String tempDataJson = objectMapper.writeValueAsString(tempUserInfo);
            String key = OAUTH_TEMP_TOKEN_PREFIX + tempToken;
            redisTemplate.opsForValue().set(key, tempDataJson, TEMP_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("OAuth 신규 사용자 - 약관 동의 필요, email: {}", userInfo.getEmail());

        return buildNewUserRedirectUrl(company.getCompanySlug(), tempToken);
    }

    /**
     * 이메일 입력 필요 (카카오 등)
     */
    private String handleEmailRequired(OAuthUserInfo userInfo, Companies company) {
        String tempToken = UUID.randomUUID().toString();

        OAuthDto.TempUserInfo tempUserInfo = OAuthDto.TempUserInfo.builder()
                .provider(userInfo.getProvider().name())
                .providerId(userInfo.getProviderId())
                .email(null)  // 이메일 없음
                .name(userInfo.getName())
                .companyId(company.getId())
                .companySlug(company.getCompanySlug())
                .build();

        try {
            String tempDataJson = objectMapper.writeValueAsString(tempUserInfo);
            String key = OAUTH_TEMP_TOKEN_PREFIX + tempToken;
            redisTemplate.opsForValue().set(key, tempDataJson, TEMP_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("OAuth 이메일 미제공 - 이메일 입력 필요, provider: {}", userInfo.getProvider());

        return buildEmailRequiredRedirectUrl(company.getCompanySlug(), tempToken);
    }

    /**
     * 이메일 입력 리다이렉트 URL 생성
     */
    private String buildEmailRequiredRedirectUrl(String companySlug, String token) {
        return frontBaseUrl + "/c/" + companySlug + "/oauth/callback?status=email_required&token=" + token;
    }

    /**
     * 소셜 계정 연동
     */
    private void linkSocialAccount(Users user, OAuthUserInfo userInfo) {
        UserSocialAccounts socialAccount = UserSocialAccounts.create(
                user,
                userInfo.getProvider(),
                userInfo.getProviderId()
        );
        socialAccountRepository.save(socialAccount);

        log.info("OAuth 계정 연동 - userId: {}, provider: {}", user.getId(), userInfo.getProvider());
    }

    /**
     * OAuth 클라이언트 조회
     */
    private OAuthClient getOAuthClient(String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.from(provider);
        return switch (oAuthProvider) {
            case KAKAO -> kakaoOAuthClient;
            case NAVER -> naverOAuthClient;
            case GOOGLE -> googleOAuthClient;
        };
    }

    /**
     * 로그인 성공 리다이렉트 URL 생성
     */
    private String buildSuccessRedirectUrl(String companySlug, Long userId) {
        return frontBaseUrl + "/c/" + companySlug + "/oauth/callback?status=success&userId=" + userId;
    }

    /**
     * 신규 사용자 리다이렉트 URL 생성
     */
    private String buildNewUserRedirectUrl(String companySlug, String token) {
        return frontBaseUrl + "/c/" + companySlug + "/oauth/callback?status=new&token=" + token;
    }

    /**
     * 에러 리다이렉트 URL 생성
     */
    private String buildErrorRedirectUrl(String companySlug, String message) {
        return frontBaseUrl + "/c/" + companySlug + "/oauth/callback?status=error&message=" + message;
    }

    /**
     * JWT 토큰 생성 (Controller에서 호출)
     */
    public String[] createTokens(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);

        // Refresh Token 저장
        tokenStorageService.saveRefreshToken(userId, refreshToken, 604800000L);

        return new String[]{accessToken, refreshToken};
    }
}