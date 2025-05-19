package com.example.crossbeat.model;

public class User{
    private String userName;
    final private String userID;

    public User(String userName, String userID){
        this.userID = userID;
        this.userName = userName;
    }

    public String getUserID() {
        return userID;
    }

    public String getUserName() {
        return userName;
    }
}
