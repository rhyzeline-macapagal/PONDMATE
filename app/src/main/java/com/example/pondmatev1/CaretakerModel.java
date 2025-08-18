package com.example.pondmatev1;

public class CaretakerModel {
    private int id;           // unique id from DB
    private String username;
    private String password;  // password included
    private String fullname;
    private String address;
    private String usertype;  // e.g. "Caretaker"

    // Empty constructor
    public CaretakerModel() {
    }

    public CaretakerModel(String username, String password, String fullname, String address, String usertype) {
        this.username = username;
        this.password = password;
        this.fullname = fullname;
        this.address = address;
        this.usertype = usertype;
    }

    // Getters


    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullname() {
        return fullname;
    }

    public String getAddress() {
        return address;
    }

    public String getUsertype() {
        return usertype;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setUsertype(String usertype) {
        this.usertype = usertype;
    }
}
