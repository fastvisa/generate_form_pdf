package com.fastvisa.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
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
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.VerticalAlignment;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.yaml.snakeyaml.util.UriEncoder;

public class FormService {
  public void fillForm(JSONArray form_array, String pdf_template, JSONArray structure_input_array, File file, String output_name) throws IOException {
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

      if (fields.get(name) != null) {
        PdfPage page = fields.get(name).getWidgets().get(0).getPage();
        Text text = new Text(value);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
        text.setFont(font).setFontSize((float) 10);
        Paragraph p = new Paragraph(text).setFontColor(ColorConstants.BLACK);

        Rectangle fieldsRectInput = fields.get(name).getWidgets().get(0).getRectangle().toRectangle();
        float inputDynamicFontSize = getDynamicFontSize(value, fieldsRectInput, font);
        boolean inputIsMultiline = form.getField(name).isMultiline();

        JSONArray structure = returnSearch(structure_input_array, name);

        if (!structure.isEmpty()) {
          JSONObject inputInnerObj = (JSONObject) structure.get(0);
          Float x = new Float(inputInnerObj.get("x").toString()) * (float) 0.75 * (float) 0.87;
          Float y = new Float(inputInnerObj.get("y").toString()) * (float) 0.75 * (float) 0.87;
          Float width = new Float(inputInnerObj.get("width").toString()) * (float) 0.75 * (float) 0.87;
          Float height = new Float(inputInnerObj.get("height").toString()) * (float) 0.75 * (float) 0.87;
          Rectangle fieldsRect = new Rectangle(x, y, width, height);
          float dynamicFontSize = getDynamicFontSize(value, fieldsRect, font);
          Boolean isMultiline = Boolean.parseBoolean(inputInnerObj.get("multiline").toString());

          if (isMultiline == false) {
            fillFieldInput(pdf, form, name, value, pdf_template, page, p, dynamicFontSize, fieldsRect, font);
          } else {
            Float row = Float.parseFloat(inputInnerObj.get("row").toString());
            fillStructureInputMultiline(pdf, form, name, value, pdf_template, page, p, dynamicFontSize, fieldsRect, font, Math.round(row));
          }
        } else {
          if (inputIsMultiline == false) {
            fillFieldInput(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font);
          } else {
            fillFieldMultiline(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font);
          }
        }

      }
    }
    form.flattenFields();
    pdf.close();
  }

  public void combineForm(File combined_file, JSONArray pdf_array) throws IOException, ParseException, org.json.simple.parser.ParseException {
    PdfDocument pdf = new PdfDocument(new PdfWriter(combined_file));
    PdfMerger merger = new PdfMerger(pdf);
    JSONArray form_array = new JSONArray();
    JSONArray structure_input_array = new JSONArray();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    Iterator<?> i = pdf_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      Object form_data = innerObj.get("form_data");
      Object structure_inputs = innerObj.get("structure_inputs");
      String pdf_template = innerObj.get("pdf_template").toString();
      String output_name = String.valueOf(timestamp.getTime());

      if (form_data != null) {
        form_array = getFormArray(form_data);
        structure_input_array = getStructureInputArray(structure_inputs);
        File file = File.createTempFile(output_name, "pdf");

        fillForm(form_array, pdf_template, structure_input_array, file, output_name);

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

  public JSONArray getStructureInputArray(Object structure_inputs) throws IOException, ParseException, org.json.simple.parser.ParseException {
    Object structure_input_object = new Object();
    JSONParser jsonParser = new JSONParser();
    JSONArray structure_input_array = new JSONArray();

    structure_input_object = jsonParser.parse(gson.toJson(structure_inputs));
    structure_input_array = (JSONArray) structure_input_object;
    return structure_input_array;
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
    PdfFont font
  ) throws IOException {
    PdfTextFormField newField = PdfTextFormField.createText(pdf, fieldsRect, name, value);
    form.removeField(name);
    if (fieldsRect.getWidth() < 200 && fieldsRect.getHeight() < 30) {
      form.addField(newField, page);
      form.getField(name)
      .setFont(font)
      .setFontSize((float) dynamicFontSize);
    } else {
      if (pdf_template.toLowerCase().contains("n-648") && fieldsRect.getHeight() > 140) {
        p.setFixedLeading((float) 15).setPaddingTop((float) -5);
      } else if (fieldsRect.getHeight() > 430 && fieldsRect.getHeight() < 660) {
        p.setFixedLeading((float) 18).setPaddingTop((float) -6.5);
      } else {
        p.setFixedLeading((float) 18).setPaddingTop(-5);
      }
      addTextToCanvas(page, pdf, fieldsRect, p);
    }
  }

  private void fillStructureInputMultiline(
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
    int row
  ) throws IOException {
    Rectangle cellStaticRect = new Rectangle(fieldsRect.getX(), fieldsRect.getY(), fieldsRect.getWidth(), 14);
    int page_number = pdf.getPageNumber(page);
    form.removeField(name);
    String[] splitted_array = value.split(" ", 0);
    String[] value_array = chunkArray(splitted_array, row);

    Table table = new Table(1);
    Cell cell;
    for (int i = 0; i < row; i++) {
      float cellDynamicFontSize = getDynamicFontSize(value_array[i], cellStaticRect, font);
      cell = new Cell().add(new Paragraph(value_array[i]).setFontSize(cellDynamicFontSize).setFont(font));
      cell.setHeight((float) 14);
      cell.setBorder(Border.NO_BORDER)
          .setVerticalAlignment(VerticalAlignment.MIDDLE);
      table.addCell(cell);
    }
    table.setFixedPosition(page_number, fieldsRect.getLeft(), fieldsRect.getBottom(), fieldsRect.getWidth());
    table.setBorder(Border.NO_BORDER);
    Document doc = new Document(page.getDocument());
    doc.add(table);
  }

  public static String[] chunkArray(String[] splitted_array, int chunkSize) {
    int numOfChunks = (int) Math.ceil((double) splitted_array.length / chunkSize);
    String[] output = new String[chunkSize];

    int index = 0;
    for(int i=0;i<splitted_array.length;i+=numOfChunks){
      String[] chunk_array = Arrays.copyOfRange(splitted_array, i, Math.min(splitted_array.length,i+numOfChunks));
      StringBuffer sb = new StringBuffer();
      for(int s = 0; s < chunk_array.length; s++) {
        sb.append(chunk_array[s] + " ");
      }
      output[index++] = sb.toString();
    }

    return output;
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
    // new Canvas(canvas, pdf, fieldsRect).add(p);
    Canvas cvs = new Canvas(canvas, fieldsRect);
    cvs.add(p);
    pdf = cvs.getPdfDocument();
    canvas.rectangle(fieldsRect);
  }

  private JSONArray returnSearch(JSONArray array, String searchValue){
    JSONArray filtedArray = new JSONArray();
    for (int i = 0; i < array.size(); i++) {
      JSONObject obj= null;
      obj = (JSONObject) array.get(i);
      if(UriEncoder.decode(obj.get("field_name").toString()).equals(searchValue))
      {
        filtedArray.add(obj);
      }
    }
    return filtedArray;
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