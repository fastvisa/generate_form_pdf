package com.fastvisa.manipulatepdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Receipt Model Tests")
class ReceiptTest {

    private Receipt receipt;
    private Map<String, String> formData;

    @BeforeEach
    void setUp() {
        formData = new HashMap<>();
        formData.put("field1", "value1");
        formData.put("field2", "value2");
        receipt = new Receipt();
    }

    @Test
    @DisplayName("Should create Receipt with default constructor")
    void shouldCreateReceiptWithDefaultConstructor() {
        Receipt newReceipt = new Receipt();

        assertThat(newReceipt.getForm_data()).isNull();
        assertThat(newReceipt.getOutput_name()).isNull();
        assertThat(newReceipt.getReceipt_type()).isNull();
        assertThat(newReceipt.getUrl_download()).isNull();
        assertThat(newReceipt.getStatus()).isNull();
    }

    @Test
    @DisplayName("Should create Receipt with three parameters")
    void shouldCreateReceiptWithThreeParameters() {
        Object formData = new Object();
        String outputName = "receipt_123";
        String receiptType = "Receipt";

        Receipt newReceipt = new Receipt(formData, outputName, receiptType);

        assertThat(newReceipt.getForm_data()).isSameAs(formData);
        assertThat(newReceipt.getOutput_name()).isEqualTo(outputName);
        assertThat(newReceipt.getReceipt_type()).isEqualTo(receiptType);
        assertThat(newReceipt.getUrl_download()).isNull();
        assertThat(newReceipt.getStatus()).isNull();
    }

    @Test
    @DisplayName("Should create Receipt with all five parameters")
    void shouldCreateReceiptWithAllFiveParameters() {
        Object formData = new Object();
        String outputName = "receipt_123";
        String receiptType = "Receipt";
        String urlDownload = "https://example.com/receipt.pdf";
        String status = "success";

        Receipt newReceipt = new Receipt(formData, outputName, receiptType, urlDownload, status);

        assertThat(newReceipt.getForm_data()).isSameAs(formData);
        assertThat(newReceipt.getOutput_name()).isEqualTo(outputName);
        assertThat(newReceipt.getReceipt_type()).isEqualTo(receiptType);
        assertThat(newReceipt.getUrl_download()).isEqualTo(urlDownload);
        assertThat(newReceipt.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Should set and get form_data")
    void shouldSetAndGetFormData() {
        Map<String, String> newFormData = new HashMap<>();
        newFormData.put("newField", "newValue");

        receipt.setForm_data(newFormData);

        assertThat(receipt.getForm_data()).isSameAs(newFormData);
    }

    @Test
    @DisplayName("Should set and get receipt_type")
    void shouldSetAndGetReceiptType() {
        receipt.setReceipt_type("Addendum");

        assertThat(receipt.getReceipt_type()).isEqualTo("Addendum");
    }

    @Test
    @DisplayName("Should set and get output_name")
    void shouldSetAndGetOutputName() {
        receipt.setOutput_name("output_test_456");

        assertThat(receipt.getOutput_name()).isEqualTo("output_test_456");
    }

    @Test
    @DisplayName("Should set and get url_download")
    void shouldSetAndGetUrlDownload() {
        String url = "https://s3.amazonaws.com/bucket/file.pdf";
        receipt.setUrl_download(url);

        assertThat(receipt.getUrl_download()).isEqualTo(url);
    }

    @Test
    @DisplayName("Should set and get status")
    void shouldSetAndGetStatus() {
        receipt.setStatus("failed");

        assertThat(receipt.getStatus()).isEqualTo("failed");
    }

    @Test
    @DisplayName("Should handle null form_data")
    void shouldHandleNullFormData() {
        receipt.setForm_data(null);

        assertThat(receipt.getForm_data()).isNull();
    }

    @Test
    @DisplayName("Should handle empty receipt_type")
    void shouldHandleEmptyReceiptType() {
        receipt.setReceipt_type("");

        assertThat(receipt.getReceipt_type()).isEmpty();
    }

    @Test
    @DisplayName("Should handle complex form_data")
    void shouldHandleComplexFormData() {
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("stringField", "value");
        complexData.put("numberField", 123);
        complexData.put("booleanField", true);

        receipt.setForm_data(complexData);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) receipt.getForm_data();
        assertThat(result).hasSize(3);
        assertThat(result.get("stringField")).isEqualTo("value");
        assertThat(result.get("numberField")).isEqualTo(123);
        assertThat(result.get("booleanField")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should support multiple setters")
    void shouldSupportMultipleSetters() {
        receipt.setForm_data(formData);
        receipt.setReceipt_type("Receipt");
        receipt.setOutput_name("test_output");
        receipt.setUrl_download("test_url");
        receipt.setStatus("success");

        assertThat(receipt.getForm_data()).isSameAs(formData);
        assertThat(receipt.getReceipt_type()).isEqualTo("Receipt");
        assertThat(receipt.getOutput_name()).isEqualTo("test_output");
        assertThat(receipt.getUrl_download()).isEqualTo("test_url");
        assertThat(receipt.getStatus()).isEqualTo("success");
    }
}
