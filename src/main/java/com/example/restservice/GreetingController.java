package com.example.restservice;

import java.io.*;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.xfa.XfaForm;

@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@GetMapping("/greeting")
	public Greeting greeting_1(@RequestParam(value = "name", defaultValue = "World") String name) {
		System.out.println(name);
		return new Greeting(counter.incrementAndGet(), String.format(template, name), 1);
	}

	@PostMapping(path = "/mashok", consumes = "application/json", produces = "application/json")
	public Request request(@RequestBody String bodyParameter) {
		System.out.println(bodyParameter);

		Gson gson = new Gson();
		Request g = gson.fromJson(bodyParameter, Request.class);

		System.out.println(g.getPath());
		return new Request(g.getName(), String.format(template, g.getPath()));
	} 

	@PostMapping(path = "/generate_pdf", consumes = "application/json", produces = "application/json")
	public Request generate_pdf(@RequestBody String bodyParameter) throws Exception {
		Gson gson = new Gson();
		Request g = gson.fromJson(bodyParameter, Request.class);

		String form_data = "/mnt/c/Users/Einherjar/Work/rest-service/N-648.csv";
		System.out.println(form_data); 
		// String template_file = "/mnt/c/Users/Einherjar/Work/rest-service/pdf_n-648_20210531.pdf";
		String template_file = String.format("/mnt/c/Users/Einherjar/Work/fast-visa/lib/pdf_templates/%s.pdf", g.getPath());
    // String template_file = "/Users/byciikel/Software Engineer/Ruby/fast-visa/lib/java/itext/n-648-kotor.pdf";
		String output_file = String.format("result/%s.pdf", g.getName());
    // String output_file = "result/out_pdf_n-648.pdf";
    File file = new File(output_file);
    file.getParentFile().mkdirs();
		// new UscisFill().manipulatePdf(form_data, template_file, output_file);
		PdfReader reader = new PdfReader(template_file);
    reader.setUnethicalReading(true);
    PdfDocument pdf = new PdfDocument(reader, new PdfWriter(output_file));
    removeUsageRights(pdf);

    PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);
    XfaForm xfa = form.getXfaForm();
    
    Map<String, PdfFormField> fields = form.getFormFields();
    BufferedReader csvReader = new BufferedReader(new FileReader(form_data));
    String row;
    while ((row = csvReader.readLine()) != null) {
      String[] data = row.split("\t");
      if (fields.get(data[0]) != null) {
        xfa.setXfaFieldValue(data[0], data[1]);
        fields.get(data[0])
        .setBorderWidth(3)
        .setFontSize((float) 6.5)
        .setValue(data[1]);
      }
    }
    // form.flattenFields();
    xfa.write(pdf);
    csvReader.close();
		pdf.close();

		System.out.println(g.getPath());
		return new Request(g.getName(), String.format(template, g.getPath()));
	}

	public void removeUsageRights(PdfDocument pdfDoc) {
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

}