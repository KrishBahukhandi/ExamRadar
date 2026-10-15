package com.examradar.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Discovery : Screen("discovery")
    object Calendar : Screen("calendar")
    object Checklist : Screen("checklist/{examId}") {
        fun createRoute(examId: String) = "checklist/$examId"
    }
    object NoticeDetail : Screen("notice/{noticeId}") {
        fun createRoute(noticeId: String) = "notice/$noticeId"
    }
    object ExamDetail : Screen("exam/{examId}") {
        fun createRoute(examId: String) = "exam/$examId"
    }
    object NotificationSettings : Screen("settings/notifications")
    object Account : Screen("account")
    object PrivacyPolicy : Screen("privacy")
}
