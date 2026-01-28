package com.fastvisa.manipulatepdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FormsController.class)
@TestPropertySource(properties = {
    "AWS_ACCESS_KEY_ID=",
    "AWS_SECRET_ACCESS_KEY=",
    "AWS_S3_BUCKET_NAME=",
    "AWS_S3_BUCKET_REGION=us-east-1",
    "project.version=1.0.0-SNAPSHOT"
})
@DisplayName("FormsController Tests")
class FormsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.fastvisa.services.FormService formService;

    @MockBean
    private com.fastvisa.services.ReceiptService receiptService;

    @MockBean
    private com.fastvisa.services.AwsS3Service awsS3Service;

    @BeforeEach
    void setUp() {
        // Reset any mock configurations if needed
    }

    @Test
    @DisplayName("Should return health status UP")
    void shouldReturnHealthStatusUp() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("manipulate-pdf"))
            .andExpect(jsonPath("$.version").value("1.0.0-SNAPSHOT"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return health status with correct structure")
    void shouldReturnHealthStatusWithCorrectStructure() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isMap())
            .andExpect(jsonPath("$", hasKey("status")))
            .andExpect(jsonPath("$", hasKey("service")))
            .andExpect(jsonPath("$", hasKey("version")))
            .andExpect(jsonPath("$", hasKey("timestamp")));
    }

    @Test
    @DisplayName("Should handle health endpoint without authentication")
    void shouldHandleHealthEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform endpoint with valid JSON")
    void shouldHandleFillformEndpointWithValidJson() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"value1\"}],"
            + "\"template_path\":\"/path/to/template.pdf\","
            + "\"structure_inputs\":[{\"field_name\":\"field1\",\"x\":100,\"y\":200,\"width\":300,\"height\":50,\"multiline\":false,\"row\":1}]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform endpoint with empty form_data")
    void shouldHandleFillformEndpointWithEmptyFormData() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[],"
            + "\"template_path\":\"/path/to/template.pdf\","
            + "\"structure_inputs\":[]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform endpoint with null structure_inputs")
    void shouldHandleFillformEndpointWithNullStructureInputs() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"value1\"}],"
            + "\"template_path\":\"/path/to/template.pdf\","
            + "\"structure_inputs\":null"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle combineform endpoint with valid JSON")
    void shouldHandleCombineformEndpointWithValidJson() throws Exception {
        String requestBody = "{"
            + "\"pdf_data\":["
            + "  {"
            + "    \"form_data\":[{\"name\":\"field1\",\"value\":\"value1\"}],"
            + "    \"pdf_template\":\"/path/to/template1.pdf\","
            + "    \"structure_inputs\":[]"
            + "  },"
            + "  {"
            + "    \"form_data\":[{\"name\":\"field2\",\"value\":\"value2\"}],"
            + "    \"pdf_template\":\"/path/to/template2.pdf\","
            + "    \"structure_inputs\":[]"
            + "  }"
            + "]"
            + "}";

        mockMvc.perform(post("/api/v1/combineform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle combineform endpoint with empty pdf_data")
    void shouldHandleCombineformEndpointWithEmptyPdfData() throws Exception {
        String requestBody = "{"
            + "\"pdf_data\":[]"
            + "}";

        mockMvc.perform(post("/api/v1/combineform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle generate-receipt endpoint with Receipt type")
    void shouldHandleGenerateReceiptEndpointWithReceiptType() throws Exception {
        String requestBody = "{"
            + "\"form_data\":{\"amount\":\"100.00\",\"date\":\"2024-01-01\"},"
            + "\"output_name\":\"receipt_123\","
            + "\"receipt_type\":\"Receipt\""
            + "}";

        mockMvc.perform(post("/api/v1/generate-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle generate-receipt endpoint with Addendum type")
    void shouldHandleGenerateReceiptEndpointWithAddendumType() throws Exception {
        String requestBody = "{"
            + "\"form_data\":{\"content\":\"Addendum content\"},"
            + "\"output_name\":\"addendum_456\","
            + "\"receipt_type\":\"Addendum\""
            + "}";

        mockMvc.perform(post("/api/v1/generate-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle generate-receipt endpoint with null form_data")
    void shouldHandleGenerateReceiptEndpointWithNullFormData() throws Exception {
        String requestBody = "{"
            + "\"form_data\":null,"
            + "\"output_name\":\"test_output\","
            + "\"receipt_type\":\"Receipt\""
            + "}";

        mockMvc.perform(post("/api/v1/generate-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle generate-receipt endpoint with empty output_name")
    void shouldHandleGenerateReceiptEndpointWithEmptyOutputName() throws Exception {
        String requestBody = "{"
            + "\"form_data\":{\"field\":\"value\"},"
            + "\"output_name\":\"\","
            + "\"receipt_type\":\"Receipt\""
            + "}";

        mockMvc.perform(post("/api/v1/generate-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 for invalid JSON in fillform endpoint")
    void shouldReturn400ForInvalidJsonInFillformEndpoint() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for missing required fields in fillform")
    void shouldReturn400ForMissingRequiredFieldsInFillform() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"value1\"}]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid JSON in combineform endpoint")
    void shouldReturn400ForInvalidJsonInCombineformEndpoint() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/api/v1/combineform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid JSON in generate-receipt endpoint")
    void shouldReturn400ForInvalidJsonInGenerateReceiptEndpoint() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/api/v1/generate-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle fillform with URL template path")
    void shouldHandleFillformWithUrlTemplatePath() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"value1\"}],"
            + "\"template_path\":\"https://example.com/template.pdf\","
            + "\"structure_inputs\":[]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform with HTTP template path")
    void shouldHandleFillformWithHttpTemplatePath() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"value1\"}],"
            + "\"template_path\":\"http://example.com/template.pdf\","
            + "\"structure_inputs\":[]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform with multiline structure input")
    void shouldHandleFillformWithMultilineStructureInput() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"value1 value2 value3\"}],"
            + "\"template_path\":\"/path/to/template.pdf\","
            + "\"structure_inputs\":[{\"field_name\":\"field1\",\"x\":100,\"y\":200,\"width\":300,\"height\":50,\"multiline\":true,\"row\":2}]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform with null values in form_data")
    void shouldHandleFillformWithNullValuesInFormData() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":null}],"
            + "\"template_path\":\"/path/to/template.pdf\","
            + "\"structure_inputs\":[]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle fillform with empty string values")
    void shouldHandleFillformWithEmptyStringValues() throws Exception {
        String requestBody = "{"
            + "\"form_data\":[{\"name\":\"field1\",\"value\":\"\"}],"
            + "\"template_path\":\"/path/to/template.pdf\","
            + "\"structure_inputs\":[]"
            + "}";

        mockMvc.perform(post("/api/v1/fillform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }
}
