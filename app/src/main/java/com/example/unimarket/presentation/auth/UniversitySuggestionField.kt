package com.example.unimarket.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.unimarket.R

private val UniversityDialogBlue = Color(0xFF006FEF)
private val UniversityDialogLightBlue = Color(0xFFEFF6FF)
private val UniversityDialogDivider = Color(0xFFE5EAF0)
private val UniversityDialogPlaceholder = Color(0xFF8A94A6)
private val UniversityDialogText = Color(0xFF202124)

@Composable
fun UniversitySuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<UniversityOption>,
    enabled: Boolean = true
) {
    val suggestions = remember(value, options) {
        filterUniversityOptions(options = options, query = value)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.auth_university_label)) },
            placeholder = { Text(text = stringResource(R.string.auth_university_placeholder)) }
        )

        if (suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                items(suggestions, key = { it.id }) { item ->
                    Text(
                        text = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onValueChange(item.name) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun UniversitySelectionDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<UniversityOption>,
    onConfirm: (UniversityOption) -> Unit,
    enabled: Boolean = true,
    showDismissButton: Boolean = true,
    onDismiss: () -> Unit = {},
    onInvalidSelection: () -> Unit = {}
) {
    val selectedUniversity = remember(options, value) {
        resolveUniversitySelection(options = options, input = value)
    }
    val suggestions = remember(value, options) {
        filterUniversityOptions(options = options, query = value)
    }

    Dialog(
        onDismissRequest = {
            if (showDismissButton && enabled) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = showDismissButton && enabled,
            dismissOnClickOutside = showDismissButton && enabled,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 18.dp)
            ) {
                UniversityDialogHeader(title = title)

                Spacer(modifier = Modifier.height(20.dp))

                UniversityDialogInput(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled
                )

                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    UniversityDialogSuggestionList(
                        suggestions = suggestions,
                        enabled = enabled,
                        onValueChange = onValueChange
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                UniversityDialogActions(
                    showDismissButton = showDismissButton,
                    enabled = enabled,
                    onDismiss = onDismiss,
                    onConfirm = {
                        val selection = selectedUniversity
                        if (selection == null) {
                            onInvalidSelection()
                        } else {
                            onConfirm(selection)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UniversityDialogHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(UniversityDialogLightBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = UniversityDialogBlue,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = UniversityDialogBlue,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UniversityDialogInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.auth_university_label),
            color = UniversityDialogBlue,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.auth_university_placeholder),
                    color = UniversityDialogPlaceholder
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UniversityDialogBlue,
                unfocusedBorderColor = UniversityDialogBlue,
                disabledBorderColor = UniversityDialogBlue.copy(alpha = 0.45f),
                cursorColor = UniversityDialogBlue,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White
            )
        )
    }
}

@Composable
private fun UniversityDialogSuggestionList(
    suggestions: List<UniversityOption>,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
    ) {
        items(suggestions, key = { it.id }) { item ->
            UniversityDialogSuggestionRow(
                item = item,
                enabled = enabled,
                onValueChange = onValueChange
            )
            HorizontalDivider(color = UniversityDialogDivider, thickness = 1.dp)
        }
    }
}

@Composable
private fun UniversityDialogSuggestionRow(
    item: UniversityOption,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(enabled = enabled) { onValueChange(item.name) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(UniversityDialogLightBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = UniversityDialogBlue,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = item.name,
            color = UniversityDialogText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UniversityDialogActions(
    showDismissButton: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (showDismissButton) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, UniversityDialogDivider),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = UniversityDialogBlue
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = onConfirm,
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = UniversityDialogBlue,
                contentColor = Color.White
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.common_save),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
