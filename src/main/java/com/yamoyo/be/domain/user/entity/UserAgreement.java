package com.yamoyo.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserAgreement Entity
 *
 * Role:
 * - 사용자의 약관 동의 정보를 저장하는 엔티티
 * - 어떤 사용자가 어떤 약관에 동의했는지 추적
 */
@Getter
@Table(name = "user_agreements")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    private Term term;

    @Column(name = "is_agreed")
    private Boolean isAgreed;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    public static UserAgreement create(User user, Term term, Boolean isAgreed) {
        UserAgreement agreement = new UserAgreement();
        agreement.user = user;
        agreement.term = term;
        agreement.isAgreed = isAgreed;
        if (isAgreed) {
            agreement.agreedAt = LocalDateTime.now();
        }
        return agreement;
    }

    public void agree() {
        this.isAgreed = true;
        this.agreedAt = LocalDateTime.now();
    }
}
