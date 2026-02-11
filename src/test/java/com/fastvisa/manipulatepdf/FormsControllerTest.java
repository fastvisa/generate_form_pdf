package com.fastvisa.manipulatepdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}

