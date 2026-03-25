package com.example.roomiesync.profile

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.roomiesync.BuildConfig
import com.example.roomiesync.ui.components.CircularIconButton
import com.example.roomiesync.ui.components.HouseholdInfoCard
import com.example.roomiesync.ui.components.RectangularButton
import com.example.roomiesync.ui.theme.PrimaryBackground
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.SecondaryGrey

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onEditProfileClick: () -> Unit = {},
    onHouseholdClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        viewModel.uploadAvatar(bytes)
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = PrimaryBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseClick
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Profile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Avatar
        Box(
            modifier = Modifier.size(100.dp)
        ) {
            val avatarUrl = uiState.profile?.avatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .listener(onError = { _, result ->
                            if (BuildConfig.DEBUG) {
                                Log.e("CoilError", "Failed to load image: ${result.throwable}")
                            }
                        })
                        .build()
                    ,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SecondaryGrey), // Placeholder
                    contentAlignment = Alignment.Center
                ) {
                    val displayName = uiState.profile?.displayName
                    val initials = if (displayName.isNullOrBlank()) {
                        "U"
                    } else if (displayName.contains("@")) {
                        displayName.take(1).uppercase()
                    } else {
                        val parts = displayName.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                        } else {
                            parts.first().take(1).uppercase()
                        }
                    }

                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            CircularIconButton(
                icon = Icons.Default.ChangeCircle,
                contentDescription = "Change avatar",
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp),
                backgroundColor = PrimaryGreen,
                iconTint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name
        Text(
            text = uiState.profile?.displayName ?: "User",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Edit Profile Button
        RectangularButton(
            text = "Edit profile",
            onClick = onEditProfileClick,
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Household Info
        val house = uiState.house

        if (house != null) {
            HouseholdInfoCard(
                houseName = house.name,
                address = house.address ?: "No address",
                joinCode = house.joinCode ?: "N/A",
                onClick = onHouseholdClick
            )
        } else {
            JoinHouseholdCard(
                onClick = onHouseholdClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out Button
        RectangularButton(
            text = "Sign Out",
            onClick = onSignOut,
            backgroundColor = Color.Red,
            textColor = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun JoinHouseholdCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryGrey)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Join a household",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
