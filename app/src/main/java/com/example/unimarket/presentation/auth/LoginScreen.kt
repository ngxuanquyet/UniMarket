package com.example.unimarket.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.AuthAccent
import com.example.unimarket.presentation.theme.AuthPlaceholder
import com.example.unimarket.presentation.theme.AuthUnderline
import com.example.unimarket.presentation.theme.TextDark
import com.example.unimarket.presentation.theme.TextGray
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onGoogleLoginClick: (String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    AuthPatternScreen(
        headerHeight = 456,
        topSpacing = 188,
        contentBelowHeader = true,
        contentTopPadding = 0,
        contentOffsetY = -132
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            AuthHeading(title = stringResource(R.string.auth_sign_in))

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = stringResource(R.string.auth_email),
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = Icons.Default.School,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthLineField(
                label = stringResource(R.string.auth_password),
                value = password,
                onValueChange = { password = it },
                placeholder = stringResource(R.string.auth_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AuthAccent,
                            uncheckedColor = AuthUnderline,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auth_remember_me),
                        fontSize = 12.sp,
                        color = TextDark
                    )
                }

                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    fontSize = 12.sp,
                    color = AuthAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            AuthPrimaryButton(
                text = stringResource(R.string.auth_login),
                isLoading = isLoading,
                onClick = { onLoginClick(email.trim(), password) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                                context.getString(R.string.default_web_client_id)
                            ).build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(signInWithGoogleOption)
                                .build()

                            val result = credentialManager.getCredential(
                                request = request,
                                context = context
                            )
                            val credential = result.credential

                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                onGoogleLoginClick(googleIdTokenCredential.idToken)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.auth_google_unexpected_credential),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: NoCredentialException) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_google_no_account),
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (_: GetCredentialCancellationException) {
                            // User dismissed the account picker; no UI feedback is needed.
                        } catch (e: GetCredentialException) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_google_failed, e.message ?: ""),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_generic_error, e.message ?: ""),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                border = BorderStroke(1.dp, AuthUnderline)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = stringResource(R.string.auth_google),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auth_continue_with_google),
                    color = TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthBottomPrompt(
                prefix = stringResource(R.string.auth_no_account),
                action = stringResource(R.string.auth_sign_up),
                onClick = onNavigateToSignUp
            )
        }
    }
}
