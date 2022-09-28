package com.redis.brewdis.web;

public class Style {

	private String id;
	private String name;

	private Style(Builder builder) {
		this.id = builder.id;
		this.name = builder.name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String id;
		private String name;

		private Builder() {
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Style build() {
			return new Style(this);
		}
	}

}
