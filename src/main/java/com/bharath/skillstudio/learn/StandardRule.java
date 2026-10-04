package com.bharath.skillstudio.learn;

public class StandardRule {

    private String title = "";
    private String rule = "";
    private String why = "";

    public StandardRule() {
    }

    public StandardRule(String title, String rule, String why) {
        this.title = title == null ? "" : title;
        this.rule = rule == null ? "" : rule;
        this.why = why == null ? "" : why;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }
}
