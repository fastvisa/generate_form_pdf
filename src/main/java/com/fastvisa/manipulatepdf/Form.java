package com.fastvisa.manipulatepdf;

public class Form {
  
  private final Object pdf_data;
  private final Object form_data;
  private final String template_path;
  private final String output_name;
  private final String url_download;
  private final String status;

  public Form(Object pdf_data, Object form_data, String template_path, String output_name, String url_download, String status) {
    this.pdf_data = pdf_data;
    this.form_data = form_data;
    this.template_path = template_path;
    this.output_name = output_name;
    this.url_download = url_download;
    this.status = status;
  }

  public Object pdfData() {
    return pdf_data;
  }

  public Object formData() {
    return form_data;
  }

  public String templatePath() {
    return template_path;
  }

  public String outputName() {
    return output_name;
  }

  //response json
  public String getUrl() {
    return url_download;
  }

  public String getStatus() {
    return status;
  }

}