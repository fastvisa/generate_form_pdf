package com.fastvisa.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CombineFormService Tests")
class CombineFormServiceTest {

    private CombineFormService combineFormService;

    @BeforeEach
    void setUp() {
        combineFormService = new CombineFormService();
    }

    @Test
    @DisplayName("Should parse form data from JSON string")
    void shouldParseFormDataFromJsonString() throws Exception {
        String jsonData = "[{\"name\":\"field1\",\"value\":\"value1\"},{\"name\":\"field2\",\"value\":\"value2\"}]";
        File tempFile = createTempJsonFile(jsonData);

        JSONArray result = combineFormService.getFormArray(tempFile.getAbsolutePath());

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

        JSONArray result = combineFormService.getFormArray(formData);

        assertThat(result).hasSize(2);
        JSONObject field1 = (JSONObject) result.get(0);
        assertThat(field1.get("name")).isEqualTo("field1");
    }

    private File createTempJsonFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("test-form-data", ".json");
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write(content);
        }
        return tempFile.toFile();
    }
}
