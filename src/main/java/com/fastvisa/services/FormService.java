package com.fastvisa.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
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
import org.yaml.snakeyaml.util.UriEncoder;

public class FormService {
  public void fillForm(JSONArray form_array, String pdf_template, File file, String output_name, JSONArray custom_fields) throws IOException {
    String output_file = file.getAbsolutePath();
		PdfReader reader = new PdfReader(pdf_template);
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

      JSONObject filtered_data = returnFormSearch(custom_fields, name);

      if (fields.get(name) != null) {
        PdfPage page = fields.get(name).getWidgets().get(0).getPage();
        Text text = new Text(value);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
        text.setFont(font).setFontSize((float) 10);
        Paragraph p = new Paragraph(text).setFontColor(ColorConstants.BLACK);
        Rectangle fieldsRectInput = fields.get(name).getWidgets().get(0).getRectangle().toRectangle();
        float inputDynamicFontSize = getDynamicFontSize(value, fieldsRectInput, font);
        boolean isMultilineInput = form.getField(name).isMultiline();
        if (isMultilineInput == false) {
          fillFieldInput(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font);
        } else if (!filtered_data.isEmpty() && filtered_data.get("field_type").equals("custom_shrink")) {
          float font_size = Float.parseFloat(filtered_data.get("font_size").toString()) * 72 / 96;
          text.setFont(font).setFontSize(font_size);
          fillFieldMultiline(pdf, form, name, value, pdf_template, page, p, font_size, fieldsRectInput, font, true);
        } else {
          fillFieldMultiline(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font, false);
        }
      }
    }
    Iterator<?> j = custom_fields.iterator();
    while (j.hasNext()) {
      JSONObject innerObj = (JSONObject) j.next();
      String field_type = innerObj.get("field_type").toString();
      
      if (field_type.equals("text_field")) {
        String value_text = innerObj.get("value").toString();
        int page = Integer.parseInt(innerObj.get("page").toString());
        int x = Integer.parseInt(innerObj.get("x").toString());
        int y = Integer.parseInt(innerObj.get("y").toString());
        float width = Float.valueOf(innerObj.get("width").toString());
        float height = Float.valueOf(innerObj.get("height").toString());
        
        int convertedX = (int) ((x + 25) * pdf.getPage(page).getMediaBox().getWidth() / 935); // (x of html + input padding) * pdf width / html width 
        int convertedY = (int) ((1210 - y - 40) * pdf.getPage(page).getMediaBox().getHeight() / 1210); // (html width - y of html - input height) * pdf height / html height

        Rectangle rect = new Rectangle(convertedX, convertedY, width, height);
        Text text = new Text(value_text);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
        text.setFont(font).setFontSize((float) 10);
        Paragraph par = new Paragraph(text).setFontColor(ColorConstants.BLACK);
        addTextToCanvas(pdf.getPage(page), pdf, rect, par); 
      }
    }
    form.flattenFields();
    pdf.close();
  }

  public void combineForm(File combined_file, JSONArray pdf_array) throws IOException, ParseException, org.json.simple.parser.ParseException {
    PdfDocument pdf = new PdfDocument(new PdfWriter(combined_file));
    PdfMerger merger = new PdfMerger(pdf);
    JSONArray form_array = new JSONArray();
    JSONArray custom_fields_array = new JSONArray();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    Iterator<?> i = pdf_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      Object form_data = innerObj.get("form_data");
      Object custom_fields = innerObj.get("custom_fields");
      String pdf_template = innerObj.get("template_path").toString();
      String output_name = String.valueOf(timestamp.getTime());

      if (form_data != null) {
        form_array = getFormArray(form_data);
        custom_fields_array = getCustomFieldArray(custom_fields);
        File file = File.createTempFile(output_name, "pdf");

        fillForm(form_array, pdf_template, file, output_name, custom_fields_array);

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

  public JSONArray getCustomFieldArray(Object custom_fields) throws IOException, ParseException, org.json.simple.parser.ParseException {
    Object custom_fields_object = new Object();
    JSONParser jsonParser = new JSONParser();
    JSONArray custom_fields_array = new JSONArray();

    custom_fields_object = jsonParser.parse(gson.toJson(custom_fields));
    custom_fields_array = (JSONArray) custom_fields_object;
    return custom_fields_array;
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

  private void fillFieldMultiline(
    PdfDocument pdf,
    PdfAcroForm form,
    String name,
    String value,
    String pdf_template,
    PdfPage page,
    Paragraph p,
    float dynamicFontSize,
    Rectangle fieldsRect,
    PdfFont font,
    Boolean isCustomShrink
  ) throws IOException {
    PdfTextFormField newField = PdfTextFormField.createText(pdf, fieldsRect, name, value);
    form.removeField(name);
    if (fieldsRect.getWidth() < 200 && fieldsRect.getHeight() < 30) {
      form.addField(newField, page);
      form.getField(name)
      .setFont(font)
      .setFontSize((float) dynamicFontSize);
    } else {
      if (!isCustomShrink) {
        if (pdf_template.toLowerCase().contains("n-648") && fieldsRect.getHeight() > 140) {
          p.setFixedLeading((float) 15).setPaddingTop((float) -5);
        } else if (fieldsRect.getHeight() > 430 && fieldsRect.getHeight() < 660) {
          p.setFixedLeading((float) 18).setPaddingTop((float) -6.5);
        } else {
          p.setFixedLeading((float) 18).setPaddingTop(-5);
        }
      }
      addTextToCanvas(page, pdf, fieldsRect, p);
    }
  }

  private void fillFieldInput(
    PdfDocument pdf,
    PdfAcroForm form,
    String name,
    String value,
    String pdf_template,
    PdfPage page,
    Paragraph p,
    float dynamicFontSize,
    Rectangle fieldsRect,
    PdfFont font
  ) throws IOException {
    if (name.toLowerCase().contains("state")) {
      form.removeField(name);
      p.setPaddingLeft(2);
      addTextToCanvas(page, pdf, fieldsRect, p);
    } else {
      form.getField(name)
      .setFont(font)
      .setFontSize((float) dynamicFontSize)
      .setColor(ColorConstants.BLACK)
      .setValue(value);
    }
  }

  private void addTextToCanvas(PdfPage page, PdfDocument pdf, Rectangle fieldsRect, Paragraph p) {
    PdfCanvas canvas = new PdfCanvas(page);
    try (
    Canvas cvs = new Canvas(canvas, fieldsRect)) {
      cvs.add(p);
      pdf = cvs.getPdfDocument();
    }
    canvas.rectangle(fieldsRect);
  }

  private JSONObject returnFormSearch(JSONArray array, String searchValue){
    JSONObject filteredObject = new JSONObject();
    for (int i = 0; i < array.size(); i++) {
      JSONObject obj= null;
      obj = (JSONObject) array.get(i);
      if(UriEncoder.decode(obj.get("label").toString()).equals(searchValue)) {
        filteredObject = obj;
      }
    }
    return filteredObject;
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