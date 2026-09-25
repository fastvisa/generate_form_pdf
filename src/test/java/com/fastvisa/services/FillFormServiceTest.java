package com.fastvisa.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FillFormService Tests")
class FillFormServiceTest {

    private FillFormService fillFormService;
    private JSONParser jsonParser;

    @BeforeEach
    void setUp() {
        fillFormService = new FillFormService();
        jsonParser = new JSONParser();
    }

    @Test
    @DisplayName("Should parse form data from JSON string")
    void shouldParseFormDataFromJsonString() throws Exception {
        String jsonData = "[{\"name\":\"field1\",\"value\":\"value1\"},{\"name\":\"field2\",\"value\":\"value2\"}]";
        File tempFile = createTempJsonFile(jsonData);

        JSONArray result = fillFormService.getFormArray(tempFile.getAbsolutePath());

        assertThat(result).hasSize(2);
        JSONObject field1 = (JSONObject) result.get(0);
        assertThat(field1.get("name")).isEqualTo("field1");
        assertThat(field1.get("value")).isEqualTo("value1");

        Files.deleteIfExists(tempFile.toPath());
    }

    @Test
    @DisplayName("Should parse form data from object")
    void shouldParseFormDataFromObject() throws Exception {
        Map<String, Object> formData1 = new HashMap<>();
        formData1.put("name", "field1");
        formData1.put("value", "value1");

        Map<String, Object> formData2 = new HashMap<>();
        formData2.put("name", "field2");
        formData2.put("value", "value2");

        Object formData = Arrays.asList(formData1, formData2);

        JSONArray result = fillFormService.getFormArray(formData);

        assertThat(result).hasSize(2);
        JSONObject field1 = (JSONObject) result.get(0);
        assertThat(field1.get("name")).isEqualTo("field1");
    }

    @Test
    @DisplayName("Should parse custom fields from object")
    void shouldParseCustomFieldsFromObject() throws Exception {
        Map<String, Object> custom1 = new HashMap<>();
        custom1.put("field_name", "field1");
        custom1.put("x", 100);
        custom1.put("y", 200);

        Object customFields = Arrays.asList(custom1);

        JSONArray result = fillFormService.getCustomFieldsArray(customFields);

        assertThat(result).hasSize(1);
        JSONObject field = (JSONObject) result.get(0);
        assertThat(field.get("field_name")).isEqualTo("field1");
        assertThat(field.get("x")).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should not collapse font for short text in short multiline field")
    void shouldNotCollapseFontForShortTextInShortMultilineField() throws Exception {
        // Real rect of I-914 Supp A field pEWhyCharged1 (174.21 x 28.346pt, ~2 lines)
        Rectangle rect = new Rectangle(54f, 320.899f, 174.21f, 28.346f);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);

        Method method = FillFormService.class.getDeclaredMethod(
            "getDynamicMultiLineFontSize", String.class, Rectangle.class, PdfFont.class, float.class);
        method.setAccessible(true);
        float fontSize = (float) method.invoke(fillFormService, "Theft", rect, font, 14f);

        assertThat(fontSize).isGreaterThan(4f);
    }

    private File createTempJsonFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("test-form-data", ".json");
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write(content);
        }
        return tempFile.toFile();
    }
}
