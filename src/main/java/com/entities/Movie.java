package com.entities;

public class Movie {
    private String title;
    private String released;
    private String genre;
    private String id;
    private String descr;
    private String poster;

    public Movie(String title, String released, String genre, String id, String descr, String poster) {
        this.title = title;
        this.released = released;
        this.genre = genre;
        this.id = id;
        this.descr = descr;
        this.poster = poster;
    }

    public Movie() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReleased() {
        return released;
    }

    public void setReleased(String released) {
        this.released = released;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }
}
