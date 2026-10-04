package com.bharath.skillstudio.ai;

import com.bharath.skillstudio.learn.CodeSample;
import com.bharath.skillstudio.learn.InterviewCard;

import java.util.ArrayList;
import java.util.List;

public class GeneratedConcept {

    private String title = "";
    private String why = "";
    private String depth = "";
    private String trap = "";
    private String takeaway = "";
    private String useCase = "";
    private String example = "";
    private List<String> points = new ArrayList<>();
    private List<InterviewCard> interviews = new ArrayList<>();
    private List<CodeSample> samples = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getTrap() {
        return trap;
    }

    public void setTrap(String trap) {
        this.trap = trap;
    }

    public String getTakeaway() {
        return takeaway;
    }

    public void setTakeaway(String takeaway) {
        this.takeaway = takeaway;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public List<String> getPoints() {
        return points;
    }

    public void setPoints(List<String> points) {
        this.points = points == null ? new ArrayList<>() : points;
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
