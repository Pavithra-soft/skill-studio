package com.bharath.skillstudio.learn;

public class InterviewCard {

    private String question = "";
    private String answer = "";
    private String followUp = "";

    public InterviewCard() {
    }

    public InterviewCard(String question, String answer, String followUp) {
        this.question = question == null ? "" : question;
        this.answer = answer == null ? "" : answer;
        this.followUp = followUp == null ? "" : followUp;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getFollowUp() {
        return followUp;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }
}
