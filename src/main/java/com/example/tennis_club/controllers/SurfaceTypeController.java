package com.example.tennis_club.controllers;

import com.example.tennis_club.dtos.surface.SurfaceTypeRequest;
import com.example.tennis_club.dtos.surface.SurfaceTypeResponse;
import com.example.tennis_club.services.SurfaceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Surface types", description = "Surface type codebook management")
@RestController
@RequestMapping("/api/surface-types")
public class SurfaceTypeController {

    private final SurfaceTypeService surfaceTypeService;

    public SurfaceTypeController(SurfaceTypeService surfaceTypeService) {
        this.surfaceTypeService = surfaceTypeService;
    }

    @Operation(summary = "Get all surface types")
    @ApiResponse(responseCode = "200", description = "Surface types returned")
    @GetMapping
    public ResponseEntity<List<SurfaceTypeResponse>> getAllSurfaceTypes() {
        return ResponseEntity.ok(surfaceTypeService.getAllSurfaceTypes());
    }

    @Operation(summary = "Get surface type by ID")
    @ApiResponse(responseCode = "200", description = "Surface type returned")
    @ApiResponse(responseCode = "404", description = "Surface type not found")
    @GetMapping("/{id}")
    public ResponseEntity<SurfaceTypeResponse> getSurfaceTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(surfaceTypeService.getSurfaceTypeById(id));
    }

    @Operation(summary = "Create surface type")
    @ApiResponse(responseCode = "201", description = "Surface type created")
    @ApiResponse(responseCode = "400", description = "Invalid request or surface type name already exists")
    @PostMapping
    public ResponseEntity<SurfaceTypeResponse> createSurfaceType(@Valid @RequestBody SurfaceTypeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(surfaceTypeService.createSurfaceType(request));
    }

    @Operation(summary = "Update surface type")
    @ApiResponse(responseCode = "200", description = "Surface type updated")
    @ApiResponse(responseCode = "400", description = "Invalid request or surface type name already exists")
    @ApiResponse(responseCode = "404", description = "Surface type not found")
    @PutMapping("/{id}")
    public ResponseEntity<SurfaceTypeResponse> updateSurfaceType(
            @PathVariable Long id,
            @Valid @RequestBody SurfaceTypeRequest request
    ) {
        return ResponseEntity.ok(surfaceTypeService.updateSurfaceType(id, request));
    }

    @Operation(summary = "Delete surface type")
    @ApiResponse(responseCode = "204", description = "Surface type deleted")
    @ApiResponse(responseCode = "400", description = "Surface type is used by active courts")
    @ApiResponse(responseCode = "404", description = "Surface type not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurfaceType(@PathVariable Long id) {
        surfaceTypeService.deleteSurfaceType(id);
        return ResponseEntity.noContent().build();
    }
}