@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.chore

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LinkedCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.roomiesync.BuildConfig
import com.example.roomiesync.data.SupabaseClient
import com.example.roomiesync.ui.components.ChoreStatus
import com.example.roomiesync.ui.components.FullWidthButtonWithIcon
import com.example.roomiesync.ui.components.getRequiredLabel
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.WarningYellow
import com.example.roomiesync.utils.getRelativeTimeText
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreSubmissionDialog(
    choreAssignment: ChoreAssignment,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> photoUri = uri }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )
                    }
                    
                    Text(
                        text = "Submit Chore",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val chore = choreAssignment.chore
                        if (chore != null) {
                            // Chore Details
                            Text(
                                text = chore.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Determine status color
                            val visualStatus = try {
                                ChoreStatus.valueOf(choreAssignment.status)
                            } catch (e: IllegalArgumentException) {
                                ChoreStatus.NOT_URGENT
                            }
                            
                            val statusColor = when (visualStatus) {
                                ChoreStatus.NOT_URGENT -> Color.Black
                                ChoreStatus.URGENT -> WarningYellow
                                ChoreStatus.OVERDUE -> ErrorRed
                                ChoreStatus.PENDING_APPROVAL -> Color(0xFF2196F3)
                            }

                            Text(
                                text = getRelativeTimeText(choreAssignment.dueDate),
                                fontSize = 16.sp,
                                color = if (visualStatus == ChoreStatus.NOT_URGENT) Color.Gray else statusColor
                            )
                            
                            Text(
                                text = chore.description,
                                fontSize = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Photo Proof Section
                            Text(text = getRequiredLabel("Photo proof"), fontWeight = FontWeight.Bold)
                            
                            if (photoUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Proof photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Button to change photo
                                    IconButton(
                                        onClick = { 
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(100))
                                    ) {
                                        Icon(Icons.Outlined.LinkedCamera, "Change photo")
                                    }
                                }
                            } else {
                                FullWidthButtonWithIcon(
                                    text = "Upload chore proof",
                                    icon = Icons.Outlined.LinkedCamera,
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = ErrorRed,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            // Submit Button
                            FullWidthButtonWithIcon(
                                text = "Submit chore for review",
                                icon = Icons.Filled.Check,
                                enabled = photoUri != null,
                                onClick = {
                                    photoUri?.let { uri ->
                                        isLoading = true
                                        errorMessage = null
                                        scope.launch {
                                            try {
                                                val publicUrl = withContext(Dispatchers.IO) {
                                                    val contentResolver = context.contentResolver
                                                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                                        ?: throw Exception("Could not read file data")

                                                    // Generate unique filename
                                                    val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                                                    val bucket = SupabaseClient.client.storage.from("chore_validation_photos")
                                                    
                                                    // Upload file
                                                    bucket.upload(fileName, bytes)
                                                    
                                                    // Get public URL using SUPABASE_S3_URL as requested
                                                    "${BuildConfig.SUPABASE_S3_URL}/chore_validation_photos/$fileName"
                                                }
                                                onSubmit(publicUrl)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                errorMessage = e.message ?: "Upload failed"
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
