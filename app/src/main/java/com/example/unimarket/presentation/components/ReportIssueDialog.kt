package com.example.unimarket.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.unimarket.R
import com.example.unimarket.presentation.theme.AppBlue

private data class ReportReasonOption(
    val code: String,
    val labelResId: Int
)

@Composable
fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onSubmit: (reasonCode: String, reasonLabel: String, details: String) -> Unit,
    title: String = stringResource(R.string.report_dialog_title)
) {
    val options = listOf(
        ReportReasonOption("ITEM_NOT_AS_DESCRIBED", R.string.report_reason_item_not_as_described),
        ReportReasonOption("SELLER_UNRESPONSIVE", R.string.report_reason_seller_unresponsive),
        ReportReasonOption("INCORRECT_DELIVERY_LOCATION", R.string.report_reason_incorrect_delivery_location),
        ReportReasonOption("SUSPICIOUS_ACTIVITY", R.string.report_reason_suspicious_activity),
        ReportReasonOption("OTHER", R.string.report_reason_other)
    )
    val reasonLabelByCode = options.associate { it.code to stringResource(it.labelResId) }
    var selectedReasonCode by rememberSaveable { mutableStateOf(options.first().code) }
    var details by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReasonCode = option.code }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReasonCode == option.code,
                            onClick = { selectedReasonCode = option.code }
                        )
                        Text(text = stringResource(option.labelResId))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text(stringResource(R.string.report_details_placeholder)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reasonLabel = reasonLabelByCode[selectedReasonCode].orEmpty()
                    onSubmit(selectedReasonCode, reasonLabel, details.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
