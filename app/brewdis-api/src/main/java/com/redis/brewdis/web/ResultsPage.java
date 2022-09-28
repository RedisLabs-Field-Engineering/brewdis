package com.redis.brewdis.web;

import com.redis.lettucemod.search.SearchResults;

public class ResultsPage {
	private long count;
	private SearchResults<String, String> results;
	private float duration;
	private long pageIndex;
	private long pageSize;

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public SearchResults<String, String> getResults() {
		return results;
	}

	public void setResults(SearchResults<String, String> results) {
		this.results = results;
	}

	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	public long getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(long pageIndex) {
		this.pageIndex = pageIndex;
	}

	public long getPageSize() {
		return pageSize;
	}

	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}

}
