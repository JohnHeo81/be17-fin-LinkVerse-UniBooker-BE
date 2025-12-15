package org.example.unibooker.domain.auth.oauth.client;

import org.example.unibooker.domain.auth.oauth.model.dto.OAuthUserInfo;

/**
 * OAuth 클라이언트 인터페이스
 * - 각 OAuth 제공자별 구현체가 상속
 */
public interface OAuthClient {

    /**
     * OAuth 인증 URL 생성
     * @param state companySlug (콜백 시 복원용)
     * @return 인증 URL
     */
    String getAuthorizationUrl(String state);

    /**
     * 인증 코드로 Access Token 교환
     * @param code 인증 코드
     * @return Access Token
     */
    String getAccessToken(String code);

    /**
     * Access Token으로 사용자 정보 조회
     * @param accessToken Access Token
     * @return 사용자 정보
     */
    OAuthUserInfo getUserInfo(String accessToken);
}