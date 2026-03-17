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

  private static final float DPI_CONVERSION = 0.75f;
  private static final float SCALE_ADJUSTMENT = 0.87f;

  public FillFormService() {
    this.pdfUtilityService = new PdfUtilityService();
  }

  private float convertHtmlToPdf(float htmlValue) {
    return htmlValue * DPI_CONVERSION * SCALE_ADJUSTMENT;
  }

  public void fillForm(JSONArray form_array, String pdf_template, JSONArray custom_field_array, File file, String output_name) throws IOException {
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
        if (field.getWidgets() == null || field.getWidgets().isEmpty()) {
          continue;
        }
        PdfPage page = field.getWidgets().get(0).getPage();
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
        Rectangle fieldsRectInput = field.getWidgets().get(0).getRectangle().toRectangle();
        boolean inputIsMultiline = field.isMultiline();

        float inputDynamicFontSize = getDynamicFontSize(value, fieldsRectInput, font);
        if (inputIsMultiline) {
          inputDynamicFontSize = getDynamicMultiLineFontSize(value, fieldsRectInput, font);
        }

        Text text = new Text(value).setFont(font).setFontSize(inputDynamicFontSize);
        Paragraph p = new Paragraph(text).setFontColor(ColorConstants.BLACK);


        if (inputIsMultiline) {
          fillFieldMultiline(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font);
        } else {
          fillFieldInput(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font, false);
        }

      }
    }
    if (custom_field_array != null) {
      PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER);
      Iterator<?> cfIter = custom_field_array.iterator();
      while (cfIter.hasNext()) {
        Object o = cfIter.next();
        if (!(o instanceof JSONObject)) {
          continue;
        }
        JSONObject cfObj = (JSONObject) o;
        String cfValue = cfObj.get("value") == null ? "" : cfObj.get("value").toString();
        String cfName = cfObj.get("label") != null ? cfObj.get("label").toString() : cfObj.get("id").toString();

        int pageNumber = 1;
        Object pageObj = cfObj.get("page");
        if (pageObj != null) {
          try {
            pageNumber = Integer.parseInt(pageObj.toString());
          } catch (NumberFormatException ignored) {}
        }
        PdfPage cfPage = pdf.getPage(pageNumber);
        Float rawX = convertHtmlToPdf(Float.valueOf(cfObj.get("x").toString()));
        Float rawY = convertHtmlToPdf(Float.valueOf(cfObj.get("y").toString()));
        Float width = convertHtmlToPdf(Float.valueOf(cfObj.get("width").toString()));
        Float height = convertHtmlToPdf(Float.valueOf(cfObj.get("height").toString()));
        float pageHeight = cfPage.getPageSize().getHeight();
        Float y = pageHeight - rawY - height;
        Rectangle fieldsRect = new Rectangle(rawX, y, width, height);
        Paragraph cfParagraph = new Paragraph(cfValue)
          .setFont(font)
          .setFontSize(8)
          .setFontColor(ColorConstants.BLACK)
          .setFixedLeading(8);

        fillFieldInput(pdf, form, cfName, cfValue, pdf_template, cfPage, cfParagraph, 8, fieldsRect, font, true);
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
    float simulatedLeading = dynamicFontSize * 1.4f;

    if (pdf_template.toLowerCase().contains("n-648") && fieldsRect.getHeight() > 140) {
      p.setFixedLeading(simulatedLeading).setPaddingTop((float) -5);
    } else if (fieldsRect.getHeight() > 430 && fieldsRect.getHeight() < 660) {
      p.setFixedLeading(simulatedLeading).setPaddingTop((float) -6.5);
    } else {
      p.setFixedLeading(simulatedLeading).setPaddingTop(-5);
    }
    addTextToCanvas(page, pdf, fieldsRect, p);
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
    PdfFont font,
    boolean isCustomField
  ) throws IOException {
    if (isCustomField) {
      System.out.println("Filling custom field " + name + " with value: " + value);
      addTextToCanvas(page, pdf, fieldsRect, p);
    }
    if (name.toLowerCase().contains("state")) {
      form.removeField(name);
      p.setPaddingLeft(2);
      addTextToCanvas(page, pdf, fieldsRect, p);
    } else {
      PdfFormField field = form.getField(name);
      if (field != null) {
        if (field instanceof PdfTextFormField) {
          PdfTextFormField textField = (PdfTextFormField) field;
          textField.setFontSize(dynamicFontSize);
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

  private float getDynamicFontSize(String value, Rectangle fieldsRect, PdfFont font) {
    float fontSize = 10f;
    int[] fontBox = font.getFontProgram().getFontMetrics().getBbox();
    int fontHeight = (fontBox[2] - fontBox[1]);
    float rectHeight = fieldsRect.getHeight();
    fontSize = Math.min(fontSize, rectHeight / fontHeight * FontProgram.UNITS_NORMALIZATION);
    float rectWidth = fieldsRect.getWidth();
    float stringWidth = font.getWidth(value, 1f);
    if (stringWidth > 0) {
      fontSize = Math.min(fontSize, (rectWidth - 3) / stringWidth);
    }
    // never shrink to zero or negative; keep at least one point
    return Math.max(fontSize, 1f);
  }

  private float getDynamicMultiLineFontSize(String value, Rectangle fieldsRect, PdfFont font) {
    float maxFontSize = 10f; 
    float minFontSize = 1f;  
    float usableWidth = fieldsRect.getWidth() - 8f; 
    float usableHeight = fieldsRect.getHeight() - 8f;

    if (value == null || value.trim().isEmpty()) {
      return maxFontSize;
    }

    String[] words = value.split("\\s+");
    float spaceWidthAt1Pt = font.getWidth(" ", 1f);
    float bestFontSize = minFontSize;
    
    // Binary search
    for (int i = 0; i < 15; i++) {
      float testSize = (minFontSize + maxFontSize) / 2f;
      
      if (doesTextFit(words, testSize, usableWidth, usableHeight, font, spaceWidthAt1Pt)) {
        bestFontSize = testSize; // It fits! Try going bigger.
        minFontSize = testSize;  
      } else {
        maxFontSize = testSize;  // Too big! Try going smaller.
      }
    }

    float finalSize = (float) (Math.floor(bestFontSize * 10) / 10);
    System.out.println("Final simulated multi-line font size: " + finalSize);
    
    return Math.max(finalSize, 1f);
  }

  private boolean doesTextFit(String[] words, float fontSize, float usableWidth, float usableHeight, PdfFont font, float spaceWidthAt1Pt) {
    float lineHeight = fontSize * 1.3f; 
    int lineCount = 1;
    float currentLineWidth = 0f;

    for (String word : words) {
      float wordWidth = font.getWidth(word, 1f) * fontSize;
      float spaceWidth = spaceWidthAt1Pt * fontSize;

      if (wordWidth > usableWidth) {
        return false; 
      }

      if (currentLineWidth == 0) {
        currentLineWidth = wordWidth; 
      } else if (currentLineWidth + spaceWidth + wordWidth > usableWidth) {
        lineCount++;
        currentLineWidth = wordWidth; 
      } else {
        currentLineWidth += spaceWidth + wordWidth; 
      }
    }

    float totalHeight = lineCount * lineHeight;
    return totalHeight <= usableHeight; 
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

  public JSONArray getCustomFieldsArray(Object custom_fields) throws IOException, java.text.ParseException, org.json.simple.parser.ParseException {
    JSONParser jsonParser = new JSONParser();
    Object custom_field_object = jsonParser.parse(gson.toJson(custom_fields));
    JSONArray custom_field_array = (JSONArray) custom_field_object;
    return custom_field_array;
  }

}
