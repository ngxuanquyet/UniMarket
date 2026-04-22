package com.example.unimarket.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.example.unimarket.R
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.auth.LoginScreen
import com.example.unimarket.presentation.auth.PhoneNumberSetupScreen
import com.example.unimarket.presentation.auth.PhoneVerificationScreen
import com.example.unimarket.presentation.auth.resolveUniversitySelection
import com.example.unimarket.presentation.auth.SignUpScreen
import com.example.unimarket.presentation.auth.UniversitySuggestionField
import com.example.unimarket.presentation.auth.WelcomeScreen
import com.example.unimarket.presentation.auth.state.AuthState
import com.example.unimarket.presentation.auth.state.PhoneVerificationFlow
import com.example.unimarket.presentation.main.MainScreen

@Composable
fun RootNavGraph(
    navController: NavHostController,
    pendingConversationId: String? = null,
    onConversationIntentConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val universityListViewModel: UniversityListViewModel = hiltViewModel()
    val sessionState = sessionViewModel.uiState.collectAsStateWithLifecycle()
    val universityListState = universityListViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    var universityInput by remember { mutableStateOf("") }

    val startDestination = if (sessionState.value.isAuthenticated && !sessionState.value.isPhoneRequired) {
        Screen.MainGraph.route
    } else {
        Screen.AuthGraph.route
    }

    LaunchedEffect(sessionState.value.isAuthenticated, currentBackStackEntry.value) {
        val currentDestination = currentBackStackEntry.value?.destination ?: return@LaunchedEffect
        val targetRoute = if (sessionState.value.isAuthenticated && !sessionState.value.isPhoneRequired) {
            Screen.MainGraph.route
        } else {
            Screen.AuthGraph.route
        }

        val isAlreadyOnTarget = currentDestination.hierarchy.any { it.route == targetRoute }
        if (!isAlreadyOnTarget) {
            navController.navigate(targetRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        route = "root_graph"
    ) {
        navigation(
            startDestination = Screen.Welcome.route,
            route = Screen.AuthGraph.route
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onContinueClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Login.route) { backStackEntry ->
                val authGraphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.AuthGraph.route)
                }
                val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
                val authState = viewModel.authState.collectAsStateWithLifecycle()
                LaunchedEffect(authState.value) {
                    when (val state = authState.value) {
                        is AuthState.PhoneNumberRequired -> {
                            navController.navigate(Screen.PhoneSetup.route) {
                                launchSingleTop = true
                            }
                            viewModel.consumeNavigationState()
                        }

                        is AuthState.VerificationRequired -> {
                            navController.navigate(
                                Screen.PhoneVerification.route(
                                    phone = Uri.encode(state.phoneNumber),
                                    flow = state.flow.name
                                )
                            ) {
                                launchSingleTop = true
                            }
                            viewModel.consumeNavigationState()
                        }

                        else -> Unit
                    }
                }

                LoginScreen(
                    onLoginClick = { email, password -> viewModel.login(email, password) },
                    onGoogleLoginClick = { idToken -> viewModel.loginWithGoogle(idToken) },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = (authState.value as? AuthState.Error)?.message
                ) 
            }
            composable(Screen.SignUp.route) { backStackEntry ->
                val authGraphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.AuthGraph.route)
                }
                val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
                val authState = viewModel.authState.collectAsStateWithLifecycle()
                LaunchedEffect(authState.value) {
                    when (val state = authState.value) {
                        is AuthState.VerificationRequired -> {
                            navController.navigate(
                                Screen.PhoneVerification.route(
                                    phone = Uri.encode(state.phoneNumber),
                                    flow = state.flow.name
                                )
                            ) {
                                launchSingleTop = true
                            }
                            viewModel.consumeNavigationState()
                        }
                        else -> Unit
                    }
                }

                SignUpScreen(
                    onSignUpClick = { name, email, university, password, phoneNumber ->
                        viewModel.signUp(name, email, university, password, phoneNumber)
                    },
                    universityOptions = universityListState.value.options,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToLogin = { navController.popBackStack() },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = (authState.value as? AuthState.Error)?.message,
                    successMessage = (authState.value as? AuthState.Success)?.message
                ) 
            }
            composable(Screen.PhoneSetup.route) { backStackEntry ->
                val authGraphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.AuthGraph.route)
                }
                val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
                val authState = viewModel.authState.collectAsStateWithLifecycle()
                LaunchedEffect(authState.value) {
                    when (val state = authState.value) {
                        is AuthState.VerificationRequired -> {
                            navController.navigate(
                                Screen.PhoneVerification.route(
                                    phone = Uri.encode(state.phoneNumber),
                                    flow = state.flow.name
                                )
                            ) {
                                launchSingleTop = true
                            }
                            viewModel.consumeNavigationState()
                        }
                        else -> Unit
                    }
                }
                PhoneNumberSetupScreen(
                    onSendCodeClick = { phoneNumber ->
                        viewModel.requestGooglePhoneVerification(phoneNumber)
                    },
                    onNavigateBack = { navController.navigateUp() },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = (authState.value as? AuthState.Error)?.message
                )
            }
            composable(
                route = Screen.PhoneVerification.route,
                arguments = listOf(
                    navArgument("phone") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("flow") {
                        type = NavType.StringType
                        defaultValue = PhoneVerificationFlow.SIGN_UP.name
                    }
                )
            ) { backStackEntry ->
                val authGraphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.AuthGraph.route)
                }
                val viewModel: AuthViewModel = hiltViewModel(authGraphEntry)
                val authState = viewModel.authState.collectAsStateWithLifecycle()
                val encodedPhone = backStackEntry.arguments?.getString("phone").orEmpty()
                val flowName = backStackEntry.arguments?.getString("flow").orEmpty()
                val phone = Uri.decode(encodedPhone)
                val flow = runCatching { PhoneVerificationFlow.valueOf(flowName) }
                    .getOrDefault(PhoneVerificationFlow.SIGN_UP)

                LaunchedEffect(authState.value) {
                    val state = authState.value as? AuthState.Success ?: return@LaunchedEffect
                    if (flow == PhoneVerificationFlow.SIGN_UP) {
                        if (!state.message.isNullOrBlank()) {
                            android.widget.Toast.makeText(
                                context,
                                state.message,
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    viewModel.resetState()
                }

                PhoneVerificationScreen(
                    phoneNumber = phone,
                    onVerifyClick = { code -> viewModel.verifyPhoneCode(code) },
                    onResendCodeClick = { viewModel.resendPhoneCode() },
                    onNavigateBack = { navController.navigateUp() },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = (authState.value as? AuthState.Error)?.message
                )
            }
        }

        composable(route = Screen.MainGraph.route) {
            MainScreen(
                rootNavController = navController,
                pendingConversationId = pendingConversationId,
                onConversationIntentConsumed = onConversationIntentConsumed
            )
        }
    }

    if (sessionState.value.isAccountLocked) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Tài khoản đã bị khóa") },
            text = { Text("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.") },
            confirmButton = {
                TextButton(onClick = { sessionViewModel.consumeAccountLockedNotice() }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    if (sessionState.value.isAuthenticated && sessionState.value.isUniversityRequired) {
        val selectedUniversity = resolveUniversitySelection(
            options = universityListState.value.options,
            input = universityInput
        )
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.auth_university_dialog_title)) },
            text = {
                UniversitySuggestionField(
                    value = universityInput,
                    onValueChange = { universityInput = it },
                    options = universityListState.value.options,
                    enabled = !sessionState.value.isUpdatingUniversity,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedUniversity == null) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_select_university_from_list),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        sessionViewModel.updateUniversity(selectedUniversity.name)
                        universityInput = ""
                    },
                    enabled = selectedUniversity != null && !sessionState.value.isUpdatingUniversity
                ) {
                    Text(text = stringResource(R.string.common_save))
                }
            }
        )
    }
}
