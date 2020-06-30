package com.example.restservice;

public class Greeting {

	private final long id;
	private final String content;
	private final int nomer;

	public Greeting(long id, String content, int nomer) {
		this.id = id;
		this.content = content;
		this.nomer = nomer;
	}

	public int getNomer() {
		return nomer;
	}

	public long getId() {
		return id;
	}

	public String getContent() {
		return content;
	}
}