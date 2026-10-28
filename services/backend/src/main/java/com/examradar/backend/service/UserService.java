package com.examradar.backend.service;

import com.examradar.backend.domain.*;
import com.examradar.backend.dto.ApplicationChecklistDto;
import com.examradar.backend.dto.ApplicationChecklistItemDto;
import com.examradar.backend.dto.UserPreferencesDto;
import com.examradar.backend.repository.*;
import com.examradar.backend.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class UserService {

    private final UserProfileRepository profileRepository;
    private final UserExamPreferenceRepository userExamPreferenceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ApplicationChecklistRepository checklistRepository;
    private final ApplicationChecklistItemRepository checklistItemRepository;
    private final ExamRepository examRepository;
    private final ExamCategoryRepository categoryRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationJobRepository notificationJobRepository;

    public UserService(
            UserProfileRepository profileRepository,
            UserExamPreferenceRepository userExamPreferenceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ApplicationChecklistRepository checklistRepository,
            ApplicationChecklistItemRepository checklistItemRepository,
            ExamRepository examRepository,
            ExamCategoryRepository categoryRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationJobRepository notificationJobRepository
    ) {
        this.profileRepository = profileRepository;
        this.userExamPreferenceRepository = userExamPreferenceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.examRepository = examRepository;
        this.categoryRepository = categoryRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationJobRepository = notificationJobRepository;
    }

    public UserProfile getOrCreateUser(AuthenticatedUser authUser) {
        return profileRepository.findById(authUser.getId())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile(authUser.getId(), authUser.getEmail(), "Candidate");
                    return profileRepository.save(profile);
                });
    }

    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences(AuthenticatedUser authUser) {
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

        return new UserPreferencesDto(
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
    }

    public UserPreferencesDto updatePreferences(AuthenticatedUser authUser, UserPreferencesDto request) {
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
        Set<String> newCatIds = new HashSet<>(request.followedCategoryIds() != null ? request.followedCategoryIds() : List.of());

        // Remove unselected exams
        for (UserExamPreference p : existing) {
            if (p.getExam() != null && !newExamIds.contains(p.getExam().getId())) {
                userExamPreferenceRepository.delete(p);
            }
            if (p.getExam() == null && p.getCategory() != null && !newCatIds.contains(p.getCategory().getId())) {
                userExamPreferenceRepository.delete(p);
            }
        }

        // Add newly selected exams
        Set<String> alreadyFollowedExams = new HashSet<>(existing.stream()
                .filter(p -> p.getExam() != null)
                .map(p -> p.getExam().getId())
                .toList());

        for (String examId : newExamIds) {
            if (!alreadyFollowedExams.contains(examId)) {
                examRepository.findById(examId).ifPresent(exam -> {
                    UserExamPreference newPref = new UserExamPreference(user, exam, exam.getCategory());
                    newPref.setNotifyNewNotices(request.notifyNewNotices());
                    newPref.setNotifyDeadlines(request.notifyDeadlines());
                    newPref.setNotifyResults(request.notifyResults());
                    userExamPreferenceRepository.save(newPref);
                });
            }
        }

        // Add newly selected categories (if not already covered by followed exams)
        Set<String> alreadyFollowedCategories = new HashSet<>(existing.stream()
                .filter(p -> p.getExam() == null && p.getCategory() != null)
                .map(p -> p.getCategory().getId())
                .toList());

        for (String catId : newCatIds) {
            if (!alreadyFollowedCategories.contains(catId)) {
                categoryRepository.findById(catId).ifPresent(cat -> {
                    UserExamPreference newCatPref = new UserExamPreference(user, null, cat);
                    newCatPref.setNotifyNewNotices(request.notifyNewNotices());
                    newCatPref.setNotifyDeadlines(request.notifyDeadlines());
                    newCatPref.setNotifyResults(request.notifyResults());
                    userExamPreferenceRepository.save(newCatPref);
                });
            }
        }

        return request;
    }

    @Transactional(readOnly = true)
    public List<ApplicationChecklistDto> getChecklists(AuthenticatedUser authUser) {
        UserProfile user = getOrCreateUser(authUser);
        List<ApplicationChecklist> checklists = checklistRepository.findByUserId(user.getId());
        return checklists.stream().map(this::toChecklistDto).toList();
    }

    public ApplicationChecklistDto createChecklist(AuthenticatedUser authUser, String examId) {
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

        return toChecklistDto(checklist);
    }

    public ApplicationChecklistDto updateChecklistItem(AuthenticatedUser authUser, UUID checklistId, String itemKey, boolean isCompleted) {
        ApplicationChecklist cl = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new NoSuchElementException("Checklist not found: " + checklistId));

        if (!cl.getUser().getId().equals(authUser.getId())) {
            throw new AccessDeniedException("Not your checklist");
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

        return toChecklistDto(cl);
    }

    public void deleteUserData(AuthenticatedUser authUser) {
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
    }

    public ApplicationChecklistDto toChecklistDto(ApplicationChecklist cl) {
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

    @Transactional(readOnly = true)
    public List<com.examradar.backend.dto.NotificationInboxDto> getNotificationInbox(AuthenticatedUser authUser) {
        UserProfile user = getOrCreateUser(authUser);
        List<NotificationJob> jobs = notificationJobRepository.findTop50ByUserIdOrderByScheduledAtDesc(user.getId());
        return jobs.stream().map(job -> {
            Notice notice = job.getNotice();
            Exam exam = notice != null ? notice.getExam() : null;
            String examName = exam != null ? exam.getExamName() : "Official Examination";
            String org = exam != null ? exam.getOrganization() : (notice != null && notice.getSource() != null ? notice.getSource().getSourceName() : "Govt of India");

            String title = switch (job.getEventType()) {
                case NEW_NOTICE -> "New Notification: " + examName;
                case DEADLINE_7D -> "Deadline Alert: 7 Days Left — " + examName;
                case DEADLINE_3D -> "🚨 Urgent: 3 Days Left to Apply — " + examName;
                case DEADLINE_1D -> "⚠️ Final Day: Applications Close Today — " + examName;
                case DEADLINE_REVISED -> "Schedule Revised: " + examName;
                case RESULT_OUT -> "🏆 Result Declared: " + examName;
            };

            String message = notice != null ? notice.getTitle() : "Important recruitment announcement posted.";

            return new com.examradar.backend.dto.NotificationInboxDto(
                    job.getId(),
                    notice != null ? notice.getId() : null,
                    exam != null ? exam.getId() : null,
                    examName,
                    org,
                    job.getEventType(),
                    title,
                    message,
                    job.getScheduledAt(),
                    job.getStatus().name()
            );
        }).toList();
    }
}
