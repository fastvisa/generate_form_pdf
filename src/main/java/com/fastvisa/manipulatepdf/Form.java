package com.fastvisa.manipulatepdf;

public class Form {
  
  private final Object pdf_data;
  private final Object form_data;
  private final String pdf_template;
  private final String url_download;

  public Form(Object pdf_data, Object form_data, String pdf_template, String url_download) {
    this.pdf_data = pdf_data;
    this.form_data = form_data;
    this.pdf_template = pdf_template;
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

  //response json
  public String getUrl() {
    return url_download;
  }

}