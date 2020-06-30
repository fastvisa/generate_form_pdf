package com.generatepdf.restservice;

public class Request {

	private final String path;
	private final String name;

	public Request(String path, String name) {
		this.path = path;
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
	}
}