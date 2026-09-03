package com.k.hosken.navipulse.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k.hosken.navipulse.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lets the user pick between exporting a JSON backup and restoring one, in place of the two
 * separate Settings rows this used to be. */
@Composable
fun BackupScreen(
    viewModel: SettingsViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { result ->
                result.onSuccess { count ->
                    Toast.makeText(context, "Backed up $count trip(s)", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri) { result ->
                result.onSuccess { count ->
                    Toast.makeText(context, "Restored $count trip(s)", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Restore failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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
                        .clickable(onClick = onBackClicked),
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
                    text = "Backup",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            SettingsSectionContainer {
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    label = "Export Backup",
                    onClick = {
                        val fileName = "NaviPulse_Backup_${
                            SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
                        }.json"
                        exportLauncher.launch(fileName)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSectionContainer {
                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    label = "Restore Backup",
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
