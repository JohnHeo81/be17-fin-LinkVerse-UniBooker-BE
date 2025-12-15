package org.example.unibooker.domain.auth.oauth.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.auth.oauth.model.dto.OAuthUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 카카오 OAuth 클라이언트
 * - 카카오 인증 URL 생성
 * - 토큰 교환 및 사용자 정보 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String getAuthorizationUrl(String state) {
        return AUTHORIZATION_URL +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&state=" + state;
    }

    @Override
    public String getAccessToken(String code) {
        Map<String, Object> response = webClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue("grant_type=authorization_code" +
                        "&client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&redirect_uri=" + redirectUri +
                        "&code=" + code)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            log.error("카카오 토큰 발급 실패: {}", response);
            throw new RuntimeException("카카오 토큰 발급 실패");
        }

        return (String) response.get("access_token");
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        Map<String, Object> response = webClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            log.error("카카오 사용자 정보 조회 실패");
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }

        String providerId = String.valueOf(response.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        Map<String, Object> profile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : null;
        String name = profile != null ? (String) profile.get("nickname") : null;

        return OAuthUserInfo.fromKakao(providerId, email, name);
    }
}