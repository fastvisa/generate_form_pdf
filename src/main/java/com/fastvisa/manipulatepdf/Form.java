package com.fastvisa.manipulatepdf;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Form {
  
  private final Object pdf_data;
  private final Object form_data;
  private final String pdf_template;
  private final Object custom_fields;
  private final String url_download;

  public Form(Object pdf_data, Object form_data, String pdf_template, Object custom_fields, String url_download) {
    this.pdf_data = pdf_data;
    this.form_data = form_data;
    this.pdf_template = pdf_template;
    this.custom_fields = custom_fields;
    this.url_download = url_download;
  }

  public Object pdfData() {
    return pdf_data;
  }

  public Object formData() {
    return form_data;
  }

  public String templatePath() {
    return pdf_template;
  }

  @JsonProperty("custom_fields")
  public Object customFields() {
    return custom_fields;
  }

  //response json
  public String getUrl() {
    return url_download;
  }

}