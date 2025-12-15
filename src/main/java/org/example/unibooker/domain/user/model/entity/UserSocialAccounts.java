package org.example.unibooker.domain.user.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.unibooker.domain.auth.oauth.model.entity.OAuthProvider;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 소셜 계정 연동 엔티티
 * - 사용자의 OAuth 연동 정보 관리
 * - 한 사용자가 여러 소셜 계정 연동 가능
 */
@Entity
@Table(
        name = "user_social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("사용자 소셜 계정")
public class UserSocialAccounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("사용자")
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("OAuth 제공자")
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    @Comment("OAuth 제공자의 사용자 ID")
    private String providerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public UserSocialAccounts(Users user, OAuthProvider provider, String providerId) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * 소셜 계정 생성 팩토리 메서드
     */
    public static UserSocialAccounts create(Users user, OAuthProvider provider, String providerId) {
        return UserSocialAccounts.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .build();
    }
}