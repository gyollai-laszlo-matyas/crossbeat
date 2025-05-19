package com.example.crossbeat.model;

public class Score {
    private int difficulty;
    private int score;
    final private String userID;

    public Score(int difficulty, int score, String userID){
        this.difficulty = difficulty;
        this.score = score;
        this.userID = userID;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getScore() {
        return score;
    }

    public String getUserID() {
        return userID;
    }
}
