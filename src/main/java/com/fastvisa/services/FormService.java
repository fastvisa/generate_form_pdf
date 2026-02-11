package com.fastvisa.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  
  /**
   * Downloads a PDF file from a URL and returns the local file path
   * If the path is already a local file, returns it as-is
   */
  private String getPdfTemplatePath(String pdf_template) throws IOException {
    // Check if it's a URL
    if (pdf_template.startsWith("http://") || pdf_template.startsWith("https://")) {
      // Download the PDF to a temporary file
      String tempFileName = "template_" + System.currentTimeMillis() + ".pdf";
      Path tempFile = Files.createTempFile(tempFileName, ".pdf");
      
      String currentUrl = pdf_template;
      int maxRedirects = 10;
      int redirectCount = 0;
      
      while (redirectCount < maxRedirects) {
        URL url = new URL(currentUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set user agent to avoid being blocked
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setInstanceFollowRedirects(false); // We'll handle redirects manually
        
        // Connect and get response code
        connection.connect();
        int responseCode = connection.getResponseCode();
        String contentType = connection.getContentType();
        
        System.out.println("Downloading PDF from URL: " + currentUrl);
        System.out.println("Response code: " + responseCode);
        System.out.println("Content-Type: " + contentType);
        
        // Check for redirects
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
          
          // Get the redirect location
          String redirectUrl = connection.getHeaderField("Location");
          if (redirectUrl != null && !redirectUrl.isEmpty()) {
            System.out.println("Redirecting to: " + redirectUrl);
            currentUrl = redirectUrl;
            redirectCount++;
            connection.disconnect();
            continue;
          }
        }
        
        // If not a redirect, check if we got a successful response
        if (responseCode != HttpURLConnection.HTTP_OK) {
          throw new IOException("Failed to download PDF. HTTP response code: " + responseCode);
        }
        
        // Check if content type is PDF
        if (contentType == null || !contentType.toLowerCase().contains("application/pdf")) {
          // Try to extract redirect URL from HTML if it's a redirect page
          if (contentType != null && contentType.toLowerCase().contains("text/html")) {
            String htmlContent = readStreamToString(connection.getInputStream());
            String extractedUrl = extractRedirectUrlFromHtml(htmlContent);
            if (extractedUrl != null) {
              System.out.println("Extracted redirect URL from HTML: " + extractedUrl);
              currentUrl = extractedUrl;
              redirectCount++;
              connection.disconnect();
              continue;
            }
          }
          System.err.println("WARNING: Content-Type is not PDF: " + contentType);
          System.err.println("URL might be a redirect or returning HTML instead of PDF");
        }
        
        // Download the file
        try (InputStream in = connection.getInputStream()) {
          Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        connection.disconnect();
        
        // Validate the downloaded file is actually a PDF
        byte[] header = new byte[4];
        try (InputStream checkStream = Files.newInputStream(tempFile)) {
          int bytesRead = checkStream.read(header);
          if (bytesRead >= 4) {
            String headerStr = new String(header);
            if (!headerStr.equals("%PDF")) {
              throw new IOException("Downloaded file is not a valid PDF. Header: " + headerStr);
            }
          }
        }
        
        String tempFilePath = tempFile.toString();
        System.out.println("Successfully downloaded PDF template to: " + tempFilePath);
        return tempFilePath;
      }
      
      throw new IOException("Too many redirects (max: " + maxRedirects + ")");
    }
    
    // It's already a local file path
    return pdf_template;
  }
  
  /**
   * Reads an input stream to a string
   */
  private String readStreamToString(InputStream inputStream) throws IOException {
    StringBuilder result = new StringBuilder();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.append(new String(buffer, 0, length));
    }
    return result.toString();
  }
  
  /**
   * Extracts redirect URL from HTML content
   */
  private String extractRedirectUrlFromHtml(String html) {
    // Pattern to match meta refresh redirects
    Pattern metaRefreshPattern = Pattern.compile(
      "<meta\\s+http-equiv\\s*=\\s*[\"']refresh[\"']\\s+content\\s*=\\s*[\"']([^\"']+)[\"']",
      Pattern.CASE_INSENSITIVE
    );
    Matcher matcher = metaRefreshPattern.matcher(html);
    if (matcher.find()) {
      String content = matcher.group(1);
      // Extract URL from content like "0;url=http://example.com"
      Pattern urlPattern = Pattern.compile("url\\s*=\\s*([^\\s;]+)", Pattern.CASE_INSENSITIVE);
      Matcher urlMatcher = urlPattern.matcher(content);
      if (urlMatcher.find()) {
        return urlMatcher.group(1);
      }
    }
    
    // Pattern to match JavaScript redirects
    Pattern jsRedirectPattern = Pattern.compile(
      "window\\.location\\s*=\\s*[\"']([^\"']+)[\"']",
      Pattern.CASE_INSENSITIVE
    );
    matcher = jsRedirectPattern.matcher(html);
    if (matcher.find()) {
      return matcher.group(1);
    }
    
    return null;
  }
  
  public void fillForm(JSONArray form_array, String pdf_template, JSONArray structure_input_array, File file, String output_name) throws IOException {
    String output_file = file.getAbsolutePath();
    
    // Handle URL-based templates by downloading them first
    String actualPdfPath = getPdfTemplatePath(pdf_template);
    
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
      Object pdfTemplateObj = innerObj.get("pdf_template");
      if (pdfTemplateObj == null) {
        continue;
      }
      String pdf_template = pdfTemplateObj.toString();
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

  public JSONArray getStructureInputArray(Object structure_inputs) throws IOException, ParseException, org.json.simple.parser.ParseException {
    JSONParser jsonParser = new JSONParser();
    Object structure_input_object = jsonParser.parse(gson.toJson(structure_inputs));
    JSONArray structure_input_array = (JSONArray) structure_input_object;
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
    if (splitted_array == null || chunkSize <= 0) {
      return new String[0];
    }
    
    int numOfChunks = (int) Math.ceil((double) splitted_array.length / chunkSize);
    String[] output = new String[chunkSize];

    int index = 0;
    for(int i = 0; i < splitted_array.length && index < chunkSize; i += numOfChunks) {
      String[] chunk_array = Arrays.copyOfRange(splitted_array, i, Math.min(splitted_array.length, i + numOfChunks));
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

}