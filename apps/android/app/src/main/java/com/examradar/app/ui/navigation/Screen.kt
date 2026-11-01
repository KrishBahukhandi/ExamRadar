package com.examradar.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Discovery : Screen("discovery")
    object Study : Screen("study")
    object Calendar : Screen("calendar")
    object Checklist : Screen("checklist/{examId}") {
        fun createRoute(examId: String) = "checklist/$examId"
    }
    object NoticeDetail : Screen("notice/{noticeId}") {
        fun createRoute(noticeId: String) = "notice/$noticeId"
    }
    object PdfViewer : Screen("pdf_viewer/{noticeId}") {
        fun createRoute(noticeId: String) = "pdf_viewer/$noticeId"
    }
    object ExamDetail : Screen("exam/{examId}") {
        fun createRoute(examId: String) = "exam/$examId"
    }
    object NotificationInbox : Screen("notifications/inbox")
    object NotificationSettings : Screen("settings/notifications")
    object Account : Screen("account")
    object PrivacyPolicy : Screen("privacy")
    object Auth : Screen("auth")
    object Vault : Screen("vault")
    object LocalPdfViewer : Screen("pdf_file?path={path}&title={title}") {
        fun createRoute(path: String, title: String): String {
            val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "pdf_file?path=$encodedPath&title=$encodedTitle"
        }
    }
}
