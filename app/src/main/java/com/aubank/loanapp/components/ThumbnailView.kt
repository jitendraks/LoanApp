package com.aubank.loanapp.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ThumbnailView(imageUri: Uri?) {
    if (imageUri != null) {
        // Load image from the URI using Coil
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "Captured Image",
            modifier = Modifier
                .size(150.dp) // Adjust size for thumbnail
                .padding(8.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder if no image is available
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No Image")
        }
    }
}