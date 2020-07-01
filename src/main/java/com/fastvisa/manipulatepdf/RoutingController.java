package com.fastvisa.manipulatepdf;

import java.io.*;
import java.util.Map;

import com.google.gson.Gson;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.xfa.XfaForm;

@RestController
public class RoutingController {

	@PostMapping(path = "/fill_form", consumes = "application/json", produces = "application/json")
	public Request fill_form(@RequestBody String bodyParameter) throws Exception {
		Gson gson = new Gson();
		Request g = gson.fromJson(bodyParameter, Request.class);

		String form_data = g.getTemplatePath();
		String template_file = g.getTemplatePath();
    String output_file = String.format("result/%s.pdf", g.getName());
    
    File file = new File(output_file);
    file.getParentFile().mkdirs();
		PdfReader reader = new PdfReader(template_file);
    reader.setUnethicalReading(true);
    PdfDocument pdf = new PdfDocument(reader, new PdfWriter(output_file));
    removeUsageRights(pdf);

    PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);
    XfaForm xfa = form.getXfaForm();
    
    Map<String, PdfFormField> fields = form.getFormFields();
    BufferedReader csvReader = new BufferedReader(new FileReader(form_data));
    String row;
    while ((row = csvReader.readLine()) != null) {
      String[] data = row.split("\t");
      if (fields.get(data[0]) != null) {
        xfa.setXfaFieldValue(data[0], data[1]);
        fields.get(data[0])
        .setBorderWidth(3)
        .setFontSize((float) 6.5)
        .setValue(data[1]);
      }
    }
    // form.flattenFields();
    xfa.write(pdf);
    csvReader.close();
		pdf.close();

    Request response = new Request(g.getName(), g.getFormData(), g.getTemplatePath());
    return response;
	}

	public void removeUsageRights(PdfDocument pdfDoc) {
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