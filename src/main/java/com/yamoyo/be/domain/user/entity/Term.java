package com.yamoyo.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Term Entity
 *
 * Role:
 * - 서비스 약관 정보를 저장하는 엔티티
 * - 서비스 이용약관, 개인정보 처리방침 등 다양한 약관 유형 관리
 */
@Getter
@Table(name = "terms")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_id")
    private Long id;

    @Column(name = "terms_type", nullable = false, length = 50)
    private String termsType;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String version;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Term create(String termsType, String title, String content, String version, Boolean isMandatory) {
        Term term = new Term();
        term.termsType = termsType;
        term.title = title;
        term.content = content;
        term.version = version;
        term.isMandatory = isMandatory;
        return term;
    }
}
