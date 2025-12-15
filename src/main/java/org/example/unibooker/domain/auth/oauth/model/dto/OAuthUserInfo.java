package org.example.unibooker.domain.auth.oauth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.unibooker.domain.auth.oauth.model.entity.OAuthProvider;

/**
 * OAuth 제공자로부터 받은 사용자 정보
 * - 카카오, 네이버, 구글 공통 포맷
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {

    private OAuthProvider provider;    // KAKAO, NAVER, GOOGLE
    private String providerId;         // OAuth 제공자의 사용자 고유 ID
    private String email;              // 이메일
    private String name;               // 이름

    /**
     * 카카오 응답으로부터 생성
     */
    public static OAuthUserInfo fromKakao(String providerId, String email, String name) {
        return OAuthUserInfo.builder()
                .provider(OAuthProvider.KAKAO)
                .providerId(providerId)
                .email(email)
                .name(name)
                .build();
    }

    /**
     * 네이버 응답으로부터 생성
     */
    public static OAuthUserInfo fromNaver(String providerId, String email, String name) {
        return OAuthUserInfo.builder()
                .provider(OAuthProvider.NAVER)
                .providerId(providerId)
                .email(email)
                .name(name)
                .build();
    }

    /**
     * 구글 응답으로부터 생성
     */
    public static OAuthUserInfo fromGoogle(String providerId, String email, String name) {
        return OAuthUserInfo.builder()
                .provider(OAuthProvider.GOOGLE)
                .providerId(providerId)
                .email(email)
                .name(name)
                .build();
    }
}