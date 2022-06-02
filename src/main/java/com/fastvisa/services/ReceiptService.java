package com.fastvisa.services;

import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import com.fastvisa.manipulatepdf.Receipt;

@Service
public class ReceiptService {

  @Autowired
  private SpringTemplateEngine templateEngine;

  public void generateInvoice(Receipt receipt, FileOutputStream file) throws IOException {
    this.templateEngine = new SpringTemplateEngine();

    Context context = new Context();
    context.setVariable("receiptEntry", receipt);
    String html = this.templateEngine.process("receipt", context);

    ConverterProperties converterProperties = new ConverterProperties();
    converterProperties.setBaseUri("http://localhost:8080"); // get css

    HtmlConverter.convertToPdf(html, file, converterProperties);
  }

}
