package com.fastvisa.services;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.xfa.XfaForm;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FormService {
  public void fillForm(JSONArray form_array, String template_path, File file) throws IOException {
    String output_file = file.getAbsolutePath();
		PdfReader reader = new PdfReader(template_path);
    reader.setUnethicalReading(true);
    PdfDocument pdf = new PdfDocument(reader, new PdfWriter(output_file));
    removeUsageRights(pdf);

    PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);
    XfaForm xfa = form.getXfaForm();
    
    Map<String, PdfFormField> fields = form.getFormFields();
    Iterator<?> i = form_array.iterator();
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