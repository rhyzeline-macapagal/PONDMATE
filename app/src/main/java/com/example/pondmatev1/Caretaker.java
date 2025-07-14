package com.example.pondmatev1;

public class Caretaker {
    private String fullName;
    private String address;
    private String username;
    private String password;

    public Caretaker(String fullName, String address, String username, String password) {
        this.fullName = fullName;
        this.address = address;
        this.username = username;
        this.password = password;
    }

    public String getFullName() { return fullName; }
    public String getAddress() { return address; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
