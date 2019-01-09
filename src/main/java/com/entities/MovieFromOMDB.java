package com.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovieFromOMDB extends Movie {
    @JsonProperty("Title")
    private String title;
    @JsonProperty("Year")
    private String released;
    @JsonProperty("Genre")
    private String genre;
    @JsonProperty("imdbID")
    private String id;
    @JsonProperty("Plot")
    private String descr;
    @JsonProperty("Poster")
    private String poster;
}
