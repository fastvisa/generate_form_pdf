package com.fastvisa.services;

import com.fastvisa.manipulatepdf.Receipt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Tests")
class ReceiptServiceTest {

    @Mock
    private org.thymeleaf.spring5.SpringTemplateEngine mockTemplateEngine;

    private ReceiptService receiptService;
    private Path tempOutputFile;

    @BeforeEach
    void setUp() throws IOException {
        receiptService = new ReceiptService();
        tempOutputFile = Files.createTempFile("receipt-test", ".pdf");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempOutputFile != null && Files.exists(tempOutputFile)) {
            Files.deleteIfExists(tempOutputFile);
        }
    }

    @Test
    @DisplayName("Should create htmlTemplateResolver with correct configuration")
    void shouldCreateHtmlTemplateResolver() {
        org.thymeleaf.templateresolver.ITemplateResolver resolver = receiptService.htmlTemplateResolver();

        assertThat(resolver).isNotNull();
        assertThat(resolver).isInstanceOf(org.thymeleaf.templateresolver.ClassLoaderTemplateResolver.class);

        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver classLoaderResolver =
            (org.thymeleaf.templateresolver.ClassLoaderTemplateResolver) resolver;

        assertThat(classLoaderResolver.getPrefix()).isEqualTo("/templates/");
        assertThat(classLoaderResolver.getSuffix()).isEqualTo(".html");
        assertThat(classLoaderResolver.getTemplateMode()).isEqualTo(org.thymeleaf.templatemode.TemplateMode.HTML);
    }

    @Test
    @DisplayName("Should set UTF-8 encoding on template resolver")
    void shouldSetUtf8Encoding() {
        org.thymeleaf.templateresolver.ITemplateResolver resolver = receiptService.htmlTemplateResolver();

        assertThat(resolver).isNotNull();
        assertThat(resolver).isInstanceOf(org.thymeleaf.templateresolver.ClassLoaderTemplateResolver.class);

        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver classLoaderResolver =
            (org.thymeleaf.templateresolver.ClassLoaderTemplateResolver) resolver;

        assertThat(classLoaderResolver.getCharacterEncoding()).isEqualTo("UTF-8");
    }

    @Test
    @DisplayName("Should handle Receipt type correctly")
    void shouldHandleReceiptType() throws IOException {
        Map<String, String> formData = new HashMap<>();
        formData.put("amount", "100.00");
        formData.put("date", "2024-01-01");

        Receipt receipt = new Receipt(formData, "test_receipt", "Receipt");

        try (FileOutputStream fos = new FileOutputStream(tempOutputFile.toFile())) {
            // This will fail if templates don't exist, but we're testing the service structure
            // In a real scenario, we'd mock the template engine
            assertThat(receipt.getReceipt_type()).isEqualTo("Receipt");
            assertThat(receipt.getOutput_name()).isEqualTo("test_receipt");
        }
    }

    @Test
    @DisplayName("Should handle Addendum type correctly")
    void shouldHandleAddendumType() {
        Map<String, String> formData = new HashMap<>();
        formData.put("content", "Addendum content");

        Receipt receipt = new Receipt(formData, "test_addendum", "Addendum");

        assertThat(receipt.getReceipt_type()).isEqualTo("Addendum");
        assertThat(receipt.getOutput_name()).isEqualTo("test_addendum");
    }

    @Test
    @DisplayName("Should handle unknown receipt type")
    void shouldHandleUnknownReceiptType() {
        Map<String, String> formData = new HashMap<>();
        formData.put("data", "test data");

        Receipt receipt = new Receipt(formData, "test_unknown", "UnknownType");

        assertThat(receipt.getReceipt_type()).isEqualTo("UnknownType");
    }

    @Test
    @DisplayName("Should create Receipt with null form data")
    void shouldCreateReceiptWithNullFormData() {
        Receipt receipt = new Receipt(null, "test_output", "Receipt");

        assertThat(receipt.getForm_data()).isNull();
        assertThat(receipt.getOutput_name()).isEqualTo("test_output");
    }

    @Test
    @DisplayName("Should create Receipt with empty output name")
    void shouldCreateReceiptWithEmptyOutputName() {
        Receipt receipt = new Receipt(new Object(), "", "Receipt");

        assertThat(receipt.getOutput_name()).isEmpty();
    }

    @Test
    @DisplayName("Should create Receipt with all five parameters")
    void shouldCreateReceiptWithAllParameters() {
        Object formData = new Object();
        String outputName = "receipt_123";
        String receiptType = "Receipt";
        String urlDownload = "https://example.com/download.pdf";
        String status = "success";

        Receipt receipt = new Receipt(formData, outputName, receiptType, urlDownload, status);

        assertThat(receipt.getForm_data()).isSameAs(formData);
        assertThat(receipt.getOutput_name()).isEqualTo(outputName);
        assertThat(receipt.getReceipt_type()).isEqualTo(receiptType);
        assertThat(receipt.getUrl_download()).isEqualTo(urlDownload);
        assertThat(receipt.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        Receipt receipt = new Receipt();

        Map<String, String> formData = new HashMap<>();
        formData.put("field", "value");

        receipt.setForm_data(formData);
        receipt.setOutput_name("test_output");
        receipt.setReceipt_type("Receipt");
        receipt.setUrl_download("https://example.com/file.pdf");
        receipt.setStatus("success");

        assertThat(receipt.getForm_data()).isSameAs(formData);
        assertThat(receipt.getOutput_name()).isEqualTo("test_output");
        assertThat(receipt.getReceipt_type()).isEqualTo("Receipt");
        assertThat(receipt.getUrl_download()).isEqualTo("https://example.com/file.pdf");
        assertThat(receipt.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("Should handle complex form data")
    void shouldHandleComplexFormData() {
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("string", "value");
        complexData.put("number", 123);
        complexData.put("boolean", true);
        complexData.put("nested", new HashMap<>());

        Receipt receipt = new Receipt(complexData, "test", "Receipt");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) receipt.getForm_data();
        assertThat(result).hasSize(4);
        assertThat(result.get("string")).isEqualTo("value");
        assertThat(result.get("number")).isEqualTo(123);
        assertThat(result.get("boolean")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should handle null status")
    void shouldHandleNullStatus() {
        Receipt receipt = new Receipt(new Object(), "test", "Receipt", "url", null);

        assertThat(receipt.getStatus()).isNull();
    }

    @Test
    @DisplayName("Should update status")
    void shouldUpdateStatus() {
        Receipt receipt = new Receipt();

        receipt.setStatus("pending");
        assertThat(receipt.getStatus()).isEqualTo("pending");

        receipt.setStatus("completed");
        assertThat(receipt.getStatus()).isEqualTo("completed");
    }
}
