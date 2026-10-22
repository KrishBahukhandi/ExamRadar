package com.examradar.backend.controller;

import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.ApplicationChecklistDto;
import com.examradar.backend.dto.UserPreferencesDto;
import com.examradar.backend.security.AuthenticatedUser;
import com.examradar.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "User Profile & Preferences", description = "Personalized candidate goals, notification settings, and private application checklists")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get current candidate's followed examinations and notification reminder intervals")
    public ApiResponse<UserPreferencesDto> getPreferences(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ApiResponse.ok(userService.getPreferences(authUser));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update candidate examination goals and notification preferences")
    public ApiResponse<UserPreferencesDto> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody UserPreferencesDto request
    ) {
        UserPreferencesDto updated = userService.updatePreferences(authUser, request);
        return ApiResponse.ok("Preferences updated successfully", updated);
    }

    @GetMapping("/checklists")
    @Operation(summary = "Get candidate's private application checklists")
    public ApiResponse<List<ApplicationChecklistDto>> getChecklists(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ApiResponse.ok(userService.getChecklists(authUser));
    }

    @PostMapping("/checklists")
    @Operation(summary = "Initialize a standard 6-step application checklist for an examination")
    public ApiResponse<ApplicationChecklistDto> createChecklist(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam String examId
    ) {
        return ApiResponse.ok("Checklist ready", userService.createChecklist(authUser, examId));
    }

    @PatchMapping("/checklists/{checklistId}/items/{itemKey}")
    @Operation(summary = "Update checklist step completion status")
    public ApiResponse<ApplicationChecklistDto> updateChecklistItem(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID checklistId,
            @PathVariable String itemKey,
            @RequestParam boolean isCompleted
    ) {
        return ApiResponse.ok("Item updated", userService.updateChecklistItem(authUser, checklistId, itemKey, isCompleted));
    }

    @DeleteMapping("/data")
    @Operation(summary = "Right to be forgotten: permanently delete all candidate profile data, preferences, checklists, and devices")
    public ApiResponse<String> deleteUserData(@AuthenticationPrincipal AuthenticatedUser authUser) {
        userService.deleteUserData(authUser);
        return ApiResponse.ok("All candidate data permanently and securely erased.");
    }
}
