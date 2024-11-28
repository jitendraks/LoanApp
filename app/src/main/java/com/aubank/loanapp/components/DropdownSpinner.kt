package com.aubank.loanapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSpinner(label: String, options: List<Any>, callback: (Any) -> Unit) {
    // State to keep track of expanded dropdown
    var expanded by remember { mutableStateOf(false) }

    // State to keep track of selected option
    var selectedOption: Any by remember { mutableStateOf("") }

    // var selectedOption by remember { mutableStateOf(if(options.isEmpty() ) "" else options.first()) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        if (selectedOption !is String)
            callback.invoke(selectedOption)
        OutlinedTextField(
            value = selectedOption.toString(),
            onValueChange = { callback.invoke(selectedOption) },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(), // Ensure the menu is anchored to the text field
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                        // Call the callback here with the selected option
                        callback(option)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownSpinnerWithOtherOptionPreview() {
    val options = listOf("Option 1", "Option 2", "Option 3", "Other")
    DropdownSpinner(label = "Select an option", options = options, callback = { result ->
    })
}
