package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Loc
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C0D14), Color(0xFF161722))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Language Selector Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language Selector",
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                // English Toggle Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (lang == Loc.EN) Color(0x3303DAC5) else Color.Transparent)
                        .clickable { viewModel.setLanguage(Loc.EN) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "EN",
                        color = if (lang == Loc.EN) Color(0xFF03DAC5) else Color(0xFF90A4AE),
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Urdu Toggle Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (lang == Loc.UR) Color(0x3303DAC5) else Color.Transparent)
                        .clickable { viewModel.setLanguage(Loc.UR) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "اردو",
                        color = if (lang == Loc.UR) Color(0xFF03DAC5) else Color(0xFF90A4AE),
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Demo Mode banner if real Firebase is missing
            if (!viewModel.isFirebaseAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("demo_mode_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F03DAC5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Demo Mode Info",
                            tint = Color(0xFF03DAC5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Loc.t(lang, "demo_banner"),
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = Color(0xFFE0E0E0),
                                lineHeight = 16.sp
                            ),
                            textAlign = align,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Branding / Logo area
            Surface(
                modifier = Modifier.size(70.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF03DAC5).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF03DAC5))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "₹",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03DAC5),
                            fontFamily = FontFamily.Serif
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = Loc.t(lang, "app_name"),
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
            )

            Text(
                text = Loc.t(lang, "tagline"),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color(0xFF90A4AE),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card enclosing the login inputs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = Loc.t(lang, "login").uppercase(),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFBB86FC),
                            textAlign = align
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.clearError()
                        },
                        label = { 
                            Text(
                                text = Loc.t(lang, "email"),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = align
                            ) 
                        },
                        leadingIcon = if (!isRtl) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Icon",
                                    tint = Color(0xFFB0BEC5)
                                )
                            }
                        } else null,
                        trailingIcon = if (isRtl) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Icon",
                                    tint = Color(0xFFB0BEC5)
                                )
                            }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedLabelColor = Color(0xFF03DAC5),
                            unfocusedLabelColor = Color(0xFF78909C),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x3FFFFFFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = align)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            viewModel.clearError()
                        },
                        label = { 
                            Text(
                                text = Loc.t(lang, "password"),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = align
                            ) 
                        },
                        leadingIcon = if (!isRtl) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password Icon",
                                    tint = Color(0xFFB0BEC5)
                                )
                            }
                        } else null,
                        trailingIcon = {
                            Row(
                                modifier = Modifier.wrapContentSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                        tint = Color(0xFFB0BEC5)
                                    )
                                }
                                if (isRtl) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Password Icon",
                                        tint = Color(0xFFB0BEC5),
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedLabelColor = Color(0xFF03DAC5),
                            unfocusedLabelColor = Color(0xFF78909C),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x3FFFFFFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = align)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic status block (for errors or loading)
                    AnimatedVisibility(
                        visible = uiState is AuthUiState.Error || uiState is AuthUiState.Loading,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            when (val state = uiState) {
                                is AuthUiState.Error -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0x26EF5350)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .testTag("login_error_card")
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = TextStyle(
                                                color = Color(0xFFEF5350),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = align
                                        )
                                    }
                                }
                                is AuthUiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF03DAC5),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // Login Button (Big readable shopkeeper layout)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.logIn(email, password)
                        },
                        enabled = uiState !is AuthUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF03DAC5),
                            contentColor = Color(0xFF0C0D14),
                            disabledContainerColor = Color(0xFF03DAC5).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = Loc.t(lang, "login_account").uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Helpful Hint for Demo Mode credentials
                    if (!viewModel.isFirebaseAvailable) {
                        Text(
                            text = Loc.t(lang, "quick_test"),
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = Color(0xFF03DAC5),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation to Signup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.t(lang, "no_account"),
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onNavigateToSignup,
                    modifier = Modifier.testTag("go_to_signup_button")
                ) {
                    Text(
                        text = Loc.t(lang, "signup"),
                        color = Color(0xFFBB86FC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
