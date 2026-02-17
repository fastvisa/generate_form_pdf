package com.fastvisa.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Iterator;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CombineFormService {
  
  private FillFormService fillFormService;
  private PdfUtilityService pdfUtilityService;

  public CombineFormService() {
    this.fillFormService = new FillFormService();
    this.pdfUtilityService = new PdfUtilityService();
  }

  public void combineForm(File combined_file, JSONArray pdf_array) throws IOException, ParseException, org.json.simple.parser.ParseException {
    PdfDocument pdf = new PdfDocument(new PdfWriter(combined_file));
    PdfMerger merger = new PdfMerger(pdf);
    JSONArray form_array = new JSONArray();
    JSONArray structure_input_array = new JSONArray();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    int mergedPdfCount = 0;

    Iterator<?> i = pdf_array.iterator();
    while (i.hasNext()) {
      JSONObject innerObj = (JSONObject) i.next();
      Object form_data = innerObj.get("form_data");
      Object structure_inputs = innerObj.get("structure_inputs");
      Object pdfTemplateObj = innerObj.get("pdf_template");
      Object templatePathObj = innerObj.get("template_path");      
      String pdf_template = (pdfTemplateObj != null) ? pdfTemplateObj.toString() : templatePathObj.toString();
      String output_name = String.valueOf(timestamp.getTime()) + "_" + mergedPdfCount;

      File file = File.createTempFile(output_name, "pdf");

      try {
        if (form_data != null) {
          form_array = fillFormService.getFormArray(form_data);
          structure_input_array = fillFormService.getStructureInputArray(structure_inputs);
          System.out.println("Filling form with " + form_array.size() + " fields");
          fillFormService.fillForm(form_array, pdf_template, structure_input_array, file, output_name);
        } else {
          System.out.println("No form data, copying template as-is");
          String actualPdfPath = pdfUtilityService.getPdfTemplatePath(pdf_template);
          Files.copy(new File(actualPdfPath).toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        PdfDocument sourcePdf = new PdfDocument(new PdfReader(file));
        int pageCount = sourcePdf.getNumberOfPages();
        merger.merge(sourcePdf, 1, pageCount);
        sourcePdf.close();
        mergedPdfCount++;
        
      } catch (Exception e) {
        System.err.println("Error processing PDF: " + e.getMessage());
        e.printStackTrace();
        if (file.exists()) {
          file.delete();
        }
        continue;
      } finally {
        if (file.exists()) {
          file.delete();
        }
      }
    }
    
    System.out.println("Total PDFs merged: " + mergedPdfCount);
    
    if (mergedPdfCount == 0) {
      pdf.close();
      throw new IOException("No PDFs were successfully merged. The combined file will be blank.");
    }
    
    pdf.close();
    System.out.println("Combined PDF created successfully at: " + combined_file.getAbsolutePath());
  }

  public JSONArray getFormArray(Object form_data) throws IOException, ParseException, org.json.simple.parser.ParseException {
    return fillFormService.getFormArray(form_data);
  }

}
