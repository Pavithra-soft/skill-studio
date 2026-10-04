package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillCurriculumTest {

    @Test
    void javaIsFirstSkillAndStartsWithIdentity() {
        List<SkillCurriculum.Outline> all = SkillCurriculum.all();
        assertThat(all).isNotEmpty();
        assertThat(all.getFirst().key()).isEqualTo("java");
        CoreConcept first = all.getFirst().concepts().getFirst();
        assertThat(first.getTitle()).containsIgnoringCase("equals");
        assertThat(first.getTitle()).containsIgnoringCase("hashCode");
        assertThat(first.getTitle()).containsIgnoringCase("identity");
    }

    @Test
    void everyCatalogSkillHasAtLeastEightConcepts() {
        assertThat(SkillCurriculum.all()).hasSize(14);
        for (SkillCurriculum.Outline outline : SkillCurriculum.all()) {
            assertThat(outline.concepts())
                    .as(outline.key())
                    .hasSizeGreaterThanOrEqualTo(8);
        }
    }

    @Test
    void javaFirstConceptDepthIsInterviewGrade() {
        CoreConcept first = SkillCurriculum.all().getFirst().concepts().getFirst();
        assertThat(first.getDepth().length()).isGreaterThan(400);
        assertThat(first.getInterviews()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(first.getSamples()).isNotEmpty();
    }

    @Test
    void javaIncludesSolidConcept() {
        List<String> titles = SkillCurriculum.all().getFirst().concepts().stream()
                .map(CoreConcept::getTitle)
                .toList();
        assertThat(titles).anyMatch(title -> title.toUpperCase().contains("SOLID"));
    }

    @Test
    void skillsFollowInterviewPriorityOrder() {
        assertThat(SkillCurriculum.all().stream().map(SkillCurriculum.Outline::key).toList())
                .containsExactly(
                        "java",
                        "spring-boot",
                        "spring-framework",
                        "rest",
                        "spring-security",
                        "kafka",
                        "postgresql",
                        "microservices",
                        "kubernetes",
                        "docker",
                        "spring-ai",
                        "keycloak",
                        "redis",
                        "aws-sdk");
    }
}
