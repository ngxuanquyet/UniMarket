package com.example.unimarket.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.AuthAction
import com.example.unimarket.presentation.theme.AuthAccent
import com.example.unimarket.presentation.theme.AuthIcon
import com.example.unimarket.presentation.theme.AuthPlaceholder
import com.example.unimarket.presentation.theme.AuthUnderline
import com.example.unimarket.presentation.theme.SurfaceWhite
import com.example.unimarket.presentation.theme.TextDark
import com.example.unimarket.presentation.theme.TextGray

@Composable
fun AuthPatternScreen(
    modifier: Modifier = Modifier,
    patternResId: Int = R.drawable.login_parttern,
    headerHeight: Int = 310,
    topSpacing: Int = 178,
    horizontalPadding: Int = 28,
    contentBelowHeader: Boolean = false,
    contentTopPadding: Int = 0,
    contentOffsetY: Int = 0,
    content: @Composable BoxScope.() -> Unit
) {
    if (contentBelowHeader) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SurfaceWhite)
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(patternResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = contentOffsetY.dp)
                    .padding(horizontal = horizontalPadding.dp)
                    .padding(top = contentTopPadding.dp, bottom = 28.dp)
            ) {
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SurfaceWhite)
        ) {
            Image(
                painter = painterResource(patternResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight.dp)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding.dp)
                    .padding(top = topSpacing.dp, bottom = 28.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun AuthHeading(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDark
            ),
            fontSize = 34.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(3.dp)
                .background(AuthAccent, CircleShape)
        )
    }
}

@Composable
fun AuthLineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable (() -> Unit))? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextDark,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = AuthIcon,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = AuthPlaceholder,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = textFieldModifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = TextDark,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = keyboardOptions,
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    cursorBrush = SolidColor(AuthAccent)
                )
            }
            trailingContent?.invoke()
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = AuthUnderline
        )
    }
}

@Composable
fun PasswordToggle(
    visible: Boolean,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(20.dp)
    ) {
        Icon(
            imageVector = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            contentDescription = stringResource(R.string.auth_toggle_password_visibility),
            tint = AuthIcon,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AuthPrimaryButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthAction,
            contentColor = Color.White
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AuthBottomPrompt(
    prefix: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prefix,
            color = TextGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = action,
            color = AuthAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}
