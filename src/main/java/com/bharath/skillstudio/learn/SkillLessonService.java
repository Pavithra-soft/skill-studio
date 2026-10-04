package com.bharath.skillstudio.learn;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SkillLessonService {

    public static final int DEFAULT_PAGE_SIZE = 4;
    private static final String[] STEPS = {"First up", "Next", "Then", "After that", "Finally"};

    private final Map<String, SkillLesson> generated = new ConcurrentHashMap<>();

    public List<SkillButton> listSkills() {
        Map<String, SkillButton> buttons = new LinkedHashMap<>();
        for (SkillCurriculum.Outline outline : SkillCurriculum.all()) {
            buttons.put(outline.key(), new SkillButton(outline.key(), outline.name(), "catalog"));
        }
        for (SkillLesson lesson : generated.values()) {
            buttons.putIfAbsent(lesson.getKey(), new SkillButton(lesson.getKey(), lesson.getName(), "custom"));
        }
        return new ArrayList<>(buttons.values());
    }

    public SkillLesson lesson(String skill) {
        return lesson(skill, null, 0, DEFAULT_PAGE_SIZE);
    }

    public SkillLesson lesson(String skill, String concept, Integer page, Integer size) {
        SkillLesson lesson = loadLesson(skill);
        return paginate(lesson, concept, page, size);
    }

    public List<SkillLesson> studio(List<String> skills, List<String> concepts) {
        List<String> skillKeys = skills == null ? List.of() : skills.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
        if (skillKeys.isEmpty()) {
            throw new IllegalArgumentException("Pick at least one skill for the studio.");
        }
        Set<String> wanted = new LinkedHashSet<>();
        if (concepts != null) {
            for (String concept : concepts) {
                if (concept != null && !concept.isBlank()) {
                    wanted.add(ConceptSlug.of(concept));
                    String raw = concept.trim();
                    int slash = raw.lastIndexOf('/');
                    if (slash > 0 && slash < raw.length() - 1) {
                        wanted.add(ConceptSlug.of(raw.substring(slash + 1)));
                    }
                }
            }
        }
        List<SkillLesson> lessons = new ArrayList<>();
        for (String skill : skillKeys) {
            SkillLesson lesson = loadLesson(skill);
            if (!wanted.isEmpty()) {
                List<CoreConcept> matched = new ArrayList<>();
                for (CoreConcept concept : lesson.getConcepts()) {
                    String slug = concept.getSlug();
                    if (wanted.contains(slug) || wanted.contains(lesson.getKey() + "-" + slug)) {
                        matched.add(concept);
                    }
                }
                if (matched.isEmpty()) {
                    continue;
                }
                lesson.setConcepts(matched);
                lesson.setVoice(voiceFor(lesson));
            }
            lessons.add(lesson);
        }
        if (lessons.isEmpty()) {
            throw new IllegalArgumentException("Those skills and concepts did not match a huddle.");
        }
        return lessons;
    }

    public byte[] projectZip(String skillsCsv, String conceptsCsv) {
        return StudioProjectWriter.zip(studio(csv(skillsCsv), csv(conceptsCsv)));
    }

    public SkillLesson generate(String skill) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("Type a skill to generate a lesson.");
        }
        Optional<SkillCurriculum.Outline> outline = SkillCurriculum.resolve(skill);
        if (outline.isPresent()) {
            return paginate(assemble(outline.get()), null, 0, DEFAULT_PAGE_SIZE);
        }
        String key = customKey(skill);
        SkillLesson cached = generated.get(key);
        if (cached != null) {
            return paginate(copyLesson(cached), null, 0, DEFAULT_PAGE_SIZE);
        }
        SkillLesson lesson = genericLesson(skill);
        lesson.setKey(key);
        lesson.setName(skill.trim());
        lesson.setStandards(new ArrayList<>(SkillPlaybook.standards(key)));
        lesson.setPatterns(new ArrayList<>(SkillPlaybook.patterns(key)));
        lesson.setVoice(voiceFor(lesson));
        generated.put(key, copyLesson(lesson));
        return paginate(lesson, null, 0, DEFAULT_PAGE_SIZE);
    }

    private SkillLesson loadLesson(String skill) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("Pick a skill first.");
        }
        Optional<SkillCurriculum.Outline> outline = SkillCurriculum.byKey(skill)
                .or(() -> SkillCurriculum.resolve(skill));
        if (outline.isPresent()) {
            return assemble(outline.get());
        }
        String key = skill.startsWith("custom:") ? skill : customKey(skill);
        SkillLesson cached = generated.get(key);
        if (cached != null) {
            return copyLesson(cached);
        }
        return generateFresh(displayName(skill));
    }

    private SkillLesson generateFresh(String skill) {
        String key = customKey(skill);
        SkillLesson lesson = genericLesson(skill);
        lesson.setKey(key);
        lesson.setName(skill.trim());
        lesson.setStandards(new ArrayList<>(SkillPlaybook.standards(key)));
        lesson.setPatterns(new ArrayList<>(SkillPlaybook.patterns(key)));
        lesson.setVoice(voiceFor(lesson));
        generated.put(key, copyLesson(lesson));
        return lesson;
    }

    private SkillLesson assemble(SkillCurriculum.Outline outline) {
        SkillLesson lesson = new SkillLesson();
        lesson.setKey(outline.key());
        lesson.setName(outline.name());
        lesson.setSummary(outline.summary());
        lesson.setConcepts(new ArrayList<>(outline.concepts()));
        lesson.setStandards(new ArrayList<>(SkillPlaybook.standards(outline.key())));
        lesson.setPatterns(new ArrayList<>(SkillPlaybook.patterns(outline.key())));
        lesson.setVoice(voiceFor(lesson));
        return lesson;
    }

    static SkillLesson paginate(SkillLesson lesson, String concept, Integer page, Integer size) {
        List<CoreConcept> all = lesson.getConcepts() == null ? List.of() : List.copyOf(lesson.getConcepts());
        List<ConceptRef> catalog = new ArrayList<>();
        for (CoreConcept item : all) {
            if (item == null) {
                continue;
            }
            catalog.add(new ConceptRef(item.getSlug(), item.getTitle()));
        }
        lesson.setCatalog(catalog);
        lesson.setTotalConcepts(catalog.size());

        if (concept != null && !concept.isBlank() && !"all".equalsIgnoreCase(concept.trim())) {
            String slug = ConceptSlug.of(concept);
            List<CoreConcept> matched = new ArrayList<>();
            for (CoreConcept item : all) {
                if (item == null) {
                    continue;
                }
                if (slug.equals(item.getSlug()) || slug.equals(ConceptSlug.of(item.getTitle()))) {
                    matched.add(item);
                }
            }
            if (matched.isEmpty()) {
                throw new IllegalArgumentException("No concept '" + concept + "' on " + lesson.getName() + ".");
            }
            lesson.setConcepts(matched);
            lesson.setPage(0);
            lesson.setSize(1);
            lesson.setTotalPages(1);
            lesson.setVoice(voiceFor(lesson));
            return lesson;
        }

        int pageSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : size;
        int totalPages = catalog.isEmpty() ? 1 : (int) Math.ceil(catalog.size() / (double) pageSize);
        int requested = page == null ? 0 : page;
        int clamped = Math.max(0, Math.min(requested, totalPages - 1));
        int from = Math.min(clamped * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        lesson.setConcepts(new ArrayList<>(all.subList(from, to)));
        lesson.setPage(clamped);
        lesson.setSize(pageSize);
        lesson.setTotalPages(totalPages);
        lesson.setVoice(voiceFor(lesson));
        return lesson;
    }

    static String voiceFor(SkillLesson lesson) {
        StringBuilder out = new StringBuilder();
        out.append("Alright. This is a working session on ").append(lesson.getName()).append(". ");
        List<CoreConcept> concepts = lesson.getConcepts() == null ? List.of() : lesson.getConcepts();
        int conceptCount = concepts.size();
        if (conceptCount == 1) {
            out.append("We are zooming into one concept: depth, interview questions, and programming examples on the screen. ");
        } else {
            out.append("I will cover the ideas you actually use, a production story for each. ");
        }
        out.append("I will not read code or commands. Those stay on the screen. ");
        if (lesson.getSummary() != null && !lesson.getSummary().isBlank()) {
            out.append(lesson.getSummary()).append(' ');
        }
        if (lesson.getStandards() != null && !lesson.getStandards().isEmpty()) {
            out.append("Coding standards are on the Standards tab. ");
            int count = 0;
            for (StandardRule rule : lesson.getStandards()) {
                if (rule == null || rule.getTitle() == null || rule.getTitle().isBlank()) {
                    continue;
                }
                out.append(rule.getTitle()).append(". ");
                if (rule.getRule() != null && !rule.getRule().isBlank()) {
                    out.append(rule.getRule()).append(' ');
                }
                count++;
                if (count == 3) {
                    break;
                }
            }
        }
        if (lesson.getPatterns() != null && !lesson.getPatterns().isEmpty()) {
            out.append("Design patterns are on the Patterns tab. ");
            int count = 0;
            for (PatternRule pattern : lesson.getPatterns()) {
                if (pattern == null || pattern.getName() == null || pattern.getName().isBlank()) {
                    continue;
                }
                out.append(pattern.getName()).append(". ");
                if (pattern.getIntent() != null && !pattern.getIntent().isBlank()) {
                    out.append(pattern.getIntent()).append(' ');
                }
                count++;
                if (count == 2) {
                    break;
                }
            }
        }
        int index = 0;
        int maxConcepts = conceptCount <= 2 ? 8 : 5;
        for (CoreConcept concept : concepts) {
            if (concept == null || concept.getTitle() == null || concept.getTitle().isBlank()) {
                continue;
            }
            out.append(STEPS[Math.min(index, STEPS.length - 1)]).append(". ");
            out.append(concept.getTitle()).append(". ");
            if (concept.getDepth() != null && !concept.getDepth().isBlank()) {
                out.append(concept.getDepth()).append(' ');
            } else if (concept.getWhy() != null && !concept.getWhy().isBlank()) {
                out.append(concept.getWhy()).append(' ');
            }
            if (concept.getUseCase() != null && !concept.getUseCase().isBlank()) {
                out.append("In production. ").append(concept.getUseCase()).append(' ');
            }
            int asked = 0;
            if (concept.getInterviews() != null) {
                for (InterviewCard card : concept.getInterviews()) {
                    if (card == null || card.getQuestion() == null || card.getQuestion().isBlank()) {
                        continue;
                    }
                    out.append("Interview question. ").append(card.getQuestion()).append(' ');
                    if (card.getAnswer() != null && !card.getAnswer().isBlank()) {
                        out.append(card.getAnswer()).append(' ');
                    }
                    asked++;
                    if (asked == 2) {
                        break;
                    }
                }
            }
            boolean hasSample = (concept.getExample() != null && !concept.getExample().isBlank())
                    || (concept.getSamples() != null && !concept.getSamples().isEmpty());
            if (hasSample) {
                out.append("There are programming examples on the screen. Glance at them. I will not read the code. ");
            }
            index++;
            if (index == maxConcepts) {
                break;
            }
        }
        out.append("That is the session. Pause me any time.");
        return SpokenScript.sanitize(out.toString());
    }

    private SkillLesson genericLesson(String skill) {
        String name = skill.trim();
        SkillLesson lesson = new SkillLesson();
        lesson.setSummary("This is a working huddle on " + name
                + ". I will cover how teams actually use it, where it sits in a stack, and the trap that shows up in production.");
        lesson.setConcepts(List.of(
                new CoreConcept("Where it sits",
                        name + " earns its keep when it replaces a worse workaround, not when it is a line on a resume.",
                        "Ask which problem disappeared after the team adopted it. If nobody can say, it is fashion.")
                        .depth("""
                                A tool is a boundary. It sits between two pains: the one it removes and the one it introduces. Start the huddle by naming both. If you cannot name the second pain — operational cost, a new failure mode, a skill the on-call now needs — you have not used it in production.

                                Interviewers for senior Java roles rarely want a feature list. They want the default production shape: how 80 percent of teams run it, what you configure on day one, and what you refuse to do with it. Sketch the boring path before the clever path.

                                Then pin it to this job search. A posting that lists %s wants a story with a metric, not a keyword. Keep one incident or a near-miss ready: timeouts, poison input, a missing boundary, a leaked secret.
                                """.formatted(name))
                        .qa("Why does " + name + " belong in a stack?",
                                "Because it deleted a class of incidents or a class of toil. If the only answer is 'the market uses it', it is fashion.",
                                "What did the team stop doing after they adopted it?")
                        .qa("Where would you refuse to use " + name + "?",
                                "Anywhere the failure mode is worse than the problem. Durability, tenancy, and on-call skill are the usual vetoes.",
                                "Name the operational cost."),
                new CoreConcept("The default production shape",
                        "Every widely used tool has a boring default: one way most teams run it. Learn that first.",
                        "In reviews, describe the default path before the clever path.")
                        .depth("""
                                Defaults are a contract. Connection pools, timeouts, ack modes, eviction, probe paths — the numbers a staff engineer can recite without opening the docs. Clever configuration is how you explain an outage.

                                Draw the box: who writes, who reads, what is stored, what is lost on crash. If you cannot draw it, you cannot size it. Size the dependency for the downstream, not for the thread count in your JVM.

                                Tests should hit the default. A suite that only exercises the exotic mode will green while production uses the boring path you never proved.
                                """)
                        .qa("What is the boring default for " + name + "?",
                                "The configuration 80 percent of production systems share, including timeouts and a bulkhead.",
                                "What happens if you leave the default timeout?")
                        .qa("How do you prove the default in a review?",
                                "A diagram, a metric, and a test that fails when the default is wrong. Not a blog diagram.",
                                "Which metric moved?"),
                new CoreConcept("The failure mode",
                        "The interview and the outage are the same story: timeouts, poison input, or a missing boundary.",
                        "Name one incident pattern: retries that amplify load, a poison message, or a leaked secret.")
                        .depth("""
                                Failure is not an exception message. It is amplification: retries that DDOS your own dependency, a poison record that stalls a partition, a lock that never expires, a token that landed in logs.

                                Senior answers name the blast radius and the brake. Timeouts, bulkheads, dead-letter, circuit breakers, and idempotency keys are brakes. If your answer is 'we would scale the pod', you have not named the brake.

                                Practice saying what you still do not know. Interviewers trust the person who can bound an incident faster than the person who claims the tool never fails.
                                """)
                        .qa("What does an outage in " + name + " look like?",
                                "Usually a timeout, a stuck consumer, or a stampeding retry. Describe the brake you would reach for first.",
                                "How would you find it in logs?")
                        .qa("How do retries make it worse?",
                                "They multiply load on a sick dependency. Cap them, backoff, and shed. Infinite retry is an outage generator.",
                                "Where is the dead letter?"),
                new CoreConcept("How you talk about it in an interview",
                        "Postings that list " + name + " want evidence, not a keyword. A story beats a bullet.",
                        "Keep one STAR story ready: situation, the tool, the measurable outcome.")
                        .depth("""
                                STAR is not a script. Situation in one sentence, the constraint, the change you made, the number. If you cannot name a number — latency, error rate, cost, time to restore — pick a smaller story.

                                Do not invent APIs. If you have not shipped %s, say how you would introduce it: the default shape, the probe, the rollback. Honesty at this level is a senior signal.

                                Close with the standard you would encode in a new repo: constructor injection, timeouts, no secrets in images. That is the same standard this studio writes into a zip.
                                """.formatted(name))
                        .qa("Give a STAR story for " + name + ".",
                                "Name the incident, the change, and the metric. Keep it under a minute.",
                                "What would you do differently now?")
                        .qa("What would you put in the generated studio for this skill?",
                                "A coding standard, a pattern, and one demo class that cannot compile a lie. Examples stay on screen.",
                                "Which pattern is the default?")
        ));
        return lesson;
    }

    static List<String> csv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    static String customKey(String skill) {
        return "custom:" + SkillCurriculum.normalize(skill);
    }

    private static String displayName(String skill) {
        if (skill != null && skill.startsWith("custom:")) {
            return skill.substring("custom:".length());
        }
        return skill;
    }

    private static SkillLesson copyLesson(SkillLesson source) {
        SkillLesson copy = new SkillLesson();
        copy.setKey(source.getKey());
        copy.setName(source.getName());
        copy.setSummary(source.getSummary());
        copy.setVoice(source.getVoice());
        copy.setConcepts(new ArrayList<>(source.getConcepts()));
        copy.setStandards(new ArrayList<>(source.getStandards()));
        copy.setPatterns(new ArrayList<>(source.getPatterns()));
        copy.setCatalog(new ArrayList<>(source.getCatalog()));
        copy.setPage(source.getPage());
        copy.setSize(source.getSize());
        copy.setTotalConcepts(source.getTotalConcepts());
        copy.setTotalPages(source.getTotalPages());
        return copy;
    }

    public record SkillButton(String key, String name, String kind) {
    }
}
