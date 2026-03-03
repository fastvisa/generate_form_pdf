package com.fastvisa.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility service for PDF operations used by FillFormService and CombineFormService
 */
public class PdfUtilityService {

  /**
   * Downloads a PDF file from a URL and returns the local file path
   * If the path is already a local file, returns it as-is
   */
  public String getPdfTemplatePath(String pdf_template) throws IOException {
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
  public String readStreamToString(InputStream inputStream) throws IOException {
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
  public String extractRedirectUrlFromHtml(String html) {
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
}
