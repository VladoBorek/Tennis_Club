package com.example.tennis_club.controllers;

import com.example.tennis_club.dtos.court.CourtRequest;
import com.example.tennis_club.dtos.court.CourtResponse;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.services.CourtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtController.class)
@Import(GlobalExceptionHandler.class)
class CourtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private CourtService courtService;

    @Test
    void getAllCourtsShouldReturnCourts() throws Exception {
        when(courtService.getAllCourts()).thenReturn(List.of(
                new CourtResponse(1L, "COURT-1", 10L, "Clay", new BigDecimal("0.30")),
                new CourtResponse(2L, "COURT-2", 11L, "Grass", new BigDecimal("0.40"))
        ));

        mockMvc.perform(get("/api/courts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].courtNumber").value("COURT-1"))
                .andExpect(jsonPath("$[0].surfaceTypeName").value("Clay"))
                .andExpect(jsonPath("$[1].courtNumber").value("COURT-2"));
    }

    @Test
    void getCourtByIdShouldReturnCourt() throws Exception {
        when(courtService.getCourtById(1L))
                .thenReturn(new CourtResponse(1L, "COURT-1", 10L, "Clay", new BigDecimal("0.30")));

        mockMvc.perform(get("/api/courts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.courtNumber").value("COURT-1"))
                .andExpect(jsonPath("$.surfaceTypeId").value(10))
                .andExpect(jsonPath("$.surfaceTypeName").value("Clay"));
    }

    @Test
    void getCourtByIdShouldReturnNotFoundWhenCourtDoesNotExist() throws Exception {
        when(courtService.getCourtById(1L)).thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/api/courts/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Court not found"))
                .andExpect(jsonPath("$.path").value("/api/courts/1"));
    }

    @Test
    void getCourtByIdShouldReturnBadRequestWhenIdHasInvalidType() throws Exception {
        mockMvc.perform(get("/api/courts/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    void createCourtShouldReturnCreatedCourt() throws Exception {
        CourtRequest request = new CourtRequest("COURT-1", 10L);

        when(courtService.createCourt(request))
                .thenReturn(new CourtResponse(1L, "COURT-1", 10L, "Clay", new BigDecimal("0.30")));

        mockMvc.perform(post("/api/courts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.courtNumber").value("COURT-1"));

        verify(courtService).createCourt(request);
    }

    @Test
    void createCourtShouldReturnBadRequestForInvalidRequestBody() throws Exception {
        CourtRequest request = new CourtRequest("", 10L);

        mockMvc.perform(post("/api/courts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.courtNumber").exists());

        verifyNoInteractions(courtService);
    }

    @Test
    void createCourtShouldReturnBadRequestWhenCourtNumberAlreadyExists() throws Exception {
        CourtRequest request = new CourtRequest("COURT-1", 10L);

        when(courtService.createCourt(request))
                .thenThrow(new BadRequestException("Court number already exists"));

        mockMvc.perform(post("/api/courts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Court number already exists"));
    }

    @Test
    void updateCourtShouldReturnUpdatedCourt() throws Exception {
        CourtRequest request = new CourtRequest("COURT-2", 10L);

        when(courtService.updateCourt(1L, request))
                .thenReturn(new CourtResponse(1L, "COURT-2", 10L, "Clay", new BigDecimal("0.30")));

        mockMvc.perform(put("/api/courts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courtNumber").value("COURT-2"));
    }

    @Test
    void deleteCourtShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/courts/1"))
                .andExpect(status().isNoContent());

        verify(courtService).deleteCourt(1L);
    }
}