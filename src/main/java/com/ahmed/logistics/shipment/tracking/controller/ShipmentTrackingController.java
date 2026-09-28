package com.ahmed.logistics.shipment.tracking.controller;

import com.ahmed.logistics.shipment.tracking.dto.ShipmentTimelineResponse;
import com.ahmed.logistics.shipment.tracking.dto.ShipmentTrackingResponse;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Shipment Tracking", description = "Shipment tracking history and chronological timeline APIs")
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentTrackingController {

    private final ShipmentTrackingService shipmentTrackingService;

    @Operation(
            summary = "Get shipment tracking history / timeline",
            description = "Retrieves the chronological tracking timeline for a shipment. " +
                    "When page or size query parameters are provided, returns a Spring Data Page of tracking records; " +
                    "otherwise returns a full chronological list. Safe maximum page size is 100.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tracking history retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ShipmentTrackingResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied (user does not have permission)"),
            @ApiResponse(responseCode = "404", description = "Not Found - shipment does not exist")
    })
    @PreAuthorize("@shipmentSecurity.canRead(#shipmentId, authentication)")
    @GetMapping("/{shipmentId}/tracking")
    public ResponseEntity<?> getShipmentTracking(
            @Parameter(description = "ID of the shipment to track", required = true)
            @PathVariable Long shipmentId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(required = false) Integer size,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC, size = 20) Pageable pageable) {

        if (page != null || size != null) {
            Page<ShipmentTrackingResponse> trackingPage = shipmentTrackingService.getShipmentTracking(shipmentId, pageable);
            return ResponseEntity.ok(trackingPage);
        }

        List<ShipmentTrackingResponse> tracking = shipmentTrackingService.getShipmentTracking(shipmentId);
        return ResponseEntity.ok(tracking);
    }

    @Operation(
            summary = "Track shipment by tracking number",
            description = "Retrieves shipment summary, current status, and full chronological tracking timeline using the tracking number.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shipment summary and tracking timeline retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShipmentTimelineResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied (user does not have permission)"),
            @ApiResponse(responseCode = "404", description = "Not Found - shipment does not exist with tracking number")
    })
    @PreAuthorize("@shipmentSecurity.canReadByTracking(#trackingNumber, authentication)")
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ShipmentTimelineResponse> getShipmentTimelineByTrackingNumber(
            @Parameter(description = "Tracking number of the shipment", required = true)
            @PathVariable String trackingNumber) {

        ShipmentTimelineResponse timeline = shipmentTrackingService.getTimelineByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(timeline);
    }
}
