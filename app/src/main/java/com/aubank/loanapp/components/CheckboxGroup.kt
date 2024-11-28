package com.aubank.loanapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aubank.loanapp.data.VisitDone

@Composable
fun CheckboxGroup(label: String, options: List<Any>, param: (Any) -> Unit) {
    // State to keep track of which checkboxes are checked
    val checkedStates = remember { mutableStateListOf(*Array(options.size) { false }) }

    // State for custom input
    var customInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.Gray, // Border color, similar to OutlinedTextField
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = Color.White, // Background color
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp) // Inner padding
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Iterate over the options and display a checkbox for each one
        options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(option.toString())
                Checkbox(
                    checked = if (option.toString() == "Other") showCustomInput else checkedStates[index],
                    onCheckedChange = { isChecked ->
                        // Update the state when the checkbox is toggled
                        if (option.toString() == "Other") {
                            showCustomInput = isChecked
                            if (!isChecked) customInput = "" // Reset custom input when "Other" is unchecked
                        } else {
                            checkedStates[index] = isChecked
                        }
                    }
                )
            }
        }

        if (showCustomInput) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customInput,
                onValueChange = { newInput -> customInput = newInput },
                label = { Text("Enter custom value") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CheckboxGroupPreview() {
    CheckboxGroup("option1", listOf("option1", "option2")) {
        result ->
    }
}
