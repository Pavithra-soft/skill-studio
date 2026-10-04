package com.bharath.skillstudio.ai;

import java.util.ArrayList;
import java.util.List;

public class ChatReply {

    private String answer = "";
    private String voice = "";
    private boolean llm;
    private String provider = "catalog";
    private List<String> sources = new ArrayList<>();

    public ChatReply() {
    }

    public ChatReply(String answer, boolean llm, String provider, List<String> sources) {
        this.answer = answer == null ? "" : answer;
        this.voice = this.answer;
        this.llm = llm;
        this.provider = provider == null ? "catalog" : provider;
        this.sources = sources == null ? new ArrayList<>() : sources;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer == null ? "" : answer;
    }

    public String getVoice() {
        return voice == null || voice.isBlank() ? answer : voice;
    }

    public void setVoice(String voice) {
        this.voice = voice == null ? "" : voice;
    }

    public boolean isLlm() {
        return llm;
    }

    public void setLlm(boolean llm) {
        this.llm = llm;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider == null ? "catalog" : provider;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }
}
