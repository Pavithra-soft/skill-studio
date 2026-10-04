package com.bharath.skillstudio.learn;

public class CodeSample {

    private String title = "";
    private String language = "java";
    private String code = "";
    private String notes = "";

    public CodeSample() {
    }

    public CodeSample(String title, String language, String code, String notes) {
        this.title = title == null ? "" : title;
        this.language = language == null || language.isBlank() ? "java" : language;
        this.code = code == null ? "" : code;
        this.notes = notes == null ? "" : notes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
