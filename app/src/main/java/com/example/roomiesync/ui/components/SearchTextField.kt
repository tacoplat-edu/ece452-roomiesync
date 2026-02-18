package com.example.roomiesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.SecondaryGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onFocusChanged: (Boolean) -> Unit = {},
    onSearch: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(42.dp)
            .onFocusChanged { onFocusChanged(it.isFocused) },
        textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = SecondaryGrey,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SecondaryGrey
                    )
                },
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                container = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 1.dp,
                                color = PrimaryGreen,
                                shape = RoundedCornerShape(50)
                            )
                            .background(Color.White, RoundedCornerShape(50))
                    )
                }
            )
        }
    )
}
