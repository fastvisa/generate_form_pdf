package com.fastvisa.services;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.xfa.XfaForm;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FormService {
  public void fillForm(JSONArray form_array, String template_path, File file, String output_name) throws IOException {
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
        fillPdfForm(pdf, form, fields, name, value, output_name);
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

  private void fillPdfForm(PdfDocument pdf, PdfAcroForm form, Map<String, PdfFormField> fields, String name, String value, String output_name) throws IOException {
    Boolean isMultiline = fields.get(name).isMultiline();
    Rectangle fieldsRect = fields.get(name).getWidgets().get(0).getRectangle().toRectangle();
    PdfPage page = fields.get(name).getWidgets().get(0).getPage();
    Text text = new Text(value);
    PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
    text.setFont(font).setFontSize(10);
    Paragraph p = new Paragraph(text);

    if (isMultiline) {
      form.removeField(name);
      if (output_name.toLowerCase().contains("n-648") && fieldsRect.getHeight() > 140) {
        p.setFixedLeading((float) 15).setPaddingTop((float) -5);
      } else if (output_name.toLowerCase().contains("g-845sup") && fieldsRect.getHeight() > 140) {
        p.setFixedLeading((float) 15).setPaddingTop((float) -5);
      } else if (fieldsRect.getHeight() > 430 && fieldsRect.getHeight() < 660) {
        p.setFixedLeading((float) 18).setPaddingTop((float) 8);
      } else {
        p.setFixedLeading((float) 18).setPaddingTop(-5);
      }
      addTextToCanvas(page, pdf, fieldsRect, p);
    } else if (name.toLowerCase().contains("state")) {
      form.removeField(name);
      p.setPaddingLeft(2);
      addTextToCanvas(page, pdf, fieldsRect, p);
    } else if (output_name.toLowerCase().contains("cert-ind")) {
      form.removeField(name);
      p.setPaddingTop(10);
      addTextToCanvas(page, pdf, fieldsRect, p);
    } else {
      fields.get(name)
      .setFont(font)
      .setValue(value);
    }
  }

  private void addTextToCanvas(PdfPage page, PdfDocument pdf, Rectangle fieldsRect, Paragraph p) {
    PdfCanvas canvas = new PdfCanvas(page);
    new Canvas(canvas, pdf, fieldsRect).add(p);
    canvas.rectangle(fieldsRect);
  }

}