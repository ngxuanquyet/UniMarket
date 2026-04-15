package com.example.unimarket.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.unimarket.R

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
