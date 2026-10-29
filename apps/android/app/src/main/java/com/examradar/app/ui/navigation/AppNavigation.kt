package com.examradar.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examradar.app.data.repository.ExamRadarRepository

import com.examradar.app.ui.components.LoadingStateView
import com.examradar.app.ui.screens.account.AccountScreen
import com.examradar.app.ui.screens.account.AccountViewModel
import com.examradar.app.ui.screens.account.PrivacyPolicyScreen
import com.examradar.app.ui.screens.calendar.CalendarScreen
import com.examradar.app.ui.screens.calendar.CalendarViewModel
import com.examradar.app.ui.screens.checklist.ChecklistScreen
import com.examradar.app.ui.screens.checklist.ChecklistViewModel
import com.examradar.app.ui.screens.details.NoticeDetailScreen
import com.examradar.app.ui.screens.details.NoticeDetailViewModel
import com.examradar.app.ui.screens.discovery.DiscoveryScreen
import com.examradar.app.ui.screens.discovery.DiscoveryViewModel
import com.examradar.app.ui.screens.home.HomeScreen
import com.examradar.app.ui.screens.home.HomeViewModel
import com.examradar.app.ui.screens.notifications.NotificationInboxScreen
import com.examradar.app.ui.screens.notifications.NotificationInboxViewModel
import com.examradar.app.ui.screens.notifications.NotificationSettingsScreen
import com.examradar.app.ui.screens.notifications.NotificationSettingsViewModel
import com.examradar.app.ui.screens.auth.AuthScreen
import com.examradar.app.ui.screens.auth.AuthViewModel
import com.examradar.app.ui.screens.onboarding.OnboardingScreen
import com.examradar.app.ui.screens.onboarding.OnboardingViewModel

@Suppress("UNCHECKED_CAST")
inline fun <reified VM : ViewModel> viewModelFactory(crossinline creator: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }

@Composable
fun AppNavigation(
    repository: ExamRadarRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(navController) {
        val activity = context as? androidx.activity.ComponentActivity
        val consumer = androidx.core.util.Consumer<android.content.Intent> { intent ->
            navController.handleDeepLink(intent)
        }
        activity?.addOnNewIntentListener(consumer)
        onDispose {
            activity?.removeOnNewIntentListener(consumer)
        }
    }

    val userPrefs by repository.getUserPreferences().collectAsState(initial = null)

    LaunchedEffect(userPrefs) {
        if (userPrefs != null) {
            val activity = context as? androidx.activity.ComponentActivity
            val intent = activity?.intent
            if (intent?.action == android.content.Intent.ACTION_VIEW && intent.data != null) {
                navController.handleDeepLink(intent)
                activity.intent = android.content.Intent(activity, com.examradar.app.MainActivity::class.java)
            }
        }
    }

    if (userPrefs == null) {
        LoadingStateView()
        return
    }

    val startDestination = if (userPrefs!!.isOnboardingCompleted) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val bottomNavRoutes = setOf(
        Screen.Home.route,
        Screen.Discovery.route,
        Screen.Calendar.route,
        Screen.Account.route
    )
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Discover") },
                        label = { Text("Discover") },
                        selected = currentRoute == Screen.Discovery.route,
                        onClick = {
                            navController.navigate(Screen.Discovery.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                        label = { Text("Calendar") },
                        selected = currentRoute == Screen.Calendar.route,
                        onClick = {
                            navController.navigate(Screen.Calendar.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                        label = { Text("Account") },
                        selected = currentRoute == Screen.Account.route,
                        onClick = {
                            navController.navigate(Screen.Account.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(paddingValues)
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = viewModelFactory { OnboardingViewModel(repository) }
                )
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home Dashboard
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory { HomeViewModel(repository) }
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNoticeClick = { noticeId ->
                        navController.navigate(Screen.NoticeDetail.createRoute(noticeId))
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.NotificationInbox.route)
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Discovery.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Discovery
            composable(Screen.Discovery.route) {
                val discoveryViewModel: DiscoveryViewModel = viewModel(
                    factory = viewModelFactory { DiscoveryViewModel(repository) }
                )
                DiscoveryScreen(
                    viewModel = discoveryViewModel,
                    onExamClick = { examId ->
                        navController.navigate(Screen.Checklist.createRoute(examId))
                    }
                )
            }

            // Calendar
            composable(Screen.Calendar.route) {
                val calendarViewModel: CalendarViewModel = viewModel(
                    factory = viewModelFactory { CalendarViewModel(repository) }
                )
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onNoticeClick = { noticeId ->
                        navController.navigate(Screen.NoticeDetail.createRoute(noticeId))
                    }
                )
            }

            // Notice Details
            composable(
                route = Screen.NoticeDetail.route,
                arguments = listOf(navArgument("noticeId") { type = NavType.StringType }),
                deepLinks = listOf(
                    androidx.navigation.navDeepLink { uriPattern = "https://app.examradar.in/notices/{noticeId}" },
                    androidx.navigation.navDeepLink { uriPattern = "https://app.examradar.in/notice/{noticeId}" },
                    androidx.navigation.navDeepLink { uriPattern = "examradar://notices/{noticeId}" },
                    androidx.navigation.navDeepLink { uriPattern = "examradar://notice/{noticeId}" }
                )
            ) { backStackEntry ->
                val noticeId = backStackEntry.arguments?.getString("noticeId") ?: ""
                val detailViewModel: NoticeDetailViewModel = viewModel(
                    key = "notice_$noticeId",
                    factory = viewModelFactory { NoticeDetailViewModel(repository, noticeId) }
                )
                NoticeDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onOpenChecklist = { examId ->
                        navController.navigate(Screen.Checklist.createRoute(examId))
                    }
                )
            }

            // Application Checklist
            composable(
                route = Screen.Checklist.route,
                arguments = listOf(navArgument("examId") { type = NavType.StringType })
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getString("examId") ?: ""
                val checklistViewModel: ChecklistViewModel = viewModel(
                    key = "exam_$examId",
                    factory = viewModelFactory { ChecklistViewModel(repository, examId) }
                )
                ChecklistScreen(
                    viewModel = checklistViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // In-App Notification History / Alert Inbox
            composable(Screen.NotificationInbox.route) {
                val inboxViewModel: NotificationInboxViewModel = viewModel(
                    factory = viewModelFactory { NotificationInboxViewModel(repository) }
                )
                NotificationInboxScreen(
                    viewModel = inboxViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.NotificationSettings.route) },
                    onNoticeClick = { noticeId ->
                        navController.navigate(Screen.NoticeDetail.createRoute(noticeId))
                    }
                )
            }

            // Notification Settings
            composable(Screen.NotificationSettings.route) {
                val notifViewModel: NotificationSettingsViewModel = viewModel(
                    factory = viewModelFactory { NotificationSettingsViewModel(repository) }
                )
                NotificationSettingsScreen(
                    viewModel = notifViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Account & Privacy
            composable(Screen.Account.route) {
                val accountViewModel: AccountViewModel = viewModel(
                    factory = viewModelFactory { AccountViewModel(repository) }
                )
                AccountScreen(
                    viewModel = accountViewModel,
                    onNavigateToNotifications = {
                        navController.navigate(Screen.NotificationSettings.route)
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(Screen.PrivacyPolicy.route)
                    },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onResetCompleted = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Authentication (Sign In & Sign Up)
            composable(Screen.Auth.route) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = viewModelFactory { AuthViewModel(repository) }
                )
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        navController.popBackStack()
                    },
                    onSkip = {
                        navController.popBackStack()
                    }
                )
            }

            // Privacy Policy Details
            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
