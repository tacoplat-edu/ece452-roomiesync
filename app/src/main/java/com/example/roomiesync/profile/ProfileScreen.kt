package com.example.roomiesync.profile

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.roomiesync.data.House
import com.example.roomiesync.ui.components.CircularIconButton
import com.example.roomiesync.ui.components.HouseholdInfoCard
import com.example.roomiesync.ui.components.RectangularButton
import com.example.roomiesync.ui.theme.PrimaryBackground
import com.example.roomiesync.ui.theme.SecondaryGrey
import com.example.roomiesync.ui.theme.PrimaryGreen

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onEditProfileClick: () -> Unit = {},
    onHouseholdClick: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // In a real app we'd upload the image somewhere and get an actual URL.
                // For now, we store the local URI as string since Supabase storage might not be set up fully here.
                viewModel.updateAvatarUrl(uri.toString())
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
                    model = avatarUrl,
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
                        .background(Color.Gray) // Placeholder
                )
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
//        val house = uiState.house

        val house = House(
            id = "house-1",
            name = "My House",
            address = "123 Main St",
            joinCode = "ABC123",
            createdBy = "user-1"
        )

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
