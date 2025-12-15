package org.example.unibooker.domain.auth.oauth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth 관련 DTO
 */
public class OAuthDto {

    /**
     * OAuth 인증 URL 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthUrlResponse {
        private String authUrl;
    }

    /**
     * OAuth 가입 완료 요청 (약관 동의 후)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteRequest {
        private String token;              // 임시 토큰
        private boolean termsAgreed;       // 이용약관 동의
        private boolean privacyAgreed;     // 개인정보 수집 동의
    }

    /**
     * OAuth 로그인/가입 완료 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private Long companyId;
        private String companySlug;
        private boolean isNewUser;         // 신규 가입 여부
    }

    /**
     * 이메일 입력 요청 (카카오 등 이메일 미제공 시)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailRequest {
        private String token;    // 임시 토큰
        private String email;    // 입력받은 이메일
    }

    /**
     * 이메일 입력 결과 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailResponse {
        private String status;         // "success" 또는 "new"
        private Long userId;           // 기존 사용자인 경우
        private String name;
        private String email;
        private String companySlug;
        private String token;          // 신규 사용자인 경우 (약관 동의용)
    }

    /**
     * OAuth 임시 토큰에 저장할 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempUserInfo {
        private String provider;
        private String providerId;
        private String email;
        private String name;
        private Long companyId;
        private String companySlug;
    }
}