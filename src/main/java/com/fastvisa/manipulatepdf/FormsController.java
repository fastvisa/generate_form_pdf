package com.fastvisa.manipulatepdf;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.fastvisa.services.AwsS3Service;
import com.fastvisa.services.FormService;
import com.fastvisa.services.ReceiptService;

@RestController
public class FormsController {
  private String url_download;
  private Gson gson = new Gson();

  @Value("${project.version:0.0.1-SNAPSHOT}")
  private String projectVersion;

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> healthStatus = new HashMap<>();
    healthStatus.put("status", "UP");
    healthStatus.put("version", projectVersion);
    healthStatus.put("service", "manipulate-pdf");
    healthStatus.put("timestamp", System.currentTimeMillis());
    
    return ResponseEntity.ok(healthStatus);
  }

  @PostMapping(path = "/api/v1/fillform", consumes = "application/json", produces = "application/json")
  public Form fillform(@RequestBody String bodyParameter) throws Exception {
    FormService formService = new FormService();
    JSONArray form_array = new JSONArray();
    JSONArray structure_input_array = new JSONArray();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    JsonObject convertedObject = gson.fromJson(bodyParameter, JsonObject.class);

    Object form_data = convertedObject.get("form_data");
    String pdf_template = convertedObject.get("template_path").getAsString();
    Object structure_inputs = convertedObject.get("structure_inputs");
    String output_name = String.valueOf(timestamp.getTime());

    form_array = formService.getFormArray(form_data);
    structure_input_array = formService.getStructureInputArray(structure_inputs);

    File file = File.createTempFile(output_name, "pdf");

    formService.fillForm(form_array, pdf_template, structure_input_array, file, output_name);

    uploadS3(file, output_name);

    return new Form(new Object(), form_data, pdf_template, structure_inputs, url_download);
  }

  @PostMapping(path = "/api/v1/combineform", consumes = "application/json", produces = "application/json")
  public Form combineform(@RequestBody String bodyParameter) throws Exception {
    JSONArray pdf_array = new JSONArray();
    FormService formService = new FormService();
    String combined_file_name = "combined-pdf";

    JsonObject convertedObject = gson.fromJson(bodyParameter, JsonObject.class);

    Object pdf_data = convertedObject.get("pdf_data");
    pdf_array = formService.getFormArray(pdf_data);

    File combined_file = File.createTempFile(combined_file_name, "pdf");

    formService.combineForm(combined_file, pdf_array);

    uploadS3(combined_file, combined_file_name);

    return new Form(new Object(), new Object(), "", new Object(), url_download);
  }


  @PostMapping(path = "/api/v1/generate-receipt", consumes = "application/json", produces = "application/json")
  public Receipt generateReceipt(@RequestBody String bodyParameter) throws Exception {
    ReceiptService receiptService = new ReceiptService();
    Receipt receipt = gson.fromJson(bodyParameter, Receipt.class);

    Object form_data = receipt.getForm_data();
    String output_name = receipt.getOutput_name();
    String receipt_type = receipt.getReceipt_type();

    File file = File.createTempFile(output_name, ".pdf");
    FileOutputStream fileOutputStream = new FileOutputStream(file);

    receiptService.generateReceipt(receipt, fileOutputStream);

    uploadS3(file, output_name);

    return new Receipt(form_data, output_name, receipt_type, url_download, "success");
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