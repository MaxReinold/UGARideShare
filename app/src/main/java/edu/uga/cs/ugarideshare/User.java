package edu.uga.cs.ugarideshare;

import java.io.Serializable;

public class User implements Serializable {
    public String uid;
    public String email;

    /**
     * Default constructor.
     */
    public User() {}

    /**
     * Constructs a User object.
     * @param uid The user ID.
     * @param email The user email.
     */
    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
    }

    /**
     * Returns a string representation of the User.
     * @return String representation.
     */
    @Override
    public String toString() {
        return "User{uid='" + uid + "', email='" + email + "'}";
    }
}