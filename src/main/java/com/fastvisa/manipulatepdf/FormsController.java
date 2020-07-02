package com.fastvisa.manipulatepdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.xfa.XfaForm;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@RestController
public class FormsController {

  @PostMapping(path = "/api/v1/fillform", consumes = "application/json", produces = "application/json")
  public ResponseEntity<InputStreamResource> fillform(@RequestBody String bodyParameter) throws Exception {
    Gson gson = new Gson();
    JSONParser jsonParser = new JSONParser();
    JSONArray form_array = new JSONArray();
    Object form_object = new Object();
    
    Form g = gson.fromJson(bodyParameter, Form.class);

    Object form_data = g.formData();
    String template_path = g.templatePath();
    String output_name = g.outputName();

    if( form_data instanceof String ) {
      FileReader form_reader = new FileReader((String) form_data);
      form_object = jsonParser.parse(form_reader);
      form_array = (JSONArray) form_object;
    } else {
      form_object = jsonParser.parse(gson.toJson(form_data));
      form_array = (JSONArray) form_object;
    }

    File file = File.createTempFile(output_name, "pdf");
    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    
    fillForm(form_array, template_path, file);

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/pdf"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + g.outputName() + ".pdf\"")
      .body(resource);
  }

  private void fillForm(JSONArray form_array, String template_path, File file) throws IOException {
    String output_file = file.getAbsolutePath();
		PdfReader reader = new PdfReader(template_path);
    reader.setUnethicalReading(true);
    PdfDocument pdf = new PdfDocument(reader, new PdfWriter(output_file));
    removeUsageRights(pdf);

    PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);
    XfaForm xfa = form.getXfaForm();
    
    Map<String, PdfFormField> fields = form.getFormFields();
    Iterator i = form_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      String name = innerObj.get("name").toString();
      String value = innerObj.get("value").toString();
      if (fields.get(name) != null) {
        xfa.setXfaFieldValue(name, value);
        fields.get(name)
        .setBorderWidth(3)
        .setFontSize((float) 6.5)
        .setValue(value);
      }
    } 
    form.flattenFields();
    xfa.write(pdf);
    pdf.close();
    file.delete();
  }

  private void removeUsageRights(PdfDocument pdfDoc) {
    PdfDictionary perms = pdfDoc.getCatalog().getPdfObject().getAsDictionary(PdfName.Perms);
    if (perms == null) {
        return;
    }
    perms.remove(new PdfName("UR"));
    perms.remove(PdfName.UR3);
    if (perms.size() == 0) {
        pdfDoc.getCatalog().remove(PdfName.Perms);
    }
  }
  
}