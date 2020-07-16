package com.fastvisa.manipulatepdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import com.google.gson.Gson;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.layout.Document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    
    form_array = getFormArray(form_data);

    File file = File.createTempFile(output_name, "pdf");

    formService.fillForm(form_array, template_path, file, output_name);

    uploadS3(file, output_name);

    return new Form(new Object(), form_data, template_path, output_name, url_download, "success");
  }

  @PostMapping(path = "/api/v1/combineform", consumes = "application/json", produces = "application/json")
  public Form combineform(@RequestBody String bodyParameter) throws Exception {
    JSONArray pdf_array = new JSONArray();
    FormService formService = new FormService();
    JSONArray form_array = new JSONArray();
    File combinedFile = File.createTempFile("combined-pdf-" + new Date(), "pdf");

    Form g = gson.fromJson(bodyParameter, Form.class);
    // pdf_array = (JSONArray) g.pdfData();

    PdfDocument pdf = new PdfDocument(new PdfWriter(combinedFile));
    PdfMerger merger = new PdfMerger(pdf);

    // Iterator<?> i = pdf_array.iterator();
    // while (i.hasNext()) {
    //   JSONObject innerObj = (JSONObject) i.next();
      // Object form_data = innerObj.get("form_data");
      // String template_path = innerObj.get("template_path").toString();
      // String output_name = innerObj.get("output_name").toString();
    
      // form_array = getFormArray(form_data);
  
      // File file = File.createTempFile(output_name, "pdf");

      // formService.fillForm(form_array, template_path, file, output_name);

      // PdfDocument sourcePdf = new PdfDocument(new PdfReader(file));
      // merger.merge(sourcePdf, 1, sourcePdf.getNumberOfPages());
      // sourcePdf.close();
    // }
    System.out.println(g.pdfData());
    // pdf.close();

    return new Form(new Object(), new Object(), "", "", combinedFile.getAbsolutePath(), "success");
  }

  @Value("${aws.accessKey}")
  private String accessKey;
  @Value("${aws.secretKey}")
  private String secretKey;
  @Value("${aws.s3bucket.name}")
  private String bucketName;

  private void uploadS3(File file, String output_name) {
    try {
      AwsS3Service s3Service = new AwsS3Service(accessKey, secretKey);
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

  private JSONArray getFormArray(Object form_data) throws IOException, ParseException {
    Object form_object = new Object();
    JSONParser jsonParser = new JSONParser();
    JSONArray form_array = new JSONArray();

    if( form_data instanceof String ) {
      FileReader form_reader = new FileReader((String) form_data);
      form_object = jsonParser.parse(form_reader);
      form_array = (JSONArray) form_object;
    } else {
      form_object = jsonParser.parse(gson.toJson(form_data));
      form_array = (JSONArray) form_object;
    }
    return form_array;
  }
  
}