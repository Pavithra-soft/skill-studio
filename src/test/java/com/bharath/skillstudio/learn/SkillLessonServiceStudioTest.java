package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillLessonServiceStudioTest {

    private SkillLessonService service;

    @BeforeEach
    void setUp() {
        service = new SkillLessonService();
    }

    @Test
    void deleteCatalogSkillHidesItUntilGenerate() {
        assertThat(service.listSkills()).anyMatch(button -> "java".equals(button.key()));
        service.deleteSkill("java");
        assertThat(service.listSkills()).noneMatch(button -> "java".equals(button.key()));
        assertThatThrownBy(() -> service.lesson("java"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("removed");
        SkillLesson restored = service.generate("java");
        assertThat(restored.getKey()).isEqualTo("java");
        assertThat(service.listSkills()).anyMatch(button -> "java".equals(button.key()));
    }

    @Test
    void generateConceptMarksOverlayAndKeepsTheSkillCatalog() {
        SkillLesson lesson = service.generate("java", "solid-in-production-java");
        assertThat(lesson.getCatalog().size()).isGreaterThan(1);
        CoreConcept solid = lesson.getConcepts().stream()
                .filter(item -> "solid-in-production-java".equals(item.getSlug()))
                .findFirst()
                .orElseThrow();
        assertThat(solid.isGenerated()).isTrue();
        assertThat(solid.getPoints()).isNotEmpty();
        assertThat(solid.getTrap()).isNotBlank();
        assertThat(solid.getTakeaway()).isNotBlank();
    }

    @Test
    void chatUsesCatalogSearchWithoutAnLlm() {
        var reply = service.chat("java", "solid-in-production-java", "What is dependency inversion?", "test-1", false);
        assertThat(reply.getAnswer()).isNotBlank();
        assertThat(reply.getAnswer()).doesNotContain("Direct answer from the catalog");
        assertThat(reply.isLlm()).isFalse();
        assertThat(reply.getVoice()).isNotBlank();
        assertThat(reply.getSources()).isNotEmpty();
    }

    @Test
    void quizPullsAnInterviewQuestion() {
        var reply = service.chat("java", null, "", "test-2", true);
        assertThat(reply.getAnswer()).containsIgnoringCase("Quiz");
        assertThat(reply.getVoice()).isNotBlank();
    }
}
