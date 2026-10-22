package com.examradar.backend.controller;

import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.DeviceRegistrationRequest;
import com.examradar.backend.security.AuthenticatedUser;
import com.examradar.backend.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Registration and lifecycle management of FCM device push tokens")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @Operation(summary = "Register or refresh an Android device token for Firebase Cloud Messaging")
    public ApiResponse<String> registerDevice(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        String token = deviceService.registerDevice(authUser, request);
        return ApiResponse.ok("Device token registered successfully", token);
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Deactivate an FCM device push token")
    public ApiResponse<String> unregisterDevice(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String token
    ) {
        deviceService.unregisterDevice(authUser, token);
        return ApiResponse.ok("Device token deactivated successfully");
    }
}
