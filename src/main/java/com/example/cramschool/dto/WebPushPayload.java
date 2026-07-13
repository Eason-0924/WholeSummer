package com.example.cramschool.dto;

public class WebPushPayload {

	private String title;
	private String body;
	private String url;
	private String icon;

	public WebPushPayload() {
	}

	public WebPushPayload(String title, String body, String url, String icon) {
		this.title = title;
		this.body = body;
		this.url = url;
		this.icon = icon;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}
