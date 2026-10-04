package com.bharath.skillstudio.ai;

import com.bharath.skillstudio.learn.LibrarySearch;
import com.bharath.skillstudio.learn.SkillLesson;
import com.bharath.skillstudio.learn.SkillLessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TutorServiceTest {

    private SkillLessonService service;

    @BeforeEach
    void setUp() {
        service = new SkillLessonService();
    }

    @Test
    void dependencyInversionAnswersThePrincipleNotASearchDump() {
        var reply = service.chat("java", "solid-in-production-java",
                "When do I use dependency inversion?", "tutor-dip", false);
        assertThat(reply.getAnswer()).doesNotContain("Direct answer from the catalog");
        assertThat(reply.getAnswer()).doesNotStartWith("When do I use");
        assertThat(reply.getAnswer()).doesNotContain("Observer");
        assertThat(reply.getAnswer().toLowerCase()).contains("depend");
        assertThat(reply.getSources()).anyMatch(source ->
                source.toLowerCase().contains("solid") || source.toLowerCase().contains("depend"));
    }

    @Test
    void concurrentHashMapHitsCollectionsNotARandomStandard() {
        SkillLesson java = service.lesson("java");
        var hits = LibrarySearch.search(java, "When do I use ConcurrentHashMap?", 3);
        assertThat(hits.getFirst().title()).containsIgnoringCase("Collections");
        var reply = service.chat("java", null, "When do I use ConcurrentHashMap?", "tutor-chm", false);
        assertThat(reply.getAnswer().toLowerCase()).contains("concurrent");
        assertThat(reply.getAnswer()).contains("•");
    }

    @Test
    void quizThenAnswerIsGradedAgainstTheCatalog() {
        var quiz = service.chat("java", "solid-in-production-java", "", "tutor-quiz", true);
        assertThat(quiz.getAnswer()).startsWith("Quiz.");
        var graded = service.chat("java", "solid-in-production-java",
                "HTTP client, JSON parser, and repository are three types so each has one reason to change.",
                "tutor-quiz", false);
        assertThat(graded.getAnswer()).containsAnyOf("On track", "Partly", "Not yet");
        assertThat(graded.getAnswer().toLowerCase()).containsAnyOf("search", "adzuna", "parser", "orchestrat", "reason");
    }
}
