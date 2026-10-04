package com.bharath.skillstudio.learn;

import com.bharath.skillstudio.catalog.AwsSdkCatalog;
import com.bharath.skillstudio.catalog.DockerCatalog;
import com.bharath.skillstudio.catalog.JavaCatalog;
import com.bharath.skillstudio.catalog.KafkaCatalog;
import com.bharath.skillstudio.catalog.KeycloakCatalog;
import com.bharath.skillstudio.catalog.KubernetesCatalog;
import com.bharath.skillstudio.catalog.MicroservicesCatalog;
import com.bharath.skillstudio.catalog.PostgresqlCatalog;
import com.bharath.skillstudio.catalog.RedisCatalog;
import com.bharath.skillstudio.catalog.RestCatalog;
import com.bharath.skillstudio.catalog.SpringAiCatalog;
import com.bharath.skillstudio.catalog.SpringBootCatalog;
import com.bharath.skillstudio.catalog.SpringFrameworkCatalog;
import com.bharath.skillstudio.catalog.SpringSecurityCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Authored interview huddles, ordered by how often they show up in senior Java backend loops.
 */
public final class SkillCurriculum {

    public record Outline(String key, String name, String summary, List<CoreConcept> concepts) {
    }

    private static final Map<String, Outline> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        add(JavaCatalog.outline());
        add(SpringBootCatalog.outline());
        add(SpringFrameworkCatalog.outline());
        add(RestCatalog.outline());
        add(SpringSecurityCatalog.outline());
        add(KafkaCatalog.outline());
        add(PostgresqlCatalog.outline());
        add(MicroservicesCatalog.outline());
        add(KubernetesCatalog.outline());
        add(DockerCatalog.outline());
        add(SpringAiCatalog.outline());
        add(KeycloakCatalog.outline());
        add(RedisCatalog.outline());
        add(AwsSdkCatalog.outline());

        alias("java", "java");
        alias("jdk", "java");
        alias("openjdk", "java");
        alias("java 21", "java");
        alias("java 17", "java");
        alias("spring boot", "spring-boot");
        alias("springboot", "spring-boot");
        alias("spring framework", "spring-framework");
        alias("spring core", "spring-framework");
        alias("rest", "rest");
        alias("rest api", "rest");
        alias("restful", "rest");
        alias("web api", "rest");
        alias("spring security", "spring-security");
        alias("oauth", "spring-security");
        alias("oauth2", "spring-security");
        alias("kafka", "kafka");
        alias("apache kafka", "kafka");
        alias("postgresql", "postgresql");
        alias("postgres", "postgresql");
        alias("microservices", "microservices");
        alias("microservice", "microservices");
        alias("micro services", "microservices");
        alias("kubernetes", "kubernetes");
        alias("k8s", "kubernetes");
        alias("docker", "docker");
        alias("docker cli", "docker");
        alias("spring ai", "spring-ai");
        alias("springai", "spring-ai");
        alias("keycloak", "keycloak");
        alias("redis", "redis");
        alias("aws", "aws-sdk");
        alias("aws sdk", "aws-sdk");
        alias("amazon web services", "aws-sdk");
    }

    private SkillCurriculum() {
    }

    public static Outline outline(String key, String name, String summary, CoreConcept... concepts) {
        return new Outline(key, name, summary, List.of(concepts));
    }

    public static Optional<Outline> byKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static Optional<Outline> resolve(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return Optional.empty();
        }
        String raw = phrase.trim();
        Outline exact = BY_KEY.get(raw);
        if (exact != null) {
            return Optional.of(exact);
        }
        String normalized = normalize(raw);
        String aliased = ALIASES.get(normalized);
        if (aliased != null) {
            return byKey(aliased);
        }
        for (Outline outline : BY_KEY.values()) {
            if (normalize(outline.key()).equals(normalized) || normalize(outline.name()).equals(normalized)) {
                return Optional.of(outline);
            }
        }
        return Optional.empty();
    }

    public static List<Outline> all() {
        return List.copyOf(BY_KEY.values());
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void add(Outline outline) {
        BY_KEY.put(outline.key(), outline);
    }

    private static void alias(String phrase, String key) {
        ALIASES.put(normalize(phrase), key);
    }
}
