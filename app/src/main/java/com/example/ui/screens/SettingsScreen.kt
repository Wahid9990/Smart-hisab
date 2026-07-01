package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Loc
import com.example.model.UserProfile
import com.example.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    
    val userProfile = userProfileState ?: UserProfile(
        shopName = Loc.t(lang, "app_name"),
        ownerName = "Valued Merchant",
        email = "merchant@smarthisab.com"
    )

    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)
    val scrollState = rememberScrollState()

    val joinDate = remember(userProfile.createdAt) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(userProfile.createdAt))
        } catch (e: Exception) {
            "Just now"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D14))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = Loc.t(lang, "settings"),
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = align
            )
            
            Text(
                text = Loc.t(lang, "tagline"),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = align
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Language Toggle Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRtl) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language settings",
                                tint = Color(0xFFBB86FC),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = Loc.t(lang, "select_language").uppercase(),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFBB86FC)
                            )
                        )
                        if (isRtl) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language settings",
                                tint = Color(0xFFBB86FC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Readable English Option Button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage(Loc.EN) }
                            .testTag("lang_en_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lang == Loc.EN) Color(0x1A03DAC5) else Color(0x0AFFFFFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (lang == Loc.EN) Color(0xFF03DAC5) else Color(0x10FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "English",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            if (lang == Loc.EN) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF03DAC5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Readable Urdu Option Button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage(Loc.UR) }
                            .testTag("lang_ur_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lang == Loc.UR) Color(0x1A03DAC5) else Color(0x0AFFFFFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (lang == Loc.UR) Color(0xFF03DAC5) else Color(0x10FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اردو (Urdu)",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            if (lang == Loc.UR) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF03DAC5)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Profile Details Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = Loc.t(lang, "business_details").uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFBB86FC),
                            textAlign = align
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileRow(
                        icon = Icons.Default.Store,
                        label = Loc.t(lang, "shop_name"),
                        value = userProfile.shopName,
                        isRtl = isRtl
                    )
                    Divider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileRow(
                        icon = Icons.Default.Face,
                        label = Loc.t(lang, "owner_name"),
                        value = userProfile.ownerName,
                        isRtl = isRtl
                    )
                    Divider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileRow(
                        icon = Icons.Default.Email,
                        label = Loc.t(lang, "email"),
                        value = userProfile.email,
                        isRtl = isRtl
                    )
                    Divider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileRow(
                        icon = Icons.Default.Phone,
                        label = Loc.t(lang, "phone"),
                        value = userProfile.phone ?: "Not Provided",
                        isRtl = isRtl
                    )
                    Divider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(vertical = 12.dp))
                    ProfileRow(
                        icon = Icons.Default.DateRange,
                        label = Loc.t(lang, "onboard_date"),
                        value = joinDate,
                        isRtl = isRtl
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = { viewModel.logOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_logout_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x26EF5350),
                    contentColor = Color(0xFFEF5350)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.t(lang, "logout").uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isRtl: Boolean
) {
    val align = if (isRtl) TextAlign.Right else TextAlign.Left
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start
    ) {
        if (!isRtl) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF03DAC5),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
        ) {
            Text(
                text = label,
                style = TextStyle(fontSize = 11.sp, color = Color(0xFF78909C)),
                textAlign = align
            )
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = align
            )
        }
        if (isRtl) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF03DAC5),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
