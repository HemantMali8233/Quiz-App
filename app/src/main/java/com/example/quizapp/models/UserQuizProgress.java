package com.example.quizapp.models;

public class UserQuizProgress {
    private int userId;
    private String subject;
    private String co;
    private int lastQuestionIndex; // Points to the last completed question (e.g., 10, 20)
    private long lastAttemptDate; // Timestamp of last attempt

    public UserQuizProgress() {}

    public UserQuizProgress(int userId, String subject, String co, int lastQuestionIndex, long lastAttemptDate) {
        this.userId = userId;
        this.subject = subject;
        this.co = co;
        this.lastQuestionIndex = lastQuestionIndex;
        this.lastAttemptDate = lastAttemptDate;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public String getSubject() { return subject; }
    public String getCo() { return co; }
    public int getLastQuestionIndex() { return lastQuestionIndex; }
    public void setLastQuestionIndex(int lastQuestionIndex) { this.lastQuestionIndex = lastQuestionIndex; }
    public long getLastAttemptDate() { return lastAttemptDate; }
    public void setLastAttemptDate(long lastAttemptDate) { this.lastAttemptDate = lastAttemptDate; }
}