package com.dancetracker;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordTest {

    public static void main(String[] args) {

        String password = "Dance123!";

        String hashedPassword =
                BCrypt.hashpw(password, BCrypt.gensalt(12));

        System.out.println("Original password: " + password);
        System.out.println("Hashed password: " + hashedPassword);

        boolean correct =
                BCrypt.checkpw(password, hashedPassword);

        System.out.println("Password matches: " + correct);
    }
}