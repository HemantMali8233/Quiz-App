package com.example.quizapp.models;

import java.io.Serializable;

/**
 * Question model for both quiz categories.
 */
public class Question implements Serializable {

    private String quizType; // "Management" or "ETI"
    private String co; // Course Outcome (CO1 to CO5)
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private int correctIndex; // 1-4

    public Question() {} // Required for Firestore

    public Question(String quizType, String co, String question,
                    String option1, String option2,
                    String option3, String option4,
                    int correctIndex) {
        this.quizType = quizType;
        this.co = co;
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.correctIndex = correctIndex;
    }

    public String getQuizType() {
        return quizType;
    }

    public String getCo() {
        return co;
    }

    public String getQuestion() {
        return question;
    }

    public String getOption1() {
        return option1;
    }

    public String getOption2() {
        return option2;
    }

    public String getOption3() {
        return option3;
    }

    public String getOption4() {
        return option4;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public void setQuizType(String quizType) {
        this.quizType = quizType;
    }

    public void setCo(String co) {
        this.co = co;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public void setCorrectIndex(int correctIndex) {
        this.correctIndex = correctIndex;
    }
}
