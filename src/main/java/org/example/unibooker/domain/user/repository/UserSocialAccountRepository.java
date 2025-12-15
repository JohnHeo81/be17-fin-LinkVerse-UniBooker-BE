package org.example.unibooker.domain.user.repository;

import org.example.unibooker.domain.auth.oauth.model.entity.OAuthProvider;
import org.example.unibooker.domain.user.model.entity.UserSocialAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 소셜 계정 Repository
 * - OAuth 연동 정보 조회
 */
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccounts, Long> {

    /**
     * provider + providerId + companyId로 소셜 계정 조회
     */
    @Query("SELECT usa FROM UserSocialAccounts usa " +
            "JOIN usa.user u " +
            "WHERE usa.provider = :provider " +
            "AND usa.providerId = :providerId " +
            "AND u.company.id = :companyId " +
            "AND u.deletedAt IS NULL")
    Optional<UserSocialAccounts> findByProviderAndProviderIdAndCompanyId(
            @Param("provider") OAuthProvider provider,
            @Param("providerId") String providerId,
            @Param("companyId") Long companyId
    );

    /**
     * 사용자 ID + provider로 소셜 계정 조회
     */
    Optional<UserSocialAccounts> findByUserIdAndProvider(Long userId, OAuthProvider provider);

    /**
     * 사용자 ID로 연동된 소셜 계정 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}