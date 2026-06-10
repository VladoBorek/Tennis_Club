package com.example.tennis_club.controllers;

import com.example.tennis_club.dtos.surface.SurfaceTypeRequest;
import com.example.tennis_club.dtos.surface.SurfaceTypeResponse;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.services.SurfaceTypeService;
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

@WebMvcTest(SurfaceTypeController.class)
@Import(GlobalExceptionHandler.class)
class SurfaceTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private SurfaceTypeService surfaceTypeService;

    @Test
    void getAllSurfaceTypesShouldReturnSurfaceTypes() throws Exception {
        when(surfaceTypeService.getAllSurfaceTypes()).thenReturn(List.of(
                new SurfaceTypeResponse(1L, "Clay", new BigDecimal("0.30")),
                new SurfaceTypeResponse(2L, "Grass", new BigDecimal("0.40"))
        ));

        mockMvc.perform(get("/api/surface-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Clay"))
                .andExpect(jsonPath("$[1].name").value("Grass"));
    }

    @Test
    void getSurfaceTypeByIdShouldReturnSurfaceType() throws Exception {
        when(surfaceTypeService.getSurfaceTypeById(1L))
                .thenReturn(new SurfaceTypeResponse(1L, "Clay", new BigDecimal("0.30")));

        mockMvc.perform(get("/api/surface-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Clay"));
    }

    @Test
    void getSurfaceTypeByIdShouldReturnNotFoundWhenSurfaceTypeDoesNotExist() throws Exception {
        when(surfaceTypeService.getSurfaceTypeById(1L))
                .thenThrow(new NotFoundException("Surface type not found"));

        mockMvc.perform(get("/api/surface-types/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Surface type not found"));
    }

    @Test
    void createSurfaceTypeShouldReturnCreatedSurfaceType() throws Exception {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", new BigDecimal("0.30"));

        when(surfaceTypeService.createSurfaceType(request))
                .thenReturn(new SurfaceTypeResponse(1L, "Clay", new BigDecimal("0.30")));

        mockMvc.perform(post("/api/surface-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Clay"));

        verify(surfaceTypeService).createSurfaceType(request);
    }

    @Test
    void createSurfaceTypeShouldReturnBadRequestForInvalidPrice() throws Exception {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", BigDecimal.ZERO);

        mockMvc.perform(post("/api/surface-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.pricePerMinute").exists());

        verifyNoInteractions(surfaceTypeService);
    }

    @Test
    void createSurfaceTypeShouldReturnBadRequestWhenNameAlreadyExists() throws Exception {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", new BigDecimal("0.30"));

        when(surfaceTypeService.createSurfaceType(request))
                .thenThrow(new BadRequestException("Surface type name already exists"));

        mockMvc.perform(post("/api/surface-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Surface type name already exists"));
    }

    @Test
    void updateSurfaceTypeShouldReturnUpdatedSurfaceType() throws Exception {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Updated Clay", new BigDecimal("0.35"));

        when(surfaceTypeService.updateSurfaceType(1L, request))
                .thenReturn(new SurfaceTypeResponse(1L, "Updated Clay", new BigDecimal("0.35")));

        mockMvc.perform(put("/api/surface-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Clay"));
    }

    @Test
    void deleteSurfaceTypeShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/surface-types/1"))
                .andExpect(status().isNoContent());

        verify(surfaceTypeService).deleteSurfaceType(1L);
    }

    @Test
    void deleteSurfaceTypeShouldReturnBadRequestWhenSurfaceTypeIsUsed() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("Surface type is used by active courts"))
                .when(surfaceTypeService)
                .deleteSurfaceType(1L);

        mockMvc.perform(delete("/api/surface-types/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Surface type is used by active courts"));
    }
}