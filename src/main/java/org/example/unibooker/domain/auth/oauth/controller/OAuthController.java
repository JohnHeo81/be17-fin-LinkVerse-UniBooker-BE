package org.example.unibooker.domain.auth.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.common.BaseResponse;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.auth.oauth.model.dto.OAuthDto;
import org.example.unibooker.domain.auth.oauth.service.OAuthService;
import org.example.unibooker.domain.company.model.entity.Companies;
import org.example.unibooker.domain.company.repository.CompanyRepository;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.domain.user.model.dto.AuthDto;
import org.example.unibooker.utils.CookieUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * OAuth 인증 컨트롤러
 * - OAuth 인증 URL 제공
 * - OAuth 콜백 처리
 * - 약관 동의 후 가입 완료
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth", description = "소셜 로그인 API")
public class OAuthController {

    private final OAuthService oAuthService;
    private final CompanyRepository companyRepository;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secureCookie;

    /**
     * OAuth 인증 URL 요청
     * - 프론트에서 호출 → OAuth 제공자 로그인 페이지로 리다이렉트
     */
    @Operation(summary = "OAuth 인증 URL 요청", description = "소셜 로그인을 위한 인증 URL을 반환합니다.")
    @GetMapping("/{provider}")
    public void redirectToOAuth(
            @PathVariable String provider,
            @RequestParam String companySlug,
            HttpServletResponse response) throws IOException {

        log.info("OAuth 인증 요청 - provider: {}, companySlug: {}", provider, companySlug);

        String authUrl = oAuthService.getAuthorizationUrl(provider, companySlug);
        response.sendRedirect(authUrl);
    }

    /**
     * OAuth 콜백 처리
     * - OAuth 제공자로부터 인증 코드 수신
     * - 토큰 교환 및 사용자 처리 후 프론트로 리다이렉트
     */
    @Operation(summary = "OAuth 콜백", description = "OAuth 인증 완료 후 콜백을 처리합니다.")
    @GetMapping("/{provider}/callback")
    public void handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        log.info("OAuth 콜백 수신 - provider: {}, state: {}", provider, state);

        String redirectUrl = oAuthService.handleCallback(provider, code, state);

        // 기존 사용자인 경우 JWT 쿠키 설정
        if (redirectUrl.contains("status=success")) {
            String userId = extractUserIdFromUrl(redirectUrl);
            if (userId != null) {
                String[] tokens = oAuthService.createTokens(Long.parseLong(userId));
                setTokenCookies(response, tokens[0], tokens[1]);
            }
        }

        response.sendRedirect(redirectUrl);
    }

    /**
     * 약관 동의 후 가입 완료
     * - 신규 사용자가 약관 동의 후 호출
     * - 계정 생성 및 JWT 발급
     */
    @Operation(summary = "OAuth 가입 완료", description = "약관 동의 후 회원가입을 완료합니다.")
    @PostMapping("/complete")
    public ResponseEntity<BaseResponse<OAuthDto.LoginResponse>> completeSignup(
            @RequestBody OAuthDto.CompleteRequest request,
            HttpServletResponse response) {

        log.info("OAuth 가입 완료 요청 - token: {}", request.getToken());

        OAuthDto.LoginResponse loginResponse = oAuthService.completeSignup(request);

        // JWT 쿠키 설정
        String[] tokens = oAuthService.createTokens(loginResponse.getUserId());
        setTokenCookies(response, tokens[0], tokens[1]);

        return ResponseEntity.ok(BaseResponse.success(loginResponse));
    }

    /**
     * 이메일 입력 처리 (카카오 등 이메일 미제공 시)
     */
    @Operation(summary = "OAuth 이메일 입력", description = "이메일 미제공 OAuth 로그인 시 이메일을 입력받습니다.")
    @PostMapping("/email")
    public ResponseEntity<BaseResponse<OAuthDto.EmailResponse>> submitEmail(
            @RequestBody OAuthDto.EmailRequest request,
            HttpServletResponse response) {

        log.info("OAuth 이메일 입력 요청 - token: {}", request.getToken());

        OAuthDto.EmailResponse emailResponse = oAuthService.submitEmail(request);

        // 기존 사용자인 경우 JWT 쿠키 설정
        if ("success".equals(emailResponse.getStatus())) {
            String[] tokens = oAuthService.createTokens(emailResponse.getUserId());
            setTokenCookies(response, tokens[0], tokens[1]);
        }

        return ResponseEntity.ok(BaseResponse.success(emailResponse));
    }

    /**
     * JWT 토큰 쿠키 설정
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // 기존 모든 역할 쿠키 삭제 (단일 세션 정책)
        CookieUtil.deleteAllRolesCookies(response);

        // USER 역할로 쿠키 설정
        response.addCookie(CookieUtil.createAccessTokenCookie(accessToken, UserRole.USER));
        response.addCookie(CookieUtil.createRefreshTokenCookie(refreshToken, UserRole.USER));

        log.info("OAuth JWT 쿠키 설정 완료");
    }

    /**
     * URL에서 userId 추출
     */
    private String extractUserIdFromUrl(String url) {
        try {
            String[] parts = url.split("userId=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        } catch (Exception e) {
            log.error("userId 추출 실패: {}", url);
        }
        return null;
    }

    // ========== 소셜 계정 연동 관리 ==========

    /**
     * 연동된 소셜 계정 목록 조회
     */
    @Operation(summary = "소셜 계정 목록", description = "연동된 소셜 계정 목록을 조회합니다.")
    @GetMapping("/accounts")
    public ResponseEntity<BaseResponse<List<OAuthDto.LinkedAccount>>> getLinkedAccounts(
            @AuthenticationPrincipal AuthDto.AuthUser authUser) {

        List<OAuthDto.LinkedAccount> accounts = oAuthService.getLinkedAccounts(authUser.getId());
        return ResponseEntity.ok(BaseResponse.success(accounts));
    }

    /**
     * 소셜 계정 연동 요청 (마이페이지에서)
     */
    @GetMapping("/link/{provider}")
    public void linkSocialAccount(
            @PathVariable String provider,
            @AuthenticationPrincipal AuthDto.AuthUser authUser,
            HttpServletResponse response) throws IOException {

        log.info("소셜 계정 연동 요청 - provider: {}, userId: {}", provider, authUser.getId());

        // companySlug 조회
        Companies company = companyRepository.findById(authUser.getCompanyId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        String authUrl = oAuthService.getLinkAuthorizationUrl(
                provider,
                authUser.getId(),           // getUserId() → getId()
                authUser.getCompanyId(),
                company.getCompanySlug()    // 조회한 slug 사용
        );
        response.sendRedirect(authUrl);
    }

    /**
     * 소셜 계정 연동 콜백
     */
    @Operation(summary = "소셜 연동 콜백", description = "소셜 계정 연동 콜백을 처리합니다.")
    @GetMapping("/link/{provider}/callback")
    public void handleLinkCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        log.info("소셜 연동 콜백 수신 - provider: {}, state: {}", provider, state);

        String redirectUrl = oAuthService.handleLinkCallback(provider, code, state);
        response.sendRedirect(redirectUrl);
    }

    /**
     * 소셜 계정 연동 해제
     */
    @Operation(summary = "소셜 계정 연동 해제", description = "연동된 소셜 계정을 해제합니다.")
    @DeleteMapping("/accounts/{provider}")
    public ResponseEntity<BaseResponse<String>> unlinkSocialAccount(
            @PathVariable String provider,
            @AuthenticationPrincipal AuthDto.AuthUser authUser) {

        log.info("소셜 계정 연동 해제 - provider: {}, userId: {}", provider, authUser.getId());

        oAuthService.unlinkSocialAccount(authUser.getId(), provider);
        return ResponseEntity.ok(BaseResponse.success("소셜 계정 연동이 해제되었습니다."));
    }
}