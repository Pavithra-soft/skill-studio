package com.bharath.skillstudio.learn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a small Spring Boot studio that encodes selected skill standards, patterns, and concepts.
 */
public final class StudioProjectWriter {

    private StudioProjectWriter() {
    }

    public static byte[] zip(List<SkillLesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            throw new IllegalArgumentException("Pick at least one skill for the project.");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                put(zip, "skill-studio/README.md", readme(lessons));
                put(zip, "skill-studio/CODING_STANDARDS.md", standardsDoc(lessons));
                put(zip, "skill-studio/DESIGN_PATTERNS.md", patternsDoc(lessons));
                put(zip, "skill-studio/.editorconfig", editorConfig());
                put(zip, "skill-studio/pom.xml", pom(lessons));
                put(zip, "skill-studio/src/main/resources/application.yml", applicationYml(lessons));
                put(zip, "skill-studio/src/main/java/com/studio/StudioApplication.java", application());
                put(zip, "skill-studio/src/main/java/com/studio/ConceptDemo.java", conceptDemo());
                put(zip, "skill-studio/src/main/java/com/studio/StudioRules.java", studioRules(lessons));
                for (SkillLesson lesson : lessons) {
                    for (CoreConcept concept : lesson.getConcepts()) {
                        if (concept == null || concept.getSlug() == null || concept.getSlug().isBlank()) {
                            continue;
                        }
                        String pkgPath = packageName(lesson.getKey()).replace('.', '/');
                        String className = className(concept.getSlug());
                        put(zip, "skill-studio/src/main/java/" + pkgPath + "/" + className + ".java",
                                conceptClass(lesson, concept));
                        String sampleDoc = sampleMarkdown(lesson, concept);
                        if (!sampleDoc.isBlank()) {
                            put(zip, "skill-studio/docs/examples/" + lesson.getKey() + "/" + concept.getSlug() + ".md",
                                    sampleDoc);
                        }
                    }
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the studio zip", e);
        }
    }

    static String packageName(String skillKey) {
        String key = skillKey == null ? "custom" : skillKey.replace("custom:", "custom.");
        key = key.replace("-", "");
        key = key.replaceAll("[^a-zA-Z0-9.]", "");
        if (key.isBlank()) {
            key = "custom";
        }
        if (Character.isDigit(key.charAt(0))) {
            key = "s" + key;
        }
        return "com.studio." + key;
    }

    static String className(String slug) {
        String source = slug == null || slug.isBlank() ? "concept" : slug;
        StringBuilder out = new StringBuilder();
        for (String part : source.split("[^a-zA-Z0-9]+")) {
            if (part.isBlank()) {
                continue;
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        if (out.isEmpty()) {
            out.append("Concept");
        }
        if (Character.isDigit(out.charAt(0))) {
            out.insert(0, "N");
        }
        return out.append("Demo").toString();
    }

    private static void put(ZipOutputStream zip, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String readme(List<SkillLesson> lessons) {
        StringBuilder out = new StringBuilder();
        out.append("# Skill studio\n\n");
        out.append("Generated from Skill Studio. This is a compiling Spring Boot 3.5 skeleton that encodes ");
        out.append("the coding standards and design patterns for the skills you selected, with one demo class per concept.\n\n");
        out.append("## Selected skills\n\n");
        for (SkillLesson lesson : lessons) {
            out.append("- **").append(lesson.getName()).append("** (`").append(lesson.getKey()).append("`)");
            out.append('\n');
        }
        out.append("\n## Concepts\n\n");
        for (SkillLesson lesson : lessons) {
            for (CoreConcept concept : lesson.getConcepts()) {
                out.append("- ").append(lesson.getName()).append(" / **").append(concept.getTitle()).append("** — ");
                out.append(firstSentence(concept.getWhy())).append('\n');
            }
        }
        out.append("\n## How the rules show up\n\n");
        out.append("- `CODING_STANDARDS.md` — the rules you filter by skill.\n");
        out.append("- `DESIGN_PATTERNS.md` — the patterns those rules imply.\n");
        out.append("- `StudioRules` — the same rules as Java constants, injected into demos.\n");
        out.append("- `docs/examples/` — programming samples from the huddle (screen only; not compiled).\n");
        out.append("- Each `*Demo` class implements `ConceptDemo` (strategy) and is constructor-injected.\n\n");
        out.append("## Run\n\n");
        out.append("```bash\n");
        out.append("mvn -q spring-boot:run\n");
        out.append("```\n\n");
        out.append("Startup logs the selected concepts. Examples stay in markdown so incomplete snippets cannot break the build.\n");
        return out.toString();
    }

    private static String standardsDoc(List<SkillLesson> lessons) {
        StringBuilder out = new StringBuilder("# Coding standards\n\n");
        out.append("Apply these on every file in this studio. They come from the skill playbooks you selected.\n\n");
        for (SkillLesson lesson : lessons) {
            out.append("## ").append(lesson.getName()).append("\n\n");
            if (lesson.getStandards() == null || lesson.getStandards().isEmpty()) {
                out.append("No authored playbook for this skill. Keep constructors honest and tests slice-sized.\n\n");
                continue;
            }
            for (StandardRule rule : lesson.getStandards()) {
                out.append("### ").append(rule.getTitle()).append("\n\n");
                out.append(rule.getRule()).append("\n\n");
                out.append("_Why:_ ").append(rule.getWhy()).append("\n\n");
            }
        }
        return out.toString();
    }

    private static String patternsDoc(List<SkillLesson> lessons) {
        StringBuilder out = new StringBuilder("# Design patterns\n\n");
        out.append("Use these shapes when you extend the studio. Names match the interview cards.\n\n");
        for (SkillLesson lesson : lessons) {
            out.append("## ").append(lesson.getName()).append("\n\n");
            if (lesson.getPatterns() == null || lesson.getPatterns().isEmpty()) {
                out.append("No authored patterns. Prefer strategy at the edge and keep adapters thin.\n\n");
                continue;
            }
            for (PatternRule pattern : lesson.getPatterns()) {
                out.append("### ").append(pattern.getName()).append("\n\n");
                out.append(pattern.getIntent()).append("\n\n");
                out.append("_In this domain:_ ").append(pattern.getHow()).append("\n\n");
            }
        }
        return out.toString();
    }

    private static String editorConfig() {
        return """
                root = true

                [*]
                charset = utf-8
                end_of_line = lf
                insert_final_newline = true
                indent_style = space
                indent_size = 4
                trim_trailing_whitespace = true

                [*.{yml,yaml,md,xml}]
                indent_size = 2
                """;
    }

    private static String pom(List<SkillLesson> lessons) {
        Set<String> keys = keys(lessons);
        StringBuilder extras = new StringBuilder();
        if (keys.contains("kafka")) {
            extras.append("""
                            <dependency>
                                <groupId>org.springframework.kafka</groupId>
                                <artifactId>spring-kafka</artifactId>
                            </dependency>
                    """);
        }
        if (keys.contains("spring-security") || keys.contains("keycloak")) {
            extras.append("""
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-security</artifactId>
                            </dependency>
                    """);
        }
        if (keys.contains("postgresql")) {
            extras.append("""
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-data-jpa</artifactId>
                            </dependency>
                    """);
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.5.5</version>
                        <relativePath/>
                    </parent>
                    <groupId>com.studio</groupId>
                    <artifactId>skill-studio</artifactId>
                    <version>1.0.0</version>
                    <name>Skill studio</name>
                    <description>Generated from Skill Studio standards and patterns</description>
                    <properties>
                        <java.version>21</java.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-actuator</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-validation</artifactId>
                        </dependency>
                """ + extras + """
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-test</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
    }

    private static String applicationYml(List<SkillLesson> lessons) {
        Set<String> keys = keys(lessons);
        StringBuilder out = new StringBuilder();
        out.append("spring:\n");
        out.append("  application:\n");
        out.append("    name: skill-studio\n");
        if (keys.contains("java") || keys.contains("spring-boot")) {
            out.append("  threads:\n");
            out.append("    virtual:\n");
            out.append("      enabled: true\n");
        }
        out.append("management:\n");
        out.append("  endpoint:\n");
        out.append("    health:\n");
        out.append("      probes:\n");
        out.append("        enabled: true\n");
        out.append("  endpoints:\n");
        out.append("    web:\n");
        out.append("      exposure:\n");
        out.append("        include: health,info\n");
        out.append("# Coding standards baked into config:\n");
        for (SkillLesson lesson : lessons) {
            for (StandardRule rule : lesson.getStandards()) {
                out.append("# - ").append(lesson.getKey()).append(" / ").append(rule.getTitle())
                        .append(": ").append(oneLine(rule.getRule())).append('\n');
            }
        }
        return out.toString();
    }

    private static String application() {
        return """
                package com.studio;

                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                import org.springframework.boot.ApplicationRunner;
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.context.annotation.Bean;

                import java.util.List;

                @SpringBootApplication
                public class StudioApplication {

                    private static final Logger log = LoggerFactory.getLogger(StudioApplication.class);

                    public static void main(String[] args) {
                        SpringApplication.run(StudioApplication.class, args);
                    }

                    @Bean
                    ApplicationRunner studioBoot(List<ConceptDemo> demos, StudioRules rules) {
                        return args -> {
                            log.info("Studio rules loaded: {} standards, {} patterns",
                                    rules.standards().size(), rules.patterns().size());
                            for (ConceptDemo demo : demos) {
                                log.info("Concept {} / {} — {}", demo.skill(), demo.slug(), demo.title());
                            }
                        };
                    }
                }
                """;
    }

    private static String conceptDemo() {
        return """
                package com.studio;

                /**
                 * Strategy: each selected concept is a bean the application can list without knowing the skill.
                 */
                public interface ConceptDemo {

                    String skill();

                    String slug();

                    String title();
                }
                """;
    }

    private static String studioRules(List<SkillLesson> lessons) {
        StringBuilder standards = new StringBuilder();
        StringBuilder patterns = new StringBuilder();
        for (SkillLesson lesson : lessons) {
            for (StandardRule rule : lesson.getStandards()) {
                standards.append("        new Rule(")
                        .append(quote(lesson.getKey())).append(", ")
                        .append(quote(rule.getTitle())).append(", ")
                        .append(quote(rule.getRule())).append("),\n");
            }
            for (PatternRule pattern : lesson.getPatterns()) {
                patterns.append("        new Rule(")
                        .append(quote(lesson.getKey())).append(", ")
                        .append(quote(pattern.getName())).append(", ")
                        .append(quote(pattern.getIntent())).append("),\n");
            }
        }
        if (standards.isEmpty()) {
            standards.append("        new Rule(\"studio\", \"Constructor injection\", \"Pass collaborators in the constructor.\"),\n");
        }
        if (patterns.isEmpty()) {
            patterns.append("        new Rule(\"studio\", \"Strategy\", \"Swap algorithms without rewriting callers.\"),\n");
        }
        return """
                package com.studio;

                import org.springframework.stereotype.Component;

                import java.util.List;

                @Component
                public class StudioRules {

                    public record Rule(String skill, String name, String text) {
                    }

                    private final List<Rule> standards = List.of(
                """ + standards + """
                            );
                    private final List<Rule> patterns = List.of(
                """ + patterns + """
                            );

                    public List<Rule> standards() {
                        return standards;
                    }

                    public List<Rule> patterns() {
                        return patterns;
                    }
                }
                """;
    }

    private static String conceptClass(SkillLesson lesson, CoreConcept concept) {
        String pkg = packageName(lesson.getKey());
        String className = className(concept.getSlug());
        String depth = concept.getDepth() == null || concept.getDepth().isBlank()
                ? concept.getWhy()
                : concept.getDepth();
        String rule = lesson.getStandards().isEmpty()
                ? "Constructor injection"
                : lesson.getStandards().getFirst().getTitle();
        String pattern = lesson.getPatterns().isEmpty()
                ? "Strategy"
                : lesson.getPatterns().getFirst().getName();
        return "package " + pkg + ";\n\n"
                + "import com.studio.ConceptDemo;\n"
                + "import com.studio.StudioRules;\n"
                + "import org.springframework.stereotype.Component;\n\n"
                + "/**\n"
                + " * " + javaDoc(concept.getTitle()) + "\n"
                + " * <p>" + javaDoc(firstSentence(depth)) + "</p>\n"
                + " * Standard: " + javaDoc(rule) + ". Pattern: " + javaDoc(pattern) + ".\n"
                + " */\n"
                + "@Component\n"
                + "public final class " + className + " implements ConceptDemo {\n\n"
                + "    private final StudioRules rules;\n\n"
                + "    public " + className + "(StudioRules rules) {\n"
                + "        this.rules = rules;\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public String skill() {\n"
                + "        return " + quote(lesson.getKey()) + ";\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public String slug() {\n"
                + "        return " + quote(concept.getSlug()) + ";\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public String title() {\n"
                + "        return " + quote(concept.getTitle()) + ";\n"
                + "    }\n\n"
                + "    public StudioRules rules() {\n"
                + "        return rules;\n"
                + "    }\n"
                + "}\n";
    }

    private static String sampleMarkdown(SkillLesson lesson, CoreConcept concept) {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(concept.getTitle()).append("\n\n");
        out.append("Skill: ").append(lesson.getName()).append("  \n");
        out.append("These samples are for the screen and for interviews. They are not compiled into the jar.\n\n");
        if (concept.getExample() != null && !concept.getExample().isBlank()) {
            out.append("## Curriculum snippet\n\n```\n").append(concept.getExample().trim()).append("\n```\n\n");
        }
        for (CodeSample sample : concept.getSamples()) {
            out.append("## ").append(sample.getTitle() == null || sample.getTitle().isBlank() ? "Example" : sample.getTitle());
            out.append("\n\n");
            if (sample.getNotes() != null && !sample.getNotes().isBlank()) {
                out.append(sample.getNotes()).append("\n\n");
            }
            out.append("```").append(sample.getLanguage() == null ? "" : sample.getLanguage()).append('\n');
            out.append(sample.getCode() == null ? "" : sample.getCode().trim()).append("\n```\n\n");
        }
        int n = 1;
        for (InterviewCard card : concept.getInterviews()) {
            out.append("## Interview ").append(n++).append("\n\n");
            out.append("**Q.** ").append(card.getQuestion()).append("\n\n");
            out.append("**A.** ").append(card.getAnswer()).append("\n\n");
            if (card.getFollowUp() != null && !card.getFollowUp().isBlank()) {
                out.append("_Follow-up:_ ").append(card.getFollowUp()).append("\n\n");
            }
        }
        return out.toString();
    }

    private static Set<String> keys(List<SkillLesson> lessons) {
        Set<String> keys = new LinkedHashSet<>();
        for (SkillLesson lesson : lessons) {
            if (lesson.getKey() != null && !lesson.getKey().isBlank()) {
                keys.add(lesson.getKey());
            }
        }
        return keys;
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }

    private static String javaDoc(String value) {
        return (value == null ? "" : value).replace("*/", "* /").replace("\n", " ");
    }

    private static String oneLine(String value) {
        return (value == null ? "" : value).replace('\n', ' ');
    }

    private static String firstSentence(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.trim();
        int dot = text.indexOf(". ");
        return dot > 40 ? text.substring(0, dot + 1) : text;
    }
}
