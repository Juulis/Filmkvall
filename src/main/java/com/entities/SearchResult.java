package com.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResult {
    @JsonProperty("Search")
    private MovieFromOMDB[] searchArray;

    public SearchResult() {

    }

    public MovieFromOMDB[] getSearchArray() {
        return searchArray;
    }

    public void setSearchArray(MovieFromOMDB[] searchArray) {
        this.searchArray = searchArray;
    }
}
