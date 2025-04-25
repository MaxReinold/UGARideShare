package edu.uga.cs.ugarideshare;

import java.io.Serializable;

public class User implements Serializable {
    public String uid;
    public String email;

    public User() {} // Needed for Firebase
    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{uid='" + uid + "', email='" + email + "'}";
    }
}