package com.bharath.skillstudio.learn;

import java.util.ArrayList;
import java.util.List;

public class CoreConcept {

    private String kind = "core";
    private String slug = "";
    private String title = "";
    private String why = "";
    private String example = "";
    private String useCase = "";
    private String depth = "";
    private List<InterviewCard> interviews = new ArrayList<>();
    private List<CodeSample> samples = new ArrayList<>();

    public CoreConcept() {
    }

    public CoreConcept(String title, String why, String useCase) {
        this("core", title, why, "", useCase);
    }

    public CoreConcept(String title, String why, String example, String useCase) {
        this("core", title, why, example, useCase);
    }

    public CoreConcept(String kind, String title, String why, String example, String useCase) {
        this.kind = kind == null || kind.isBlank() ? "core" : kind;
        this.title = title == null ? "" : title;
        this.slug = ConceptSlug.of(this.title);
        this.why = why == null ? "" : why;
        this.example = example == null ? "" : example;
        this.useCase = useCase == null ? "" : useCase;
    }

    public CoreConcept depth(String depth) {
        setDepth(depth);
        return this;
    }

    public CoreConcept qa(String question, String answer, String followUp) {
        interviews.add(new InterviewCard(question, answer, followUp));
        return this;
    }

    public CoreConcept sample(String title, String code, String notes) {
        String body = code == null ? "" : code.stripIndent();
        samples.add(new CodeSample(title, "java", body, notes));
        return this;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSlug() {
        return slug == null || slug.isBlank() ? ConceptSlug.of(title) : slug;
    }

    public void setSlug(String slug) {
        this.slug = slug == null ? "" : slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = ConceptSlug.of(title);
        }
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth == null ? "" : depth;
    }

    public List<InterviewCard> getInterviews() {
        return interviews;
    }

    public void setInterviews(List<InterviewCard> interviews) {
        this.interviews = interviews == null ? new ArrayList<>() : interviews;
    }

    public List<CodeSample> getSamples() {
        return samples;
    }

    public void setSamples(List<CodeSample> samples) {
        this.samples = samples == null ? new ArrayList<>() : samples;
    }
}
