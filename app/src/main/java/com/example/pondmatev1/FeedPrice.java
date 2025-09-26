package com.example.pondmatev1;

public class FeedPrice {
    private int id;
    private String breed, feedType, price;

    public FeedPrice(int id, String breed, String feedType, String price) {
        this.id = id;
        this.breed = breed;
        this.feedType = feedType;
        this.price = price;
    }

    public int getId() { return id; }
    public String getBreed() { return breed; }
    public String getFeedType() { return feedType; }
    public String getPrice() { return price; }
}

