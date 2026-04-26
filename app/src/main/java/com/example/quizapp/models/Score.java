package com.example.quizapp.models;

/**
 * Score entry for the dashboard list.
 */
public class Score {

    private int id;
    private String quizType;
    private long score;
    private long totalQuestions;
    private String timestamp;
    private String userName;
    private String email;
    private String uid;

    // Required empty constructor for Firebase
    public Score() {
    }

    public Score(int id, String quizType, long score, long totalQuestions, String timestamp) {
        this.id = id;
        this.quizType = quizType;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timestamp = timestamp;
    }

    public Score(int id, String userName, String quizType, long score, long totalQuestions, String timestamp) {
        this.id = id;
        this.userName = userName;
        this.quizType = quizType;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getId() {
        return id;
    }

    public String getQuizType() {
        return quizType;
    }

    public long getScore() {
        return score;
    }

    public long getTotalQuestions() {
        return totalQuestions;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setQuizType(String quizType) {
        this.quizType = quizType;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public void setTimestamp(Object timestamp) {
        if (timestamp instanceof Long) {
            this.timestamp = String.valueOf(timestamp);
        } else if (timestamp instanceof String) {
            this.timestamp = (String) timestamp;
        }
    }
    
    public int getPercentage() {
        if (totalQuestions == 0) return 0;
        return (int) (((double) score / totalQuestions) * 100);
    }
}
