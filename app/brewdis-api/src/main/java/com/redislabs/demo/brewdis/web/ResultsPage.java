package com.redislabs.demo.brewdis.web;

import com.redis.lettucemod.api.search.SearchResults;
import lombok.Data;

@Data
public class ResultsPage {
    private long count;
    private SearchResults<String, String> results;
    private float duration;
    private long pageIndex;
    private long pageSize;
}
