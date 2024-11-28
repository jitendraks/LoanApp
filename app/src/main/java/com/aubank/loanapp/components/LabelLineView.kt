package com.aubank.loanapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LabelLineValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    dividerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.1f
    ),
    dividerThickness: Dp = 1.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        HorizontalDivider(
            thickness = dividerThickness,
            color = dividerColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }


}