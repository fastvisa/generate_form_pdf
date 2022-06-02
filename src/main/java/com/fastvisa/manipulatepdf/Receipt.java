package com.fastvisa.manipulatepdf;

public class Receipt {
  private Object form_data;
  private String template_path;
  private String output_name;
  private String url_download;
  private String status;

  public Receipt() {
  }

  public Receipt(String output_name, String status) {
    this.output_name = output_name;
    this.status = status;
  }

  public Receipt(Object form_data, String output_name, String status) {
    this.form_data = form_data;
    this.output_name = output_name;
    this.status = status;
  }

  public Receipt(Object form_data, String output_name, String url_download, String status) {
    this.form_data = form_data;
    this.output_name = output_name;
    this.url_download = url_download;
    this.status = status;
  }

  public Object getForm_data() {
    return form_data;
  }

  public void setForm_data(Object form_data) {
    this.form_data = form_data;
  }

  public String getTemplate_path() {
    return template_path;
  }

  public void setTemplate_path(String template_path) {
    this.template_path = template_path;
  }

  public String getOutput_name() {
    return output_name;
  }

  public void setOutput_name(String output_name) {
    this.output_name = output_name;
  }

  public String getUrl_download() {
    return url_download;
  }

  public void setUrl_download(String url_download) {
    this.url_download = url_download;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
