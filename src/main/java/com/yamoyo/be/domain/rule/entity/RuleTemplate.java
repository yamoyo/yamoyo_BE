package com.yamoyo.be.domain.rule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 규칙 템플릿 엔티티
 * - 규칙 정보 : 프론트, 최종 표시 정보 (요약된 규칙): 백
 */
@Entity
@Table(name = "rule_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "content", length = 255)
    private String content;

    // 정적 팩토리 메서드
    public static RuleTemplate of(String content) {
        RuleTemplate template = new RuleTemplate();
        template.content = content;
        return template;
    }
}
