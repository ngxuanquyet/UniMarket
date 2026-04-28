package com.example.unimarket.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.unimarket.R

@Composable
fun SignUpScreen(
    onSignUpClick: (String, String, String, String, String) -> Unit,
    universityOptions: List<UniversityOption>,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var showUniversityDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val fullNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()
            onNavigateToLogin()
        }
    }

    AuthPatternScreen(
        patternResId = R.drawable.signup_parttern,
        headerHeight = 286,
        topSpacing = 158
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            AuthHeading(title = stringResource(R.string.auth_sign_up))

            Spacer(modifier = Modifier.height(26.dp))

            AuthLineField(
                label = stringResource(R.string.auth_full_name),
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = stringResource(R.string.auth_full_name_placeholder),
                leadingIcon = Icons.Default.Person,
                textFieldModifier = Modifier.focusRequester(fullNameFocusRequester)
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = stringResource(R.string.auth_email),
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = Icons.Default.School,
                textFieldModifier = Modifier.focusRequester(emailFocusRequester),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = stringResource(R.string.auth_phone_number),
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = stringResource(R.string.auth_phone_number_placeholder),
                leadingIcon = Icons.Default.Phone,
                textFieldModifier = Modifier.focusRequester(phoneFocusRequester),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = stringResource(R.string.auth_password),
                value = password,
                onValueChange = { password = it },
                placeholder = stringResource(R.string.auth_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
                textFieldModifier = Modifier.focusRequester(passwordFocusRequester),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingContent = {
                    PasswordToggle(
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = stringResource(R.string.auth_confirm_password),
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = stringResource(R.string.auth_confirm_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
                textFieldModifier = Modifier.focusRequester(confirmPasswordFocusRequester),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingContent = {
                    PasswordToggle(
                        visible = confirmPasswordVisible,
                        onToggle = { confirmPasswordVisible = !confirmPasswordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            AuthPrimaryButton(
                text = stringResource(R.string.auth_create_account),
                isLoading = isLoading,
                onClick = {
                    val trimmedFullName = fullName.trim()
                    val trimmedEmail = email.trim()

                    when {
                        trimmedFullName.isEmpty() -> {
                            fullNameFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_enter_full_name),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        trimmedEmail.isEmpty() -> {
                            emailFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_enter_email),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        phoneNumber.trim().isEmpty() -> {
                            phoneFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_enter_phone_number),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        password.isBlank() -> {
                            passwordFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_enter_password),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        confirmPassword.isBlank() -> {
                            confirmPasswordFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_confirm_password),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        password != confirmPassword -> {
                            confirmPasswordFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_passwords_mismatch),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }
                    }

                    showUniversityDialog = true
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            AuthBottomPrompt(
                prefix = stringResource(R.string.auth_have_account),
                action = stringResource(R.string.auth_login),
                onClick = onNavigateToLogin
            )
        }
    }

    if (showUniversityDialog) {
        UniversitySelectionDialog(
            title = stringResource(R.string.auth_university_dialog_title),
            value = university,
            onValueChange = { university = it },
            options = universityOptions,
            enabled = !isLoading,
            onDismiss = { showUniversityDialog = false },
            onInvalidSelection = {
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_error_select_university_from_list),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onConfirm = { selectedUniversity ->
                showUniversityDialog = false
                university = selectedUniversity.name
                onSignUpClick(
                    fullName.trim(),
                    email.trim(),
                    selectedUniversity.name,
                    password,
                    phoneNumber.trim()
                )
            }
        )
    }
}
