package com.example.expense.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expense.settings.AppLockSettings
import com.example.expense.settings.LockTimeout

/**
 * Settings screen for app lock configuration.
 * Currently supports enabling/disabling app lock and timeout selection.
 * Ready for future enhancement with additional settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var appLockSettings by remember {
        mutableStateOf(AppLockSettings())
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Lock Section Header
            Text(
                text = "App Lock",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Enable App Lock Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable App Lock",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Require authentication to access the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appLockSettings.isEnabled,
                    onCheckedChange = { isEnabled ->
                        appLockSettings = appLockSettings.copy(isEnabled = isEnabled)
                    }
                )
            }

            if (appLockSettings.isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // Lock Timeout Selection
                Text(
                    text = "Lock Timeout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LockTimeout.entries.forEach { timeout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = appLockSettings.lockTimeout == timeout,
                                onClick = {
                                    appLockSettings = appLockSettings.copy(lockTimeout = timeout)
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appLockSettings.lockTimeout == timeout,
                            onClick = {
                                appLockSettings = appLockSettings.copy(lockTimeout = timeout)
                            }
                        )
                        Text(
                            text = timeout.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Note about 30 seconds implementation
                if (appLockSettings.lockTimeout != LockTimeout.THIRTY_SECONDS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Note: Only '30 Seconds' timeout is currently active. Other options will be available in future updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
