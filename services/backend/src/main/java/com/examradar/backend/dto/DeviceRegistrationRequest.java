package com.examradar.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRegistrationRequest(
    @NotBlank(message = "FCM device token is required")
    @Size(max = 512, message = "Token cannot exceed 512 characters")
    String token,

    String deviceType,
    String appVersion
) {}
