package com.k.hosken.navipulse.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k.hosken.navipulse.R
import com.k.hosken.navipulse.data.AppFont
import com.k.hosken.navipulse.data.AppSummaryTextSize
import com.k.hosken.navipulse.data.AppTextColor
import com.k.hosken.navipulse.data.AppTextSize
import com.k.hosken.navipulse.ui.theme.toColor
import com.k.hosken.navipulse.ui.theme.toFontFamily

@Composable
fun FontsScreen(
    viewModel: SettingsViewModel,
    onBackClicked: () -> Unit
) {
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()

    // Staged locally - only written to the ViewModel (and persisted) when the user
    // navigates back, so picking through options doesn't commit until they're done.
    var draftFont by remember { mutableStateOf(viewModel.appFont.value) }
    var draftColor by remember { mutableStateOf(viewModel.textColor.value) }
    var draftValueSize by remember { mutableStateOf(viewModel.textSize.value) }
    var draftTitleSize by remember { mutableStateOf(viewModel.titleTextSize.value) }
    var draftSummarySize by remember { mutableStateOf(viewModel.summaryTextSize.value) }

    val saveAndGoBack: () -> Unit = {
        viewModel.setAppFont(draftFont)
        viewModel.setTextColor(draftColor)
        viewModel.setTextSize(draftValueSize)
        viewModel.setTitleTextSize(draftTitleSize)
        viewModel.setSummaryTextSize(draftSummarySize)
        onBackClicked()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val customBackground = remember(backgroundImagePath) {
            backgroundImagePath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
        if (customBackground != null) {
            Image(
                bitmap = customBackground,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.dashboard_background_default),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = saveAndGoBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Fonts",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Sample title/value preview - updates live as the drafts below change.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sample Title",
                    color = if (draftColor == AppTextColor.DEFAULT) Color.White else draftColor.toColor(),
                    fontFamily = draftFont.toFontFamily(),
                    fontSize = draftTitleSize.sp.sp
                )
                Text(
                    text = "Sample Value",
                    color = if (draftColor == AppTextColor.DEFAULT) Color.White else draftColor.toColor(),
                    fontFamily = draftFont.toFontFamily(),
                    fontSize = draftValueSize.sp.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sample Summary",
                    color = if (draftColor == AppTextColor.DEFAULT) Color.White else draftColor.toColor(),
                    fontFamily = draftFont.toFontFamily(),
                    fontSize = draftSummarySize.sp.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionContainer {
                DropdownSection(
                    label = "Font",
                    selectedLabel = draftFont.label,
                    options = AppFont.entries,
                    optionLabel = { it.label },
                    isSelected = { it == draftFont },
                    onSelect = { draftFont = it },
                    optionFontFamily = { it.toFontFamily() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionContainer {
                DropdownSection(
                    label = "Text Colour",
                    selectedLabel = draftColor.label,
                    options = AppTextColor.entries,
                    optionLabel = { it.label },
                    isSelected = { it == draftColor },
                    onSelect = { draftColor = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionContainer {
                DropdownSection(
                    label = "Summary Text Size",
                    selectedLabel = draftSummarySize.label,
                    options = AppSummaryTextSize.entries,
                    optionLabel = { it.label },
                    isSelected = { it == draftSummarySize },
                    onSelect = { draftSummarySize = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionContainer {
                DropdownSection(
                    label = "Recent Logged Trips",
                    selectedLabel = draftTitleSize.label,
                    options = AppTextSize.entries,
                    optionLabel = { it.label },
                    isSelected = { it == draftTitleSize },
                    onSelect = { draftTitleSize = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionContainer {
                DropdownSection(
                    label = "Fuel Up",
                    selectedLabel = draftValueSize.label,
                    options = AppTextSize.entries,
                    optionLabel = { it.label },
                    isSelected = { it == draftValueSize },
                    onSelect = { draftValueSize = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        content()
    }
}

@Composable
private fun <T> DropdownSection(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    optionFontFamily: ((T) -> FontFamily)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = selectedLabel,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = Color.White
        )
    }

    if (expanded) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(options) { option ->
                val selected = isSelected(option)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable {
                            onSelect(option)
                            expanded = false
                        }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = optionLabel(option),
                        color = Color.White,
                        fontFamily = optionFontFamily?.invoke(option) ?: FontFamily.Default,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Text(text = "✓", color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
