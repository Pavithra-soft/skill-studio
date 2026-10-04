package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillLessonPaginationTest {

    private final SkillLessonService service = new SkillLessonService();

    @Test
    void pageZeroSizeFourReturnsFourJavaConcepts() {
        SkillLesson page0 = service.lesson("java", null, 0, 4);
        assertThat(page0.getConcepts()).hasSize(4);
        assertThat(page0.getSize()).isEqualTo(4);
        assertThat(page0.getPage()).isEqualTo(0);
        assertThat(page0.getTotalPages()).isGreaterThanOrEqualTo(2);
        assertThat(page0.getCatalog()).hasSizeGreaterThanOrEqualTo(8);
        assertThat(page0.getTotalConcepts()).isEqualTo(page0.getCatalog().size());
    }

    @Test
    void pageOneReturnsDifferentSlugs() {
        SkillLesson page0 = service.lesson("java", null, 0, 4);
        SkillLesson page1 = service.lesson("java", null, 1, 4);
        List<String> slugs0 = page0.getConcepts().stream().map(CoreConcept::getSlug).toList();
        List<String> slugs1 = page1.getConcepts().stream().map(CoreConcept::getSlug).toList();
        assertThat(slugs1).isNotEmpty();
        assertThat(slugs1).doesNotContainAnyElementsOf(slugs0);
        assertThat(page1.getCatalog()).isEqualTo(page0.getCatalog());
    }

    @Test
    void outOfRangePageIsClamped() {
        SkillLesson last = service.lesson("java", null, 99, 4);
        assertThat(last.getPage()).isEqualTo(last.getTotalPages() - 1);
        assertThat(last.getConcepts()).isNotEmpty();
    }

    @Test
    void conceptFilterIgnoresPageAndReturnsOne() {
        SkillLesson page0 = service.lesson("java", null, 0, 4);
        String slug = page0.getCatalog().getFirst().slug();
        SkillLesson filtered = service.lesson("java", slug, 3, 4);
        assertThat(filtered.getConcepts()).hasSize(1);
        assertThat(filtered.getConcepts().getFirst().getSlug()).isEqualTo(slug);
        assertThat(filtered.getPage()).isEqualTo(0);
    }

    @Test
    void omittedPageReturnsEveryJavaConceptAndStandards() {
        SkillLesson all = service.lesson("java");
        assertThat(all.getConcepts()).hasSize(all.getCatalog().size());
        assertThat(all.getConcepts()).hasSizeGreaterThan(4);
        assertThat(all.getStandards()).isNotEmpty();
        assertThat(all.getTotalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void everyCatalogLessonIncludesStandards() {
        for (SkillCurriculum.Outline outline : SkillCurriculum.all()) {
            assertThat(service.lesson(outline.key()).getStandards())
                    .as(outline.key())
                    .isNotEmpty();
        }
    }
}
