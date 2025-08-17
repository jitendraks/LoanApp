package com.aubank.loanapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PresenceCard(
    title: String,
    address: String,
    time: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = {},
                label = { Text("Address") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = time,
                onValueChange = {},
                label = { Text("Time") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            actionLabel?.let { label ->
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onAction!!,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label)
                }
            }
        }
    }
}
