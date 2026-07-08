package com.zik00.shop.dto;

public class MypageMenuItem {
    private final String title;
    private final String description;
    private final String url;
    private final boolean active;

    public MypageMenuItem(String title, String description, String url, boolean active) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.active = active;
    }

    public String getTitle() {
        return title;
    }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public boolean isActive() {
        return active;
    }
}
