package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class StudioProjectWriterTest {

    @Test
    void zipContainsReadmeAndADemoClass() throws Exception {
        SkillLesson java = new SkillLessonService().lesson("java", null, 0, 1);
        assertThat(java.getConcepts()).isNotEmpty();

        byte[] zip = StudioProjectWriter.zip(List.of(java));
        Set<String> names = new HashSet<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }

        assertThat(names).anyMatch(name -> name.endsWith("README.md"));
        assertThat(names).anyMatch(name -> name.endsWith("Demo.java"));
        assertThat(StudioProjectWriter.className("equals-hashcode-and-identity"))
                .endsWith("Demo");
    }
}
