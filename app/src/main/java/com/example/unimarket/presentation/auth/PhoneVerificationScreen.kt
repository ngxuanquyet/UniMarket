package com.example.unimarket.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.AuthAction
import com.example.unimarket.presentation.theme.AuthAccent
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TextGray
import kotlinx.coroutines.delay

private const val RESEND_CODE_COOLDOWN_SECONDS = 60

@Composable
fun PhoneVerificationScreen(
    phoneNumber: String,
    onVerifyClick: (String) -> Unit,
    onResendCodeClick: () -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    showSuccessDialog: Boolean = false,
    successDialogMessage: String = stringResource(R.string.auth_verification_success_message),
    successDialogAction: String = stringResource(R.string.auth_continue_to_home),
    onSuccessDialogContinue: () -> Unit = {}
) {
    var verificationCode by remember { mutableStateOf("") }
    var resendSecondsRemaining by remember(phoneNumber) {
        mutableStateOf(RESEND_CODE_COOLDOWN_SECONDS)
    }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(resendSecondsRemaining, showSuccessDialog) {
        if (!showSuccessDialog && resendSecondsRemaining > 0) {
            delay(1_000)
            resendSecondsRemaining -= 1
        }
    }

    AuthPatternScreen(
        patternResId = R.drawable.signup_parttern,
        headerHeight = 286,
        contentBelowHeader = true,
        contentTopPadding = 16
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            AuthHeading(title = stringResource(R.string.auth_verify_number_title))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.auth_verify_number_subtitle, phoneNumber),
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthLineField(
                label = stringResource(R.string.auth_verification_code),
                value = verificationCode,
                onValueChange = { verificationCode = it.filter(Char::isDigit).take(4) },
                placeholder = stringResource(R.string.auth_verification_code_placeholder),
                leadingIcon = Icons.Filled.Phone,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_did_not_receive_code),
                    color = TextGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                val canResend = !isLoading && resendSecondsRemaining == 0
                TextButton(
                    onClick = {
                        if (canResend) {
                            resendSecondsRemaining = RESEND_CODE_COOLDOWN_SECONDS
                            onResendCodeClick()
                        }
                    },
                    enabled = canResend
                ) {
                    Text(
                        text = if (resendSecondsRemaining > 0) {
                            stringResource(
                                R.string.auth_resend_code_countdown,
                                resendSecondsRemaining
                            )
                        } else {
                            stringResource(R.string.auth_resend_code)
                        },
                        color = if (canResend) AuthAccent else TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AuthPrimaryButton(
                text = stringResource(R.string.auth_verify),
                isLoading = isLoading,
                onClick = { onVerifyClick(verificationCode.trim()) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(onClick = onNavigateBack, enabled = !isLoading) {
                Text(text = stringResource(R.string.common_back))
            }
        }
    }

    if (showSuccessDialog) {
        VerificationSuccessDialog(
            message = successDialogMessage,
            actionText = successDialogAction,
            onContinue = onSuccessDialogContinue
        )
    }
}

@Composable
private fun VerificationSuccessDialog(
    message: String,
    actionText: String,
    onContinue: () -> Unit
) {
    val latestOnContinue by rememberUpdatedState(onContinue)
    var secondsRemaining by remember { mutableStateOf(5) }

    LaunchedEffect(Unit) {
        secondsRemaining = 5
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining -= 1
        }
        latestOnContinue()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFDCEBFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(AuthAction, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.auth_verification_success_title),
                    color = Color(0xFF27294F),
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    color = Color(0xFF7A83A4),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .background(Color(0xFFFCE0F6), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF8A3D7B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.auth_verified_student_account),
                        color = Color(0xFF8A3D7B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = latestOnContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuthAction,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = actionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(
                        R.string.auth_redirecting_in_seconds,
                        secondsRemaining.coerceAtLeast(0)
                    ).uppercase(),
                    color = Color(0xFF9AA0BE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
