package com.fastvisa.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfButtonFormField;
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
import java.util.Arrays;

public class FillFormService {
  
  private Gson gson = new GsonBuilder().serializeNulls().create();
  private PdfUtilityService pdfUtilityService;

  public FillFormService() {
    this.pdfUtilityService = new PdfUtilityService();
  }


  public void fillForm(JSONArray form_array, String pdf_template, JSONArray structure_input_array, File file, String output_name) throws IOException {
    String output_file = file.getAbsolutePath();
    
    // Handle URL-based templates by downloading them first
    String actualPdfPath = pdfUtilityService.getPdfTemplatePath(pdf_template);
    
    PdfReader reader = new PdfReader(actualPdfPath);
    reader.setUnethicalReading(true);
    PdfDocument pdf = new PdfDocument(reader, new PdfWriter(output_file));
    removeUsageRights(pdf);

    PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);

    Iterator<?> i = form_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      Object nameObj = innerObj.get("name");
      if (nameObj == null) {
        continue;
      }
      String name = nameObj.toString();
      Object valueObject = innerObj.get("value");
      String value = valueObject == null ? "" : valueObject.toString();

      PdfFormField field = form.getField(name);
      if (field != null) {
        PdfPage page = field.getWidgets().get(0).getPage();
        Text text = new Text(value);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
        text.setFont(font).setFontSize((float) 10);
        Paragraph p = new Paragraph(text).setFontColor(ColorConstants.BLACK);

        Rectangle fieldsRectInput = field.getWidgets().get(0).getRectangle().toRectangle();
        float inputDynamicFontSize = getDynamicFontSize(value, fieldsRectInput, font);
        boolean inputIsMultiline = field.isMultiline();

        JSONArray structure = returnSearch(structure_input_array, name);

        if (!structure.isEmpty()) {
          JSONObject inputInnerObj = (JSONObject) structure.get(0);
          Object xObj = inputInnerObj.get("x");
          Object yObj = inputInnerObj.get("y");
          Object widthObj = inputInnerObj.get("width");
          Object heightObj = inputInnerObj.get("height");
          Object multilineObj = inputInnerObj.get("multiline");
          Object rowObj = inputInnerObj.get("row");
          
          if (xObj == null || yObj == null || widthObj == null || heightObj == null || multilineObj == null) {
            continue;
          }
          
          Float x = Float.valueOf(xObj.toString()) * 0.75f * 0.87f;
          Float y = Float.valueOf(yObj.toString()) * 0.75f * 0.87f;
          Float width = Float.valueOf(widthObj.toString()) * 0.75f * 0.87f;
          Float height = Float.valueOf(heightObj.toString()) * 0.75f * 0.87f;
          Rectangle fieldsRect = new Rectangle(x, y, width, height);
          float dynamicFontSize = getDynamicFontSize(value, fieldsRect, font);
          Boolean isMultiline = Boolean.parseBoolean(multilineObj.toString());

          if (isMultiline == false) {
            fillFieldInput(pdf, form, name, value, pdf_template, page, p, dynamicFontSize, fieldsRect, font);
          } else {
            if (rowObj == null) {
              continue;
            }
            Float row = Float.parseFloat(rowObj.toString());
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
    // Skip creating new field and just use canvas for multiline text
    form.removeField(name);
    if (pdf_template.toLowerCase().contains("n-648") && fieldsRect.getHeight() > 140) {
      p.setFixedLeading((float) 15).setPaddingTop((float) -5);
    } else if (fieldsRect.getHeight() > 430 && fieldsRect.getHeight() < 660) {
      p.setFixedLeading((float) 18).setPaddingTop((float) -6.5);
    } else {
      p.setFixedLeading((float) 18).setPaddingTop(-5);
    }
    addTextToCanvas(page, pdf, fieldsRect, p);
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
    
    // Check if value_array has enough elements for the specified row count
    if (value_array == null || value_array.length < row) {
      return;
    }

    Table table = new Table(1);
    Cell cell;
    for (int i = 0; i < row && i < value_array.length; i++) {
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
    if (splitted_array == null || chunkSize <= 0 || splitted_array.length == 0) {
      return new String[0];
    }
    
    int numOfChunks = (int) Math.ceil((double) splitted_array.length / chunkSize);
    String[] output = new String[Math.min(chunkSize, numOfChunks)];

    int index = 0;
    for(int i = 0; i < splitted_array.length && index < output.length; i += numOfChunks) {
      String[] chunk_array = java.util.Arrays.copyOfRange(splitted_array, i, Math.min(splitted_array.length, i + numOfChunks));
      StringBuilder sb = new StringBuilder();
      for(int s = 0; s < chunk_array.length; s++) {
        sb.append(chunk_array[s]);
        if (s < chunk_array.length - 1) {
          sb.append(" ");
        }
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
      PdfFormField field = form.getField(name);
      if (field != null) {

        if (field instanceof PdfTextFormField) {
          PdfTextFormField textField = (PdfTextFormField) field;
          textField.setValue(value);
        }
        else if (field instanceof PdfButtonFormField) {
          PdfButtonFormField buttonField = (PdfButtonFormField) field;
          if (value != null && !value.isEmpty() && !value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("no")) {
            buttonField.setValue("Yes");
          } else {
            buttonField.setValue("Off");
          }
        }
        else {
          form.removeField(name);
          addTextToCanvas(page, pdf, fieldsRect, p);
        }
      }
    }
  }

  private void addTextToCanvas(PdfPage page, PdfDocument pdf, Rectangle fieldsRect, Paragraph p) {
    PdfCanvas canvas = new PdfCanvas(page);
    try (Canvas cvs = new Canvas(canvas, fieldsRect)) {
      cvs.add(p);
      pdf = cvs.getPdfDocument();
    }
    canvas.rectangle(fieldsRect);
  }

  @SuppressWarnings("unchecked")
  private JSONArray returnSearch(JSONArray array, String searchValue){
    JSONArray filtedArray = new JSONArray();
    // Check if array is null to avoid NullPointerException
    if (array == null) {
      return filtedArray;
    }
    
    for (int i = 0; i < array.size(); i++) {
      Object item = array.get(i);
      if (!(item instanceof JSONObject)) {
        continue;
      }
      JSONObject obj = (JSONObject) item;
      Object fieldName = obj.get("field_name");
      if (fieldName != null && UriEncoder.decode(fieldName.toString()).equals(searchValue)) {
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
    float stringWidth = font.getWidth(value);
    fontSize = Math.min(fontSize, (rectWidth - 3) / stringWidth);
    return fontSize;
  }

  public JSONArray getFormArray(Object form_data) throws IOException, java.text.ParseException, org.json.simple.parser.ParseException {
    JSONParser jsonParser = new JSONParser();
    JSONArray form_array = new JSONArray();

    if( form_data instanceof String ) {
      FileReader form_reader = new FileReader((String) form_data);
      Object form_object = jsonParser.parse(form_reader);
      form_array = (JSONArray) form_object;
    } else {
      Object form_object = jsonParser.parse(gson.toJson(form_data));
      form_array = (JSONArray) form_object;
    }
    return form_array;
  }

  public JSONArray getStructureInputArray(Object structure_inputs) throws IOException, java.text.ParseException, org.json.simple.parser.ParseException {
    JSONParser jsonParser = new JSONParser();
    Object structure_input_object = jsonParser.parse(gson.toJson(structure_inputs));
    JSONArray structure_input_array = (JSONArray) structure_input_object;
    return structure_input_array;
  }

}
