package com.example.myapplication.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSpinner(label: String, options: List<String>) {
    // State to keep track of expanded dropdown
    var expanded by remember { mutableStateOf(false) }

    // State to keep track of selected option
    var selectedOption by remember { mutableStateOf(options.first()) }

    // State to handle custom input when "Other" is selected
    var customInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(), // Ensure the menu is anchored to the text field
            colors = TextFieldDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        if (option == "Other") {
                            selectedOption = customInput
                            showCustomInput = true
                        } else {
                            selectedOption = option
                            showCustomInput = false
                        }
                        expanded = false
                    }
                )
            }

            if (showCustomInput) {
                DropdownMenuItem(
                    text = {
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { newInput -> customInput = newInput },
                            label = { Text("Enter custom value") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {} // No action needed on click
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownSpinnerWithOtherOptionPreview() {
    val options = listOf("Option 1", "Option 2", "Option 3", "Other")
    DropdownSpinner(label = "Select an option", options = options)
}
