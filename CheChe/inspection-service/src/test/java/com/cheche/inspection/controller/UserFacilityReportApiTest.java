package com.cheche.inspection.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cheche.inspection.dto.FacilitySummary;
import com.cheche.inspection.service.FacilityServiceClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report-api;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserFacilityReportApiTest {
    private static final Path UPLOAD_DIR = createUploadDirectory();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacilityServiceClient facilityServiceClient;

    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("cheche.upload-dir", UPLOAD_DIR::toString);
    }

    @Test
    void closedFacilityReportIsStoredThroughMultipartApi() throws Exception {
        when(facilityServiceClient.get(1676L, 20L, "11545")).thenReturn(new FacilitySummary(
                1676L, "검증 전용 시설", "체육시설", "서울특별시 금천구", "서울 금천구",
                null, "CLOSED", "운영 종료", null, false, "/guide"));
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "damage.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});

        mockMvc.perform(multipart("/api/user/reports")
                        .file(photo)
                        .header("X-User-Id", "20")
                        .header("X-User-Region", "11545")
                        .param("facilityId", "1676")
                        .param("category", "OTHER")
                        .param("locationDescription", "검증용 위치")
                        .param("comment", "실제 손상·수리 요청이 아닌 기능 검증 내용"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.facilityId").value(1676))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());

        mockMvc.perform(get("/api/user/reports").header("X-User-Id", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].facilityId").value(1676));
    }

    private static Path createUploadDirectory() {
        try {
            return Files.createTempDirectory("cheche-report-api-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
