package com.fastvisa.manipulatepdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FormsController.class)
@TestPropertySource(properties = {
    "AWS_ACCESS_KEY_ID=",
    "AWS_SECRET_ACCESS_KEY=",
    "AWS_S3_BUCKET_NAME=",
    "AWS_S3_BUCKET_REGION=us-east-1"
})
@DisplayName("FormsController Tests")
class FormsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.version}")
    private String projectVersion;

    @MockBean
    private com.fastvisa.services.FillFormService fillFormService;

    @MockBean
    private com.fastvisa.services.CombineFormService combineFormService;

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
            .andExpect(jsonPath("$.version").value(projectVersion))
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

    private static final String QUESTIONNAIRE_PAYLOAD = "{"
        + "\"receipt_type\":\"Questionnaire\","
        + "\"output_name\":\"Questionnaire-42-1791358200\","
        + "\"form_data\":{"
        + "\"case\":{\"number\":\"238547\",\"name\":\"H-1B Extension 2026\"},"
        + "\"client\":\"Zhang Ling He\",\"questionnaire\":\"H-1B Extension Questionnaire\","
        + "\"submitted_at\":\"Jun 28, 2026\",\"verified_at\":\"Oct 7, 2026 at 2:30 PM WIB\","
        + "\"verified_by\":\"Yusen Lee\",\"totals\":{\"provided\":1,\"total\":2},"
        + "\"blocks\":[{\"section_index\":1,\"section_total\":1,\"title\":\"Additional information\","
        + "\"continued\":false,\"description\":null,\"provided\":1,\"total\":2,\"fields\":["
        + "{\"label\":\"Arrested?\",\"display_value\":\"No\",\"provided\":true},"
        + "{\"label\":\"Notes\",\"display_value\":null,\"provided\":false}]}],"
        + "\"missing_optional\":[\"Notes\"]}}";

    @Test
    @DisplayName("Should generate a Questionnaire PDF and return a download url")
    void shouldGenerateQuestionnaireReceipt() throws Exception {
        mockMvc.perform(post("/api/v1/generate-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(QUESTIONNAIRE_PAYLOAD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.receipt_type").value("Questionnaire"))
            .andExpect(jsonPath("$.output_name").value("Questionnaire-42-1791358200"))
            .andExpect(jsonPath("$.url_download").isNotEmpty());
    }
}
