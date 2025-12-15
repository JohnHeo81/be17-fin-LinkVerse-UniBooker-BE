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
 * 구글 OAuth 클라이언트
 * - 구글 인증 URL 생성
 * - 토큰 교환 및 사용자 정보 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthClient {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String getAuthorizationUrl(String state) {
        return AUTHORIZATION_URL +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=email%20profile" +
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
            log.error("구글 토큰 발급 실패: {}", response);
            throw new RuntimeException("구글 토큰 발급 실패");
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
            log.error("구글 사용자 정보 조회 실패");
            throw new RuntimeException("구글 사용자 정보 조회 실패");
        }

        String providerId = (String) response.get("id");
        String email = (String) response.get("email");
        String name = (String) response.get("name");

        return OAuthUserInfo.fromGoogle(providerId, email, name);
    }
}