package com.fastvisa.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

@DisplayName("FormService Tests")
class FormServiceTest {

    private FormService formService;
    private JSONParser jsonParser;

    @BeforeEach
    void setUp() {
        formService = new FormService();
        jsonParser = new JSONParser();
    }

    @Test
    @DisplayName("Should parse form data from JSON string")
    void shouldParseFormDataFromJsonString() throws Exception {
        String jsonData = "[{\"name\":\"field1\",\"value\":\"value1\"},{\"name\":\"field2\",\"value\":\"value2\"}]";
        File tempFile = createTempJsonFile(jsonData);

        JSONArray result = formService.getFormArray(tempFile.getAbsolutePath());

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

        JSONArray result = formService.getFormArray(formData);

        assertThat(result).hasSize(2);
        JSONObject field1 = (JSONObject) result.get(0);
        assertThat(field1.get("name")).isEqualTo("field1");
    }

    @Test
    @DisplayName("Should parse structure inputs from object")
    void shouldParseStructureInputsFromObject() throws Exception {
        Map<String, Object> structure1 = new HashMap<>();
        structure1.put("field_name", "field1");
        structure1.put("x", 100);
        structure1.put("y", 200);

        Object structureInputs = Arrays.asList(structure1);

        JSONArray result = formService.getStructureInputArray(structureInputs);

        assertThat(result).hasSize(1);
        JSONObject structure = (JSONObject) result.get(0);
        assertThat(structure.get("field_name")).isEqualTo("field1");
        assertThat(structure.get("x")).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should chunk array correctly")
    void shouldChunkArrayCorrectly() {
        String[] input = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
        int chunkSize = 3;

        String[] result = FormService.chunkArray(input, chunkSize);

        assertThat(result).hasSize(chunkSize);
        assertThat(result[0]).contains("one", "two", "three");
        assertThat(result[1]).contains("four", "five", "six");
        assertThat(result[2]).contains("seven", "eight", "nine");
    }

    @Test
    @DisplayName("Should chunk array with exact division")
    void shouldChunkArrayWithExactDivision() {
        String[] input = {"one", "two", "three", "four", "five", "six"};
        int chunkSize = 2;

        String[] result = FormService.chunkArray(input, chunkSize);

        assertThat(result).hasSize(2);
        assertThat(result[0]).contains("one", "two", "three");
        assertThat(result[1]).contains("four", "five", "six");
    }

    @Test
    @DisplayName("Should chunk array with remainder")
    void shouldChunkArrayWithRemainder() {
        String[] input = {"one", "two", "three", "four", "five"};
        int chunkSize = 2;

        String[] result = FormService.chunkArray(input, chunkSize);

        assertThat(result).hasSize(2);
        assertThat(result[0]).contains("one", "two", "three");
        assertThat(result[1]).contains("four", "five");
    }

    @Test
    @DisplayName("Should handle single element array")
    void shouldHandleSingleElementArray() {
        String[] input = {"only"};
        int chunkSize = 1;

        String[] result = FormService.chunkArray(input, chunkSize);

        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo("only ");
    }

    private File createTempJsonFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("test-form-data", ".json");
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write(content);
        }
        return tempFile.toFile();
    }
}
