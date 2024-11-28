/*
package com.aubank.loanapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
fun ShowDialog(
    openDialog: Boolean,
    onDismiss: () -> Unit,
    title: String = "Title",
    message: String = "Message",
    positiveButtonText: String = "OK",
    onPositiveButtonClick: () -> Unit,
    negativeButtonText: String? = null,
    onNegativeButtonClick: () -> Unit = {}
) {
    if (openDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            content = {
                DialogContent {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(message)
                }
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.End
                ) {
                    if (negativeButtonText != null) {
                        TextButton(
                            onClick = onNegativeButtonClick
                        ) {
                            Text(negativeButtonText)
                        }
                    }
                    Button(
                        onClick = onPositiveButtonClick
                    ) {
                        Text(positiveButtonText)
                    }
                }
            }
        )
    }
}*/
