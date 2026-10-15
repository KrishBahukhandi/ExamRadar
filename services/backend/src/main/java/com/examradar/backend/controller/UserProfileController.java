package com.examradar.backend.controller;

import com.examradar.backend.domain.*;
import com.examradar.backend.dto.*;
import com.examradar.backend.repository.*;
import com.examradar.backend.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "User Profile & Preferences", description = "Personalized candidate goals, notification settings, and private application checklists")
public class UserProfileController {

    private final UserProfileRepository profileRepository;
    private final UserExamPreferenceRepository userExamPreferenceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ApplicationChecklistRepository checklistRepository;
    private final ApplicationChecklistItemRepository checklistItemRepository;
    private final ExamRepository examRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    public UserProfileController(
            UserProfileRepository profileRepository,
            UserExamPreferenceRepository userExamPreferenceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ApplicationChecklistRepository checklistRepository,
            ApplicationChecklistItemRepository checklistItemRepository,
            ExamRepository examRepository,
            DeviceTokenRepository deviceTokenRepository
    ) {
        this.profileRepository = profileRepository;
        this.userExamPreferenceRepository = userExamPreferenceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.examRepository = examRepository;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    private UserProfile getOrCreateUser(AuthenticatedUser authUser) {
        return profileRepository.findById(authUser.getId())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile(authUser.getId(), authUser.getEmail(), "Candidate");
                    return profileRepository.save(profile);
                });
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get current candidate's followed examinations and notification reminder intervals")
    public ApiResponse<UserPreferencesDto> getPreferences(@AuthenticationPrincipal AuthenticatedUser authUser) {
        UserProfile user = getOrCreateUser(authUser);

        List<UserExamPreference> examPrefs = userExamPreferenceRepository.findByUserId(user.getId());
        List<String> followedExamIds = examPrefs.stream()
                .filter(p -> p.getExam() != null)
                .map(p -> p.getExam().getId())
                .toList();

        List<String> followedCategoryIds = examPrefs.stream()
                .filter(p -> p.getCategory() != null)
                .map(p -> p.getCategory().getId())
                .distinct()
                .toList();

        NotificationPreference notifPref = notificationPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> new NotificationPreference(user));

        boolean notifyNew = examPrefs.isEmpty() || examPrefs.get(0).getNotifyNewNotices();
        boolean notifyDeadlines = examPrefs.isEmpty() || examPrefs.get(0).getNotifyDeadlines();
        boolean notifyResults = examPrefs.isEmpty() || examPrefs.get(0).getNotifyResults();

        UserPreferencesDto dto = new UserPreferencesDto(
                followedExamIds,
                followedCategoryIds,
                notifPref.getReminder7Days(),
                notifPref.getReminder3Days(),
                notifPref.getReminder1Day(),
                notifPref.getPushEnabled(),
                notifyNew,
                notifyDeadlines,
                notifyResults
        );

        return ApiResponse.ok(dto);
    }

    @PutMapping("/preferences")
    @Transactional
    @Operation(summary = "Update candidate examination goals and notification preferences")
    public ApiResponse<UserPreferencesDto> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody UserPreferencesDto request
    ) {
        UserProfile user = getOrCreateUser(authUser);

        // Update notification timing preferences
        NotificationPreference notifPref = notificationPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> new NotificationPreference(user));

        notifPref.setReminder7Days(request.reminder7Days());
        notifPref.setReminder3Days(request.reminder3Days());
        notifPref.setReminder1Day(request.reminder1Day());
        notifPref.setPushEnabled(request.pushEnabled());
        notifPref.setUpdatedAt(OffsetDateTime.now());
        notificationPreferenceRepository.save(notifPref);

        // Sync followed exams
        List<UserExamPreference> existing = userExamPreferenceRepository.findByUserId(user.getId());
        Set<String> newExamIds = new HashSet<>(request.followedExamIds() != null ? request.followedExamIds() : List.of());

        // Remove unselected
        for (UserExamPreference p : existing) {
            if (p.getExam() != null && !newExamIds.contains(p.getExam().getId())) {
                userExamPreferenceRepository.delete(p);
            }
        }

        // Add newly selected
        Set<String> alreadyFollowed = new HashSet<>(existing.stream()
                .filter(p -> p.getExam() != null)
                .map(p -> p.getExam().getId())
                .toList());

        for (String examId : newExamIds) {
            if (!alreadyFollowed.contains(examId)) {
                examRepository.findById(examId).ifPresent(exam -> {
                    UserExamPreference newPref = new UserExamPreference(user, exam, exam.getCategory());
                    newPref.setNotifyNewNotices(request.notifyNewNotices());
                    newPref.setNotifyDeadlines(request.notifyDeadlines());
                    newPref.setNotifyResults(request.notifyResults());
                    userExamPreferenceRepository.save(newPref);
                });
            }
        }

        return ApiResponse.ok("Preferences updated successfully", request);
    }

    @GetMapping("/checklists")
    @Operation(summary = "Get candidate's private application checklists")
    public ApiResponse<List<ApplicationChecklistDto>> getChecklists(@AuthenticationPrincipal AuthenticatedUser authUser) {
        UserProfile user = getOrCreateUser(authUser);
        List<ApplicationChecklist> checklists = checklistRepository.findByUserId(user.getId());

        List<ApplicationChecklistDto> dtos = checklists.stream().map(this::toChecklistDto).toList();
        return ApiResponse.ok(dtos);
    }

    @PostMapping("/checklists")
    @Transactional
    @Operation(summary = "Initialize a standard 6-step application checklist for an examination")
    public ApiResponse<ApplicationChecklistDto> createChecklist(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam String examId
    ) {
        UserProfile user = getOrCreateUser(authUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new NoSuchElementException("Exam not found: " + examId));

        ApplicationChecklist checklist = checklistRepository.findByUserIdAndExamId(user.getId(), examId)
                .orElseGet(() -> {
                    ApplicationChecklist cl = new ApplicationChecklist(user, exam);
                    cl = checklistRepository.save(cl);

                    // Initialize standard candidate steps
                    String[][] standardSteps = {
                            {"check_eligibility", "Review official notification and verify eligibility criteria"},
                            {"age_limit", "Verify age limit, cut-off date, and applicable category relaxations"},
                            {"prepare_docs", "Prepare scanned photo, signature, and category certificates"},
                            {"fill_application", "Complete online application form on official portal"},
                            {"pay_fee", "Pay applicable examination fee and verify transaction status"},
                            {"save_confirmation", "Download and safely store final application confirmation PDF"}
                    };

                    int order = 1;
                    for (String[] step : standardSteps) {
                        ApplicationChecklistItem item = new ApplicationChecklistItem(cl, step[0], step[1]);
                        checklistItemRepository.save(item);
                        cl.getItems().add(item);
                    }

                    return cl;
                });

        return ApiResponse.ok("Checklist ready", toChecklistDto(checklist));
    }

    @PatchMapping("/checklists/{checklistId}/items/{itemKey}")
    @Transactional
    @Operation(summary = "Update checklist step completion status")
    public ApiResponse<ApplicationChecklistDto> updateChecklistItem(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID checklistId,
            @PathVariable String itemKey,
            @RequestParam boolean isCompleted
    ) {
        ApplicationChecklist cl = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new NoSuchElementException("Checklist not found: " + checklistId));

        if (!cl.getUser().getId().equals(authUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your checklist");
        }

        List<ApplicationChecklistItem> items = checklistItemRepository.findByChecklistId(checklistId);
        items.stream()
                .filter(i -> i.getItemKey().equalsIgnoreCase(itemKey))
                .findFirst()
                .ifPresent(i -> {
                    i.setIsCompleted(isCompleted);
                    checklistItemRepository.save(i);
                });

        cl.setUpdatedAt(OffsetDateTime.now());
        checklistRepository.save(cl);

        return ApiResponse.ok("Item updated", toChecklistDto(cl));
    }

    @DeleteMapping("/data")
    @Transactional
    @Operation(summary = "Right to be forgotten: permanently delete all candidate profile data, preferences, checklists, and devices")
    public ApiResponse<String> deleteUserData(@AuthenticationPrincipal AuthenticatedUser authUser) {
        UUID userId = authUser.getId();

        // 1. Delete checklists & checklist items
        List<ApplicationChecklist> checklists = checklistRepository.findByUserId(userId);
        for (ApplicationChecklist cl : checklists) {
            checklistItemRepository.deleteAll(checklistItemRepository.findByChecklistId(cl.getId()));
        }
        checklistRepository.deleteAll(checklists);

        // 2. Delete preferences
        userExamPreferenceRepository.deleteAll(userExamPreferenceRepository.findByUserId(userId));
        notificationPreferenceRepository.findByUserId(userId).ifPresent(notificationPreferenceRepository::delete);

        // 3. Delete registered devices
        deviceTokenRepository.deleteAll(deviceTokenRepository.findByUserIdAndIsActiveTrue(userId));

        // 4. Delete user profile
        profileRepository.deleteById(userId);

        return ApiResponse.ok("All candidate data permanently and securely erased.");
    }

    private ApplicationChecklistDto toChecklistDto(ApplicationChecklist cl) {
        List<ApplicationChecklistItem> items = checklistItemRepository.findByChecklistId(cl.getId());
        int order = 1;
        List<ApplicationChecklistItemDto> itemDtos = new ArrayList<>();
        for (ApplicationChecklistItem item : items) {
            itemDtos.add(new ApplicationChecklistItemDto(
                    item.getId(),
                    item.getItemKey(),
                    item.getTitle(),
                    item.getIsCompleted(),
                    order++
            ));
        }

        return new ApplicationChecklistDto(
                cl.getId(),
                null,
                cl.getExam() != null ? cl.getExam().getId() : null,
                cl.getExam() != null ? cl.getExam().getExamName() : null,
                cl.getExam() != null ? cl.getExam().getExamName() + " Application Checklist" : "Checklist",
                cl.getNotes(),
                itemDtos,
                cl.getCreatedAt(),
                cl.getUpdatedAt()
        );
    }
}
