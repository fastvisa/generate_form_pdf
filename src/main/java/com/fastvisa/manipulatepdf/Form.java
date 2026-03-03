package com.fastvisa.manipulatepdf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

public class Form {
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private final Object form_data;
  
  private final String pdf_template;
  
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private final Object custom_fields;
  
  private final String url_download;

  public Form(Object form_data, String pdf_template, Object custom_fields, String url_download) {
    this.form_data = form_data;
    this.pdf_template = pdf_template;
    this.custom_fields = custom_fields;
    this.url_download = url_download;
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