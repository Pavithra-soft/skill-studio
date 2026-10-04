package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillPlaybookTest {

    @Test
    void everyCatalogSkillHasAuthoredStandardsAndPatterns() {
        for (SkillCurriculum.Outline outline : SkillCurriculum.all()) {
            assertThat(SkillPlaybook.authored(outline.key())).as(outline.key()).isTrue();
            assertThat(SkillPlaybook.standards(outline.key())).as(outline.key()).isNotEmpty();
            assertThat(SkillPlaybook.patterns(outline.key())).as(outline.key()).isNotEmpty();
        }
    }

    @Test
    void javaPatternsIncludeSolidAndCommonGoF() {
        List<String> names = SkillPlaybook.patterns("java").stream().map(PatternRule::getName).toList();
        assertThat(names.stream().anyMatch(name -> name.contains("SOLID") && name.contains("Single"))).isTrue();
        assertThat(names.stream().anyMatch(name -> name.contains("SOLID") && name.contains("Open"))).isTrue();
        assertThat(names.stream().anyMatch(name -> name.contains("SOLID") && name.contains("Liskov"))).isTrue();
        assertThat(names.stream().anyMatch(name -> name.contains("SOLID") && name.contains("Interface"))).isTrue();
        assertThat(names.stream().anyMatch(name -> name.contains("SOLID") && name.contains("Dependency"))).isTrue();
        assertThat(names).anyMatch(name -> name.contains("Strategy"));
        assertThat(names).anyMatch(name -> name.contains("Facade"));
        assertThat(names).anyMatch(name -> name.contains("Command"));
    }

    @Test
    void javaStandardsCoverEqualsAndInterrupts() {
        List<String> titles = SkillPlaybook.standards("java").stream().map(StandardRule::getTitle).toList();
        assertThat(titles).anyMatch(title -> title.toLowerCase().contains("equals"));
        assertThat(titles).anyMatch(title -> title.toLowerCase().contains("interrupt"));
        assertThat(titles).anyMatch(title -> title.toLowerCase().contains("enum"));
    }
}
