package com.fastvisa.manipulatepdf;

public class Form {
  
  private final String form_data;
  private final String template_path;
  private final String output_name;
  private final String status;

  public Form(String form_data, String template_path, String output_name, String status) {
    this.form_data = form_data;
    this.template_path = template_path;
    this.output_name = output_name;
    this.status = status;
  }

  public String formData() {
    return form_data;
  }

  public String templatePath() {
    return template_path;
  }

  public String outputName() {
    return output_name;
  }

  //response json
  public String getOutput() {
    return output_name;
  }

  public String getStatus() {
    return status;
  }

}