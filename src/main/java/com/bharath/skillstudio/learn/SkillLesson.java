package com.bharath.skillstudio.learn;

import java.util.ArrayList;
import java.util.List;

public class SkillLesson {

    private String key = "";
    private String name = "";
    private String summary = "";
    private String voice = "";
    private int page;
    private int size;
    private int totalConcepts;
    private int totalPages;
    private List<ConceptRef> catalog = new ArrayList<>();
    private List<CoreConcept> concepts = new ArrayList<>();
    private List<StandardRule> standards = new ArrayList<>();
    private List<PatternRule> patterns = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key == null ? "" : key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary == null ? "" : summary;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice == null ? "" : voice;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalConcepts() {
        return totalConcepts;
    }

    public void setTotalConcepts(int totalConcepts) {
        this.totalConcepts = totalConcepts;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<ConceptRef> getCatalog() {
        return catalog;
    }

    public void setCatalog(List<ConceptRef> catalog) {
        this.catalog = catalog == null ? new ArrayList<>() : catalog;
    }

    public List<CoreConcept> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<CoreConcept> concepts) {
        this.concepts = concepts == null ? new ArrayList<>() : concepts;
    }

    public List<StandardRule> getStandards() {
        return standards;
    }

    public void setStandards(List<StandardRule> standards) {
        this.standards = standards == null ? new ArrayList<>() : standards;
    }

    public List<PatternRule> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<PatternRule> patterns) {
        this.patterns = patterns == null ? new ArrayList<>() : patterns;
    }
}
