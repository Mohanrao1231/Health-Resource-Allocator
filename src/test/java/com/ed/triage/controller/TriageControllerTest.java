package com.ed.triage.controller;

import com.ed.triage.dto.request.AssessmentRequest;
import com.ed.triage.dto.request.EnqueueRequest;
import com.ed.triage.dto.request.StatusUpdateRequest;
import com.ed.triage.entity.Vitals;
import com.ed.triage.enums.TriageStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TriageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /triage/assess - Should calculate ESI score and return triage record")
    void testAssessEndpoint() throws Exception {
        Vitals vitals = new Vitals(110, 140, 90, 22, 95.0, 37.5, 6, 15);
        AssessmentRequest request = new AssessmentRequest(
                1L,
                "MRN-89210",
                "Arthur",
                "Pendleton",
                LocalDate.of(1952, 4, 12),
                "Male",
                vitals,
                "Acute moderate chest tightness and fatigue",
                2,
                "Clinical check required",
                null,
                false
        );

        mockMvc.perform(post("/triage/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.triageRecordId", notNullValue()))
                .andExpect(jsonPath("$.data.esiLevel", notNullValue()))
                .andExpect(jsonPath("$.data.priorityScore", notNullValue()));
    }

    @Test
    @DisplayName("Full Triage Flow: Assess -> Enqueue -> View Queue -> Dispatch Next -> Status Update -> Undo")
    void testFullTriageFlow() throws Exception {
        // 1. Assess patient
        Vitals vitals = new Vitals(125, 95, 60, 24, 88.0, 38.2, 8, 14);
        AssessmentRequest assessReq = new AssessmentRequest(
                2L,
                "MRN-67341",
                "Elena",
                "Rostova",
                LocalDate.of(1988, 11, 23),
                "Female",
                vitals,
                "Severe shortness of breath, asthma exacerbation",
                3,
                "Oxygen therapy initiated",
                null,
                false
        );

        String assessResponse = mockMvc.perform(post("/triage/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assessReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.esiLevel", is("ESI_2")))
                .andReturn().getResponse().getContentAsString();

        Long recordId = objectMapper.readTree(assessResponse).path("data").path("triageRecordId").asLong();

        // 2. Enqueue patient
        EnqueueRequest enqueueReq = new EnqueueRequest(recordId, "NURSE_SARAH");
        mockMvc.perform(post("/triage/queue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enqueueReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("WAITING_IN_QUEUE")));

        // 3. View Waiting Queue
        mockMvc.perform(get("/triage/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));

        // 4. Dispatch Next Patient to Doctor
        mockMvc.perform(post("/triage/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.doctorId", notNullValue()))
                .andExpect(jsonPath("$.data.triageRecordId", is(recordId.intValue())));

        // 5. Update Status to COMPLETED
        StatusUpdateRequest updateReq = new StatusUpdateRequest(TriageStatus.COMPLETED, "Discharged with nebulizer prescription", "DR_MITCHELL");
        mockMvc.perform(put("/triage/" + recordId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));

        // 6. Undo Last Action (revert COMPLETED to IN_CONSULTATION)
        mockMvc.perform(post("/triage/undo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 7. View Audit Logs
        mockMvc.perform(get("/triage/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
