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
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Point;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.geom.Subpath;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.itextpdf.kernel.utils.PdfMerger;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FillFormService {

  private Gson gson = new GsonBuilder().serializeNulls().create();
  private PdfUtilityService pdfUtilityService;

  private static final float DPI_CONVERSION = 0.75f;
  private static final float SCALE_ADJUSTMENT = 0.87f;

  // Multiline text is drawn on canvas at this base size's line height, regardless of how far
  // the font auto-shrinks to fit width - otherwise the line spacing shrinks with long text and
  // drifts away from the template's fixed, pre-printed ruled lines the longer the field's value is.
  private static final float MULTILINE_BASE_FONT_SIZE = 10f;
  private static final float MULTILINE_LEADING_FACTOR = 1.4f;
  private static final float MULTILINE_LINE_HEIGHT = MULTILINE_BASE_FONT_SIZE * MULTILINE_LEADING_FACTOR;

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
    // Parsing a page's content stream for ruled-line candidates is done once per page and reused
    // across every multiline field on that page, instead of re-parsing per field.
    Map<PdfPage, List<float[]>> ruledLineCandidatesByPage = new HashMap<>();

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
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
        Rectangle fieldsRectInput = field.getWidgets().get(0).getRectangle().toRectangle();
        boolean inputIsMultiline = field.isMultiline();

        float multilineLineHeight = MULTILINE_LINE_HEIGHT;
        if (inputIsMultiline) {
          List<float[]> candidates = ruledLineCandidatesByPage.computeIfAbsent(page, this::findThinWideShapes);
          Float detectedSpacing = detectRuledLineSpacing(candidates, fieldsRectInput);
          if (detectedSpacing != null) {
            multilineLineHeight = detectedSpacing;
          }
          // A value with more explicit line breaks than the field can hold at this lineHeight
          // would make the fit-check permanently impossible regardless of font size (lineHeight
          // isn't tied to font size) and collapse the font to the 1pt floor. Tighten the leading
          // to whatever the forced line count needs instead, so those lines still render at a
          // legible size - e.g. the browser's own textarea preview for this same field shows the
          // forced line break as two normal-sized lines, not one shrunk line.
          multilineLineHeight = tightenLineHeightForForcedLines(value, fieldsRectInput.getHeight() - 8f, multilineLineHeight);
        }

        float inputDynamicFontSize = getDynamicFontSize(value, fieldsRectInput, font);
        if (inputIsMultiline) {
          inputDynamicFontSize = getDynamicMultiLineFontSize(value, fieldsRectInput, font, multilineLineHeight);
        }

        Text text = new Text(value).setFont(font).setFontSize(inputDynamicFontSize);
        Paragraph p = new Paragraph(text).setFontColor(ColorConstants.BLACK);


        if (inputIsMultiline) {
          fillFieldMultiline(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font, multilineLineHeight);
        } else {
          fillFieldInput(pdf, form, name, value, pdf_template, page, p, inputDynamicFontSize, fieldsRectInput, font, false);
        }

      }
    }
    if (custom_field_array != null) {
      PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
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
    PdfFont font,
    float lineHeight
  ) throws IOException {
    // Skip creating new field and just use canvas for multiline text
    form.removeField(name);
    float simulatedLeading = lineHeight;

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
          textField.setFont(font);
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

  // Scans a page's content stream once for thin shapes (stroked lines or filled bars) of any
  // width - candidate ruled lines. Cached per page (see ruledLineCandidatesByPage in fillForm) so
  // a page with several multiline fields only pays for content-stream parsing once, not per field.
  // Each candidate is {loX, hiX, midY}.
  private List<float[]> findThinWideShapes(PdfPage page) {
    final List<float[]> candidates = new ArrayList<>();
    final float maxRuleThickness = 2f;

    IEventListener listener = new IEventListener() {
      @Override
      public void eventOccurred(IEventData data, EventType type) {
        if (type != EventType.RENDER_PATH) {
          return;
        }
        PathRenderInfo renderInfo = (PathRenderInfo) data;
        if (renderInfo.getOperation() == PathRenderInfo.NO_OP) {
          return;
        }
        Matrix ctm = renderInfo.getCtm();
        for (Subpath subpath : renderInfo.getPath().getSubpaths()) {
          List<Point> points = subpath.getPiecewiseLinearApproximation();
          if (points.size() < 2) {
            continue;
          }
          float loX = Float.MAX_VALUE;
          float hiX = -Float.MAX_VALUE;
          float loY = Float.MAX_VALUE;
          float hiY = -Float.MAX_VALUE;
          for (Point pt : points) {
            Point transformed = transformPoint(pt, ctm);
            loX = Math.min(loX, (float) transformed.getX());
            hiX = Math.max(hiX, (float) transformed.getX());
            loY = Math.min(loY, (float) transformed.getY());
            hiY = Math.max(hiY, (float) transformed.getY());
          }
          if ((hiY - loY) > maxRuleThickness) {
            continue;
          }
          candidates.add(new float[] { loX, hiX, (loY + hiY) / 2f });
        }
      }

      @Override
      public Set<EventType> getSupportedEvents() {
        return Collections.singleton(EventType.RENDER_PATH);
      }
    };

    new PdfCanvasProcessor(listener).processPageContent(page);
    return candidates;
  }

  // Filters a page's pre-computed candidate shapes down to the ones that sit inside the given
  // field's rectangle - the pre-printed ruled lines of that field - and returns the median
  // vertical spacing between them. Returns null when no consistent set of ruled lines is found,
  // so the caller can fall back to the MULTILINE_LINE_HEIGHT default.
  private Float detectRuledLineSpacing(List<float[]> candidates, Rectangle fieldsRect) {
    final float minX = fieldsRect.getLeft();
    final float maxX = fieldsRect.getRight();
    final float minY = fieldsRect.getBottom() - 2f;
    final float maxY = fieldsRect.getTop() + 2f;
    final float minRuleWidth = fieldsRect.getWidth() * 0.5f;

    List<Float> ruleYs = new ArrayList<>();
    for (float[] c : candidates) {
      float loX = c[0];
      float hiX = c[1];
      float midY = c[2];
      if ((hiX - loX) < minRuleWidth) {
        continue;
      }
      if (midY < minY || midY > maxY || hiX < minX || loX > maxX) {
        continue;
      }
      ruleYs.add(midY);
    }

    if (ruleYs.isEmpty()) {
      return null;
    }
    Collections.sort(ruleYs);
    List<Float> dedup = new ArrayList<>();
    for (float y : ruleYs) {
      if (dedup.isEmpty() || Math.abs(y - dedup.get(dedup.size() - 1)) > 1f) {
        dedup.add(y);
      }
    }
    // Need at least 3 detected rules so the median gap is meaningful, not a single guess.
    if (dedup.size() < 3) {
      return null;
    }

    List<Float> gaps = new ArrayList<>();
    for (int idx = 1; idx < dedup.size(); idx++) {
      gaps.add(dedup.get(idx) - dedup.get(idx - 1));
    }
    List<Float> sortedGaps = new ArrayList<>(gaps);
    Collections.sort(sortedGaps);
    float median = sortedGaps.get(sortedGaps.size() / 2);
    if (median <= 0f) {
      return null;
    }

    // Require most gaps to agree with the median before trusting it - otherwise what we detected
    // is unlikely to be an evenly-spaced ruled grid (could be unrelated borders/underlines).
    int consistent = 0;
    for (float g : gaps) {
      if (Math.abs(g - median) <= median * 0.25f) {
        consistent++;
      }
    }
    if (consistent < gaps.size() * 0.6f) {
      return null;
    }

    return median;
  }

  private static Point transformPoint(Point p, Matrix m) {
    double x = p.getX();
    double y = p.getY();
    double newX = x * m.get(Matrix.I11) + y * m.get(Matrix.I21) + m.get(Matrix.I31);
    double newY = x * m.get(Matrix.I12) + y * m.get(Matrix.I22) + m.get(Matrix.I32);
    return new Point(newX, newY);
  }

  private void addTextToCanvas(PdfPage page, PdfDocument pdf, Rectangle fieldsRect, Paragraph p) {
    PdfCanvas canvas = new PdfCanvas(page);
    try (Canvas cvs = new Canvas(canvas, fieldsRect)) {
      cvs.add(p);
      pdf = cvs.getPdfDocument();
    }
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

  // lineHeight is fixed (either the default or a detected ruled-line spacing), not tied to font
  // size, so a value whose explicit line breaks alone need more lines than the field can hold at
  // that lineHeight makes the fit-check in doesTextFitWithNewlines impossible at any font size -
  // it always falls back to the 1pt floor. When that happens, tighten the leading to exactly what
  // the forced line count needs so those lines can still render at a legible size, instead of
  // discarding the line breaks or collapsing the font.
  private float tightenLineHeightForForcedLines(String value, float usableHeight, float lineHeight) {
    if (value == null || (value.indexOf('\n') < 0 && value.indexOf('\r') < 0)) {
      return lineHeight;
    }
    String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
    int forcedLines = normalized.split("\n", -1).length;
    if (forcedLines <= 1 || forcedLines * lineHeight <= usableHeight) {
      return lineHeight;
    }
    return usableHeight / forcedLines;
  }

  private float getDynamicMultiLineFontSize(String value, Rectangle fieldsRect, PdfFont font, float lineHeight) {
    float maxFontSize = MULTILINE_BASE_FONT_SIZE;
    float minFontSize = 1f;
    float usableWidth = fieldsRect.getWidth() - 8f;
    float usableHeight = fieldsRect.getHeight() - 8f;

    if (value == null || value.trim().isEmpty()) {
      return maxFontSize;
    }

    String[] lines = value.split("\n", -1);
    float spaceWidthAt1Pt = font.getWidth(" ", 1f);
    float bestFontSize = minFontSize;

    // Binary search
    for (int i = 0; i < 15; i++) {
      float testSize = (minFontSize + maxFontSize) / 2f;

      if (doesTextFitWithNewlines(lines, testSize, usableWidth, usableHeight, font, spaceWidthAt1Pt, lineHeight)) {
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

  private boolean doesTextFitWithNewlines(String[] lines, float fontSize, float usableWidth, float usableHeight, PdfFont font, float spaceWidthAt1Pt, float lineHeight) {
    int lineCount = 0;

    for (String line : lines) {
      if (line.trim().isEmpty()) {
        lineCount++;
        continue;
      }
      String[] words = line.split("\\s+");
      float currentLineWidth = 0f;
      boolean firstWord = true;

      for (String word : words) {
        if (word.isEmpty()) continue;
        float wordWidth = font.getWidth(word, 1f) * fontSize;
        float spaceWidth = spaceWidthAt1Pt * fontSize;

        if (wordWidth > usableWidth) {
          return false;
        }

        if (firstWord) {
          currentLineWidth = wordWidth;
          firstWord = false;
        } else if (currentLineWidth + spaceWidth + wordWidth > usableWidth) {
          lineCount++;
          currentLineWidth = wordWidth;
        } else {
          currentLineWidth += spaceWidth + wordWidth;
        }
      }
      lineCount++; // end of this explicit line
    }

    return (lineCount * lineHeight) <= usableHeight;
  }

  public File fillFormWithExtras(
      JSONArray formArray, String pdfTemplate, JSONArray customFields,
      JSONArray extraPages, String outputName
  ) throws Exception {
    File mainFile = File.createTempFile(outputName + "_main", ".pdf");
    fillForm(formArray, pdfTemplate, customFields, mainFile, outputName);

    if (extraPages == null || extraPages.isEmpty()) {
      return mainFile;
    }

    // Pre-generate all extra page PDFs, grouped by their own duplicatable_page
    Map<Integer, List<File>> epFilesByPage = new TreeMap<>();
    for (Object ep : extraPages) {
      JSONObject epObj = (JSONObject) ep;
      JSONArray epFormArray = getFormArray(epObj.get("form_data"));
      String epTemplate = epObj.get("template_path").toString();
      int dupPage = epObj.get("duplicatable_page") != null
          ? ((Long) epObj.get("duplicatable_page")).intValue() : 1;

      File epFile = File.createTempFile(outputName + "_ep", ".pdf");
      fillForm(epFormArray, epTemplate, new JSONArray(), epFile, outputName + "_ep");
      epFilesByPage.computeIfAbsent(dupPage, k -> new ArrayList<>()).add(epFile);
    }

    // Build combined PDF, splicing each group of extra pages in after its own source page
    File combinedFile = File.createTempFile(outputName + "_combined", ".pdf");
    PdfDocument combined = new PdfDocument(new PdfWriter(combinedFile.getAbsolutePath()));
    PdfMerger merger = new PdfMerger(combined);

    PdfDocument mainSrc = new PdfDocument(new PdfReader(mainFile));
    int totalMainPages = mainSrc.getNumberOfPages();

    int cursor = 1;
    for (Map.Entry<Integer, List<File>> group : epFilesByPage.entrySet()) {
      int dupPage = group.getKey();
      merger.merge(mainSrc, cursor, dupPage);

      for (File epFile : group.getValue()) {
        PdfDocument epSrc = new PdfDocument(new PdfReader(epFile));
        merger.merge(epSrc, dupPage, dupPage);
        epSrc.close();
        epFile.delete();
      }

      cursor = dupPage + 1;
    }

    if (cursor <= totalMainPages) {
      merger.merge(mainSrc, cursor, totalMainPages);
    }

    mainSrc.close();
    combined.close();
    mainFile.delete();
    return combinedFile;
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
