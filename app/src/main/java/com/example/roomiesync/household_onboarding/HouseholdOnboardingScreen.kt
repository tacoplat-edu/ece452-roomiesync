package com.example.roomiesync.household_onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.R
import com.example.roomiesync.ui.components.FullWidthButtonWithIcon
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.SecondaryGrey
import com.example.roomiesync.ui.theme.Typography
import com.example.roomiesync.utils.household_onboarding.validation.HouseholdDetailsFormValidation
import com.example.roomiesync.utils.household_onboarding.validation.JoinCodeFormValidation

@Composable
fun HouseholdOnboardingScreen(
    modifier: Modifier = Modifier,
    householdOnboardingViewModel: HouseholdOnboardingViewModel = viewModel()
) {
    val uiState by householdOnboardingViewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentStep != HouseholdOnboardingStep.HOME) {
        householdOnboardingViewModel.onGoHome()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            when (uiState.currentStep) {
                HouseholdOnboardingStep.HOME -> HouseholdOnboardingHomeContent(
                    onNavigateToCreate = householdOnboardingViewModel::onGoToCreate,
                    onNavigateToJoin = householdOnboardingViewModel::onGoToJoin
                )
                HouseholdOnboardingStep.CREATE -> CreateHouseholdContent(
                    nickname = uiState.householdNickname,
                    address = uiState.householdAddress,
                    onNicknameChange = householdOnboardingViewModel::updateHouseholdNickname,
                    onAddressChange = householdOnboardingViewModel::updateHouseholdAddress,
                    onBack = householdOnboardingViewModel::onGoHome
                )
                HouseholdOnboardingStep.JOIN -> JoinHouseholdContent(
                    code = uiState.joinCode,
                    onCodeChange = householdOnboardingViewModel::updateJoinCode,
                    onBack = householdOnboardingViewModel::onGoHome
                )
            }
        }
    }
}

@Composable
fun HouseholdOnboardingHomeContent(
    onNavigateToCreate: () -> Unit,
    onNavigateToJoin: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val curveHeight = size.height * (8.0f / 16)
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, curveHeight)
                cubicTo(
                    size.width * 2 / 3,
                    curveHeight + size.height * 0.15f,
                    size.width / 3,
                    curveHeight - size.height * 0.15f,
                    0f,
                    curveHeight
                )
                close()
            }
            drawPath(path, color = PrimaryGreen)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * 0.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontSize = 28.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.ic_roomiesync_wordmark_logo_full_white),
                contentDescription = "RoomieSync Logo",
                modifier = Modifier.height(64.dp),
                contentScale = ContentScale.Fit
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.BottomCenter)
                .offset(y = -screenHeight * 0.1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "I don't have a household yet.",
                style = Typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Black
            )
            FullWidthButtonWithIcon(
                text = "Create",
                icon = Icons.Filled.Add,
                backgroundColor = PrimaryGreen,
                onClick = onNavigateToCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "I want to join an existing household.",
                style = Typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Black
            )
            FullWidthButtonWithIcon(
                text = "Join",
                icon = Icons.Filled.Group,
                backgroundColor = SecondaryGrey,
                onClick = onNavigateToJoin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHouseholdContent(
    nickname: String,
    address: String,
    onNicknameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Create a new household",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Household Name",
                    style = Typography.labelMedium,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = PrimaryGreen
                    )
                )
            }

            /*TODO: If needed, convert to several blocks (address line 1, address line 2, city,
               province/state, country, zip) or Autocomplete field */
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Address",
                    style = Typography.labelMedium,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = PrimaryGreen
                    )
                )
            }

            Spacer(modifier = Modifier.weight(2f))

            val isFormValid = HouseholdDetailsFormValidation.isHouseholdDetailsFormValid(nickname, address)

            FullWidthButtonWithIcon(
                text = "Next",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                enabled = isFormValid,
                onClick = { /* Handle Create */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinHouseholdContent(
    code: String,
    onCodeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Join a household",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter your household's\n 8-digit invite code",
                    style = Typography.labelMedium,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.7f).padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = PrimaryGreen
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))

            val isFormValid = JoinCodeFormValidation.isJoinCodeValid(code)

            FullWidthButtonWithIcon(
                text = "Join",
                icon = Icons.Filled.Check,
                enabled = isFormValid,
                onClick = { /* Handle Join */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }
    }
}
