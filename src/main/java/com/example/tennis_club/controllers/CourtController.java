package com.example.tennis_club.controllers;

import com.example.tennis_club.dtos.court.CourtRequest;
import com.example.tennis_club.dtos.court.CourtResponse;
import com.example.tennis_club.services.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Courts", description = "Court management")
@RestController
@RequestMapping("/api/courts")
public class CourtController {

    private final CourtService courtService;

    public CourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    @Operation(summary = "Get all courts")
    @ApiResponse(responseCode = "200", description = "Courts returned")
    @GetMapping
    public ResponseEntity<List<CourtResponse>> getAllCourts() {
        return ResponseEntity.ok(courtService.getAllCourts());
    }

    @Operation(summary = "Get court by ID")
    @ApiResponse(responseCode = "200", description = "Court returned")
    @ApiResponse(responseCode = "404", description = "Court not found")
    @GetMapping("/{id}")
    public ResponseEntity<CourtResponse> getCourtById(@PathVariable Long id) {
        return ResponseEntity.ok(courtService.getCourtById(id));
    }

    @Operation(summary = "Create court")
    @ApiResponse(responseCode = "201", description = "Court created")
    @ApiResponse(responseCode = "400", description = "Invalid request or court number already exists")
    @ApiResponse(responseCode = "404", description = "Surface type not found")
    @PostMapping
    public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courtService.createCourt(request));
    }

    @Operation(summary = "Update court")
    @ApiResponse(responseCode = "200", description = "Court updated")
    @ApiResponse(responseCode = "400", description = "Invalid request or court number already exists")
    @ApiResponse(responseCode = "404", description = "Court or surface type not found")
    @PutMapping("/{id}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CourtRequest request
    ) {
        return ResponseEntity.ok(courtService.updateCourt(id, request));
    }

    @Operation(summary = "Delete court")
    @ApiResponse(responseCode = "204", description = "Court deleted")
    @ApiResponse(responseCode = "404", description = "Court not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourt(@PathVariable Long id) {
        courtService.deleteCourt(id);
        return ResponseEntity.noContent().build();
    }
}