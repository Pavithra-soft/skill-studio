package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonCopyTest {

    @Test
    void polishDoesNotMutateTheCatalogObject() {
        CoreConcept original = SkillCurriculum.all().getFirst().concepts().getFirst();
        int before = original.getPoints().size();
        CoreConcept polished = LessonCopy.polish(original);
        assertThat(polished.getPoints()).isNotEmpty();
        assertThat(original.getPoints()).hasSize(before);
        assertThat(polished).isNotSameAs(original);
    }

    @Test
    void extractPointsTurnsDepthIntoShortBullets() {
        CoreConcept concept = new CoreConcept("Demo", "One line why.", "A production story.");
        concept.depth("""
                First idea is a complete sentence. Second idea follows.

                The trap is mutating a map key. Never do that in production.
                """);
        CoreConcept polished = LessonCopy.polish(concept);
        assertThat(polished.getPoints()).isNotEmpty();
        assertThat(polished.getPoints().getFirst()).contains("One line why");
        assertThat(polished.getTrap()).containsIgnoringCase("trap");
        assertThat(polished.getTakeaway()).isNotBlank();
    }
}
