package com.redis.brewdis.web;

public class Query {

	public static final String SORT_ASC = "Ascending";

	private String query = "*";
	private String sortByField;
	private String sortByDirection = SORT_ASC;
	private long pageIndex = 0;
	private long pageSize = 100;

	public long getOffset() {
		return pageIndex * pageSize;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getSortByField() {
		return sortByField;
	}

	public void setSortByField(String sortByField) {
		this.sortByField = sortByField;
	}

	public String getSortByDirection() {
		return sortByDirection;
	}

	public void setSortByDirection(String sortByDirection) {
		this.sortByDirection = sortByDirection;
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
