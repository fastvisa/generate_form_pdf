package com.fastvisa.manipulatepdf;

public class Request {

	private final String name;
	private final String form_data;
	private final String template_path;

	public Request(String name, String form_data, String template_path) {
		this.name = name;
		this.form_data = form_data;
		this.template_path = template_path;
	}

	public String getName() {
		return name;
	}

	public String getFormData() {
		return form_data;
	}

	public String getTemplatePath() {
		return template_path;
	}
}