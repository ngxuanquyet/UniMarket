package com.example.unimarket.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.unimarket.R

@Composable
fun SignUpScreen(
    onSignUpClick: (String, String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val fullNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
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
            AuthHeading(title = "Sign up")

            Spacer(modifier = Modifier.height(26.dp))

            AuthLineField(
                label = "Full name",
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "Your display name",
                leadingIcon = Icons.Default.Person,
                textFieldModifier = Modifier.focusRequester(fullNameFocusRequester)
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                placeholder = "demo@email.com",
                leadingIcon = Icons.Default.School,
                textFieldModifier = Modifier.focusRequester(emailFocusRequester),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                placeholder = "Enter your password",
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
                label = "Confirm Password",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirm your password",
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
                text = "Create Account",
                isLoading = isLoading,
                onClick = {
                    val trimmedFullName = fullName.trim()
                    val trimmedEmail = email.trim()

                    when {
                        trimmedFullName.isEmpty() -> {
                            fullNameFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                "Please enter your full name",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        trimmedEmail.isEmpty() -> {
                            emailFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                "Please enter your email",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        password.isBlank() -> {
                            passwordFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                "Please enter your password",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        confirmPassword.isBlank() -> {
                            confirmPasswordFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                "Please confirm your password",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }

                        password != confirmPassword -> {
                            confirmPasswordFocusRequester.requestFocus()
                            Toast.makeText(
                                context,
                                "Passwords do not match",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AuthPrimaryButton
                        }
                    }

                    onSignUpClick(trimmedFullName, trimmedEmail, password)
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            AuthBottomPrompt(
                prefix = "Already have an Account?",
                action = "Login",
                onClick = onNavigateToLogin
            )
        }
    }
}
