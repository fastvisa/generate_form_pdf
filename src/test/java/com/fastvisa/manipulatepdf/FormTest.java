package com.fastvisa.manipulatepdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Form Model Tests")
class FormTest {

    @Test
    @DisplayName("Should create Form with all parameters")
    void shouldCreateFormWithAllParameters() {
        Object pdfData = new Object();
        Object formData = new HashMap<>();
        String pdfTemplate = "/path/to/template.pdf";
        Object customFields = Arrays.asList("input1", "input2");
        String urlDownload = "https://example.com/download.pdf";

        Form form = new Form(pdfData, formData, pdfTemplate, customFields, urlDownload);

        assertThat(form.pdfData()).isSameAs(pdfData);
        assertThat(form.formData()).isSameAs(formData);
        assertThat(form.templatePath()).isEqualTo(pdfTemplate);
        assertThat(form.customFields()).isSameAs(customFields);
        assertThat(form.getUrl()).isEqualTo(urlDownload);
    }

    @Test
    @DisplayName("Should create Form with null values")
    void shouldCreateFormWithNullValues() {
        Form form = new Form(null, null, null, null, null);

        assertThat(form.pdfData()).isNull();
        assertThat(form.formData()).isNull();
        assertThat(form.templatePath()).isNull();
        assertThat(form.customFields()).isNull();
        assertThat(form.getUrl()).isNull();
    }

    @Test
    @DisplayName("Should create Form with empty string for template")
    void shouldCreateFormWithEmptyTemplate() {
        Form form = new Form(null, null, "", null, "");

        assertThat(form.templatePath()).isEmpty();
        assertThat(form.getUrl()).isEmpty();
    }

    @Test
    @DisplayName("Should create Form with complex form data")
    void shouldCreateFormWithComplexFormData() {
        Map<String, String> formData = new HashMap<>();
        formData.put("field1", "value1");
        formData.put("field2", "value2");

        Form form = new Form(null, formData, "/template.pdf", null, "url");

        assertThat(form.formData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) form.formData();
        assertThat(result).hasSize(2);
        assertThat(result.get("field1")).isEqualTo("value1");
    }

    @Test
    @DisplayName("Should create Form with custom fields array")
    void shouldCreateFormWithCustomFieldsArray() {
        Object[] customFields = new Object[]{"input1", "input2", "input3"};

        Form form = new Form(null, null, "/template.pdf", customFields, "url");

        assertThat(form.customFields()).isInstanceOf(Object[].class);
        Object[] result = (Object[]) form.customFields();
        assertThat(result).hasSize(3);
    }
}
