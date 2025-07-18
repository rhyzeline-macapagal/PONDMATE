package com.example.pondmatev1;
public class Caretaker {
    private int id;
    private String username;
    private String password;
    private String fullname;
    private String address;

    public Caretaker(int id, String username, String password, String fullname, String address) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullname = fullname;
        this.address = address;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullname() { return fullname; }
    public String getAddress() { return address; }
}

