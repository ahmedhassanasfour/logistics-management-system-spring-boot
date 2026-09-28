package com.ahmed.logistics.shipment.controller;

import com.ahmed.logistics.shipment.dto.AssignShipmentRequest;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.dto.UpdateShipmentRequest;
import com.ahmed.logistics.shipment.dto.UpdateShipmentStatusRequest;
import com.ahmed.logistics.shipment.service.ShipmentAssignmentService;
import com.ahmed.logistics.shipment.service.ShipmentLifecycleService;
import com.ahmed.logistics.shipment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shipments", description = "Shipment lifecycle, distance-based pricing, driver/vehicle assignment, and status transitions")
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentLifecycleService shipmentLifecycleService;
    private final ShipmentAssignmentService shipmentAssignmentService;

    @Operation(summary = "Create shipment", description = "Creates a shipment with automatic address geocoding, route distance calculation, and distance-based pricing.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Shipment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or geocoding failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - customer ID mismatch")
    })
    @PreAuthorize("@shipmentSecurity.canCreate(#request.customerId(), authentication)")
    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get shipment by ID", description = "Retrieves shipment details by ID. Restricted to owner, assigned driver, or staff.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment details retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PreAuthorize("@shipmentSecurity.canRead(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long id,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get shipment by tracking number", description = "Retrieves shipment details by its unique tracking number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment details retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PreAuthorize("@shipmentSecurity.canReadByTracking(#trackingNumber, authentication)")
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @Parameter(description = "Tracking number", required = true)
            @PathVariable String trackingNumber,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get customer shipments (paginated)", description = "Retrieves paginated shipments belonging to a specific customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer shipments retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PreAuthorize("@shipmentSecurity.canReadCustomerShipments(#customerId, authentication)")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<ShipmentResponse>> getCustomerShipments(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Long customerId,
            @Parameter(description = "Pagination configuration")
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<ShipmentResponse> response = shipmentService.getCustomerShipments(customerId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update shipment", description = "Updates address and package parameters, recalculating distance and pricing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or shipment cannot be updated in its current status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PreAuthorize("@shipmentSecurity.canModify(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentRequest request,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.updateShipment(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Transition shipment status", description = "Transitions shipment status following the state machine. Requires ADMIN or DISPATCHER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition requested"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or DISPATCHER role"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentStatusRequest request
    ) {
        ShipmentResponse response = shipmentLifecycleService.transitionStatus(id, request.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Assign driver and vehicle", description = "Assigns an available driver and vehicle to a confirmed shipment. Requires ADMIN or DISPATCHER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver and vehicle assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Driver or vehicle not available or shipment in invalid state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or DISPATCHER role"),
            @ApiResponse(responseCode = "404", description = "Shipment, driver, or vehicle not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @PatchMapping("/{shipmentId}/assignment")
    public ResponseEntity<ShipmentResponse> assignDriverAndVehicle(
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long shipmentId,
            @Valid @RequestBody AssignShipmentRequest request
    ) {
        ShipmentResponse response = shipmentAssignmentService.assignDriverAndVehicle(
                shipmentId,
                request.driverId(),
                request.vehicleId()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete / cancel shipment", description = "Deletes or cancels a shipment. Requires customer ownership or staff role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Shipment deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PreAuthorize("@shipmentSecurity.canModify(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long id,
            Authentication authentication
    ) {
        shipmentService.deleteShipment(id);
        return ResponseEntity.noContent().build();
    }
}
