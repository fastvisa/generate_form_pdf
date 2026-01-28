package com.fastvisa.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

  public void generateReceipt(Receipt receipt, FileOutputStream file) throws IOException {
    templateEngine = new SpringTemplateEngine();
    templateEngine.addTemplateResolver(htmlTemplateResolver());

    Context context = new Context();
    String receipt_type = receipt.getReceipt_type();
    String html = "";

    switch(receipt_type){
      case "Addendum":
        context.setVariable("addendum", receipt);
        html = templateEngine.process("addendum", context);
        break;
      case "Receipt":
        context.setVariable("receiptEntry", receipt);
        html = templateEngine.process("receipt", context);
        break;
      default:
        break;
    }

    HtmlConverter.convertToPdf(html, file);
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
