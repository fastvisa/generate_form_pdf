package com.fastvisa.services;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.font.FontProvider;

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

        FontProvider fontProvider = new FontProvider();
        fontProvider.addFont(StandardFonts.COURIER);
        fontProvider.addFont(StandardFonts.COURIER_BOLD);

        ConverterProperties properties = new ConverterProperties();
        properties.setFontProvider(fontProvider);

        HtmlConverter.convertToPdf(html, file, properties);
        break;

      case "Receipt":
        context.setVariable("receiptEntry", receipt);
        html = templateEngine.process("receipt", context);
        HtmlConverter.convertToPdf(html, file);
        break;

      case "Questionnaire":
        context.setVariable("view", new QuestionnaireView(receipt.getForm_data()));
        context.setVariable("logoDataUri", logoDataUri());
        html = templateEngine.process("questionnaire", context);

        ConverterProperties questionnaireProperties = new ConverterProperties();
        questionnaireProperties.setFontProvider(questionnaireFontProvider());

        HtmlConverter.convertToPdf(html, file, questionnaireProperties);
        break;

      default:
        break;
    }
  }

  private static final String[] UNICODE_FONT_FILES = {
    "NotoSans-Regular.ttf", "NotoSans-Bold.ttf", "NotoSans-Italic.ttf",
    "NotoSansSC-Regular.ttf", "NotoSansSC-Regular.otf", "NotoSansSC-Bold.ttf", "NotoSansSC-Bold.otf"
  };

  FontProvider questionnaireFontProvider() throws IOException {
    FontProvider fontProvider = new FontProvider();
    fontProvider.addFont(StandardFonts.HELVETICA);
    fontProvider.addFont(StandardFonts.HELVETICA_BOLD);
    fontProvider.addFont(StandardFonts.HELVETICA_OBLIQUE);

    for (String name : UNICODE_FONT_FILES) {
      try (InputStream in = ReceiptService.class.getResourceAsStream("/fonts/" + name)) {
        if (in != null) {
          fontProvider.addFont(FontProgramFactory.createFont(readAll(in)));
        }
      }
    }
    return fontProvider;
  }

  String logoDataUri() throws IOException {
    try (InputStream in = ReceiptService.class.getResourceAsStream("/static/logo-fastvisa-2.png")) {
      if (in == null) {
        return null;
      }
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(readAll(in));
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int read;
    while ((read = in.read(chunk)) != -1) {
      buffer.write(chunk, 0, read);
    }
    return buffer.toByteArray();
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
