package com.fastvisa.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.context.Context;

import com.fastvisa.manipulatepdf.Receipt;

@Service
public class ReceiptService {

  @Autowired
  private SpringTemplateEngine templateEngine;

  public void generateInvoice(Receipt receipt, FileOutputStream file) throws IOException {
    templateEngine = new SpringTemplateEngine();
    templateEngine.addTemplateResolver(htmlTemplateResolver());

    Context context = new Context();
    context.setVariable("receiptEntry", receipt);
    String html = templateEngine.process("receipt", context);

    ConverterProperties converterProperties = new ConverterProperties();
    HtmlConverter.convertToPdf(html, file, converterProperties);
  }

  public ITemplateResolver htmlTemplateResolver(){
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
    return templateResolver;
  }

}
