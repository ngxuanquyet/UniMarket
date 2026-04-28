package com.example.unimarket.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.TextGray

@Composable
fun PhoneNumberSetupScreen(
    onSendCodeClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
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
            AuthHeading(title = stringResource(R.string.auth_phone_setup_title))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.auth_phone_setup_subtitle),
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthLineField(
                label = stringResource(R.string.auth_phone_number),
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = stringResource(R.string.auth_phone_number_placeholder),
                leadingIcon = Icons.Default.Phone,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            AuthPrimaryButton(
                text = stringResource(R.string.auth_send_verification_code),
                isLoading = isLoading,
                onClick = { onSendCodeClick(phoneNumber.trim()) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthBottomPrompt(
                prefix = stringResource(R.string.common_back),
                action = stringResource(R.string.auth_sign_in),
                onClick = onNavigateBack
            )
        }
    }
}
