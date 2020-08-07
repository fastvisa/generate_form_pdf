package com.fastvisa.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.io.font.FontProgram;
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
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class FormService {
  public void fillForm(JSONArray form_array, String template_path, File file, String output_name) throws IOException {
    String output_file = file.getAbsolutePath();
		PdfReader reader = new PdfReader(template_path);
    reader.setUnethicalReading(true);
    PdfDocument pdf = new PdfDocument(reader, new PdfWriter(output_file));
    removeUsageRights(pdf);

    PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);
    
    Map<String, PdfFormField> fields = form.getFormFields();
    Iterator<?> i = form_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      String name = innerObj.get("name").toString();
      Object valueObject = innerObj.get("value");
      String value = valueObject == null ? "" : valueObject.toString();

      if (fields.get(name) != null) {
        fillField(pdf, form, fields, name, value, output_name, template_path);
      }
    } 
    form.flattenFields();
    pdf.close();
  }

  public void combineForm(File combined_file, JSONArray pdf_array) throws IOException, ParseException, org.json.simple.parser.ParseException {
    PdfDocument pdf = new PdfDocument(new PdfWriter(combined_file));
    PdfMerger merger = new PdfMerger(pdf);
    JSONArray form_array = new JSONArray();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    Iterator<?> i = pdf_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      Object form_data = innerObj.get("form_data");
      String template_path = innerObj.get("template_path").toString();
      String output_name = String.valueOf(timestamp.getTime());
    
      if (form_data != null) {
        form_array = getFormArray(form_data);
        File file = File.createTempFile(output_name, "pdf");
  
        fillForm(form_array, template_path, file, output_name);
  
        PdfDocument sourcePdf = new PdfDocument(new PdfReader(file));
        merger.merge(sourcePdf, 1, sourcePdf.getNumberOfPages());
        sourcePdf.close();
      }
    }
    pdf.close();
  }

  public JSONArray getFormArray(Object form_data) throws IOException, ParseException, org.json.simple.parser.ParseException {
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

  private Gson gson = new GsonBuilder().serializeNulls().create();

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

  private void fillField(PdfDocument pdf, PdfAcroForm form, Map<String, PdfFormField> fields, String name, String value, String output_name, String template_path) throws IOException {
    Boolean isMultiline = fields.get(name).isMultiline();
    Rectangle fieldsRect = fields.get(name).getWidgets().get(0).getRectangle().toRectangle();
    PdfPage page = fields.get(name).getWidgets().get(0).getPage();
    Text text = new Text(value);
    PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
    text.setFont(font).setFontSize((float) 10);
    Paragraph p = new Paragraph(text);
    float dynamicFontSize = getDynamicFontSize(value, fieldsRect, font);

    if (isMultiline) {
      PdfTextFormField newField = PdfTextFormField.createText(pdf, fieldsRect, name, value);
      form.removeField(name);
      if (fieldsRect.getWidth() < 200 && fieldsRect.getHeight() < 30) {
        form.addField(newField, page);
        fields.get(name)
        .setFont(font)
        .setFontSize((float) dynamicFontSize);
      } else {
        if (template_path.toLowerCase().contains("n-648") && fieldsRect.getHeight() > 140) {
          p.setFixedLeading((float) 15).setPaddingTop((float) -5);
        } else if (fieldsRect.getHeight() > 430 && fieldsRect.getHeight() < 660) {
          p.setFixedLeading((float) 18).setPaddingTop((float) -6.5);
        } else {
          p.setFixedLeading((float) 18).setPaddingTop(-5);
        }
        addTextToCanvas(page, pdf, fieldsRect, p);
      }
    } else if (name.toLowerCase().contains("state")) {
      form.removeField(name);
      p.setPaddingLeft(2);
      addTextToCanvas(page, pdf, fieldsRect, p);
    } else {
      fields.get(name)
      .setFont(font)
      .setFontSize((float) dynamicFontSize)
      .setValue(value);
    }
  }

  private void addTextToCanvas(PdfPage page, PdfDocument pdf, Rectangle fieldsRect, Paragraph p) {
    PdfCanvas canvas = new PdfCanvas(page);
    new Canvas(canvas, pdf, fieldsRect).add(p);
    canvas.rectangle(fieldsRect);
  }

  private float getDynamicFontSize(String value, Rectangle fieldsRect, PdfFont font) {
    float fontSize = 10;
    int[] fontBox = font.getFontProgram().getFontMetrics().getBbox();
    int fontHeight = (fontBox[2] - fontBox[1]);
    float rectHeight = fieldsRect.getHeight();
    fontSize = Math.min(fontSize, rectHeight / fontHeight * FontProgram.UNITS_NORMALIZATION);
    float rectWidth = fieldsRect.getWidth();
    float stringWidth = font.getWidth(value, 1);
    fontSize = Math.min(fontSize, (rectWidth - 3) / stringWidth);
    return fontSize;
  }

}