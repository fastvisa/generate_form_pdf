package com.fastvisa.manipulatepdf;

import java.io.File;
import java.util.Date;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.JSONArray;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.fastvisa.services.AwsS3Service;
import com.fastvisa.services.FormService;

@RestController
public class FormsController {
  private String url_download;    
  private Gson gson = new Gson();

  @PostMapping(path = "/api/v1/fillform", consumes = "application/json", produces = "application/json")
  public Form fillform(@RequestBody String bodyParameter) throws Exception {
    FormService formService = new FormService();
    JSONArray form_array = new JSONArray();

    Form g = gson.fromJson(bodyParameter, Form.class);

    Object form_data = g.formData();
    String template_path = g.templatePath();
    String output_name = g.outputName();
    
    form_array = formService.getFormArray(form_data);

    File file = File.createTempFile(output_name, "pdf");

    formService.fillForm(form_array, template_path, file, output_name);

    uploadS3(file, output_name);

    return new Form(new Object(), form_data, template_path, output_name, url_download, "success");
  }

  @PostMapping(path = "/api/v1/combineform", consumes = "application/json", produces = "application/json")
  public Form combineform(@RequestBody String bodyParameter) throws Exception {
    JSONArray pdf_array = new JSONArray();
    FormService formService = new FormService();
    Date date = new Date();
    String combined_file_name = "combined-pdf-" + date.getTime();
    
    Form g = gson.fromJson(bodyParameter, Form.class);
    Object pdf_data = g.pdfData();
    pdf_array = formService.getFormArray(pdf_data);
    
    File combined_file = File.createTempFile(combined_file_name, "pdf");

    formService.combineForm(combined_file, pdf_array);

    uploadS3(combined_file, combined_file_name);

    return new Form(new Object(), new Object(), "", "", url_download, "success");
  }

  @Value("${aws.accessKey}")
  private String accessKey;
  @Value("${aws.secretKey}")
  private String secretKey;
  @Value("${aws.s3bucket.name}")
  private String bucketName;
  @Value("${aws.s3bucket.region}")
  private String region;

  private void uploadS3(File file, String output_name) {
    try {
      AwsS3Service s3Service = new AwsS3Service(accessKey, secretKey, region);
      s3Service.postObject(bucketName, output_name + ".pdf", file);
      String url_download = s3Service.getUrl(bucketName, output_name + ".pdf");

      file.delete();

      this.url_download = url_download;
    } catch (AmazonServiceException e) {
      e.printStackTrace();
    } catch (SdkClientException e) {
      e.printStackTrace();
    }
  }
  
}