package com.bharath.skillstudio.learn;

public class PatternRule {

    private String name = "";
    private String intent = "";
    private String how = "";

    public PatternRule() {
    }

    public PatternRule(String name, String intent, String how) {
        this.name = name == null ? "" : name;
        this.intent = intent == null ? "" : intent;
        this.how = how == null ? "" : how;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getHow() {
        return how;
    }

    public void setHow(String how) {
        this.how = how;
    }
}
