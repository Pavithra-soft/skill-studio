package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibrarySearchTest {

    private final SkillLessonService service = new SkillLessonService();

    @Test
    void solidQuestionHitsJavaSolidConceptOrPattern() {
        SkillLesson java = service.lesson("java");
        var hits = LibrarySearch.search(java, "SOLID principles dependency inversion", 4);
        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().title()).containsIgnoringCase("SOLID");
        assertThat(hits.getFirst().title()).doesNotContain("Observer");
    }

    @Test
    void searchAllFindsEqualsFromTheLibrary() {
        var hits = LibrarySearch.searchAll("equals hashCode HashMap key", 4);
        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().title()).containsIgnoringCase("equals");
    }

    @Test
    void weakLocalHitWidensToTheCatalog() {
        SkillLesson redis = service.lesson("redis");
        var hits = LibrarySearch.searchOrWiden(redis, "SOLID single responsibility", 4);
        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().title()).containsIgnoringCase("SOLID");
    }
}
