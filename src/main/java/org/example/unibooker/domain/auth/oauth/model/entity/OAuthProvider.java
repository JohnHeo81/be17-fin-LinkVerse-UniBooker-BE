package org.example.unibooker.domain.auth.oauth.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth 제공자 Enum
 * - 지원하는 소셜 로그인 제공자 정의
 */
@Getter
@RequiredArgsConstructor
public enum OAuthProvider {

    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google");

    private final String registrationId;

    /**
     * 문자열로 OAuthProvider 조회
     */
    public static OAuthProvider from(String provider) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> KAKAO;
            case "naver" -> NAVER;
            case "google" -> GOOGLE;
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 제공자: " + provider);
        };
    }
}