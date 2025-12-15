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
 * 네이버 OAuth 클라이언트
 * - 네이버 인증 URL 생성
 * - 토큰 교환 및 사용자 정보 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverOAuthClient implements OAuthClient {

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

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
                        "&code=" + code)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            log.error("네이버 토큰 발급 실패: {}", response);
            throw new RuntimeException("네이버 토큰 발급 실패");
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
            log.error("네이버 사용자 정보 조회 실패");
            throw new RuntimeException("네이버 사용자 정보 조회 실패");
        }

        Map<String, Object> naverResponse = (Map<String, Object>) response.get("response");
        if (naverResponse == null) {
            log.error("네이버 사용자 정보 응답 형식 오류: {}", response);
            throw new RuntimeException("네이버 사용자 정보 조회 실패");
        }

        String providerId = (String) naverResponse.get("id");
        String email = (String) naverResponse.get("email");
        String name = (String) naverResponse.get("name");

        return OAuthUserInfo.fromNaver(providerId, email, name);
    }
}