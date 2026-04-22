package com.example.unimarket.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.AuthAccent
import com.example.unimarket.presentation.theme.TextGray

@Composable
fun PhoneVerificationScreen(
    phoneNumber: String,
    onVerifyClick: (String) -> Unit,
    onResendCodeClick: () -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var verificationCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
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
                TextButton(onClick = onResendCodeClick, enabled = !isLoading) {
                    Text(
                        text = stringResource(R.string.auth_resend_code),
                        color = AuthAccent
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
}
