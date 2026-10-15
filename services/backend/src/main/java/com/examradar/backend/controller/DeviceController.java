package com.examradar.backend.controller;

import com.examradar.backend.domain.DeviceToken;
import com.examradar.backend.domain.UserProfile;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.DeviceRegistrationRequest;
import com.examradar.backend.repository.DeviceTokenRepository;
import com.examradar.backend.repository.UserProfileRepository;
import com.examradar.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Registration and lifecycle management of FCM device push tokens")
public class DeviceController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserProfileRepository userProfileRepository;

    public DeviceController(DeviceTokenRepository deviceTokenRepository, UserProfileRepository userProfileRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Register or refresh an Android device token for Firebase Cloud Messaging")
    public ApiResponse<String> registerDevice(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        UserProfile user = userProfileRepository.findById(authUser.getId())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile(authUser.getId(), authUser.getEmail(), "Candidate");
                    return userProfileRepository.save(profile);
                });

        Optional<DeviceToken> existingOpt = deviceTokenRepository.findByToken(request.token());
        if (existingOpt.isPresent()) {
            DeviceToken token = existingOpt.get();
            token.setUser(user);
            token.setDeviceType(request.deviceType() != null ? request.deviceType() : "ANDROID");
            token.setAppVersion(request.appVersion());
            token.setIsActive(true);
            token.setLastUsedAt(OffsetDateTime.now());
            deviceTokenRepository.save(token);
            return ApiResponse.ok("Device token refreshed successfully", request.token());
        }

        DeviceToken newToken = new DeviceToken(user, request.token(), request.deviceType(), request.appVersion());
        deviceTokenRepository.save(newToken);
        return ApiResponse.ok("Device token registered successfully", request.token());
    }

    @DeleteMapping("/{token}")
    @Transactional
    @Operation(summary = "Deactivate an FCM device push token")
    public ApiResponse<String> unregisterDevice(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String token
    ) {
        deviceTokenRepository.findByToken(token).ifPresent(dt -> {
            if (dt.getUser().getId().equals(authUser.getId())) {
                dt.setIsActive(false);
                deviceTokenRepository.save(dt);
            }
        });
        return ApiResponse.ok("Device token deactivated successfully");
    }
}
