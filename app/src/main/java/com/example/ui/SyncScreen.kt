package com.example.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubsystemType
import com.example.ui.components.PermissionAuditCard
import com.example.ui.components.SubsystemCard
import com.example.ui.components.SyncHeroCard
import com.example.ui.components.SyncLogCard
import com.example.ui.components.SyncMetricsChartCard
import com.example.ui.components.SyncProgressDialog
import com.example.ui.theme.StatusDenied
import com.example.ui.theme.StatusGranted
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SyncBlue
import com.example.ui.theme.SyncCyan
import com.example.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
  viewModel: SyncViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val logs by viewModel.syncLogs.collectAsStateWithLifecycle()
  val auditEvents by viewModel.auditEvents.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  // Batch permission launcher
  val batchPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ ->
    viewModel.refreshSubsystems()
  }

  // Auto-refresh when app resumes from background / system settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSubsystems()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Toast / Snackbar handling
  LaunchedEffect(state.toastMessage) {
    state.toastMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearToast()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SyncBlue),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.CloudSync,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Sync Hub",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Device Permissions & Data Sync",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.refreshSubsystems() },
            modifier = Modifier.testTag("refresh_button")
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = "Refresh Telemetry Status",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets.statusBars
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Hero Status Card
        item {
          SyncHeroCard(
            grantedCount = state.grantedCount,
            totalCount = state.totalCount,
            scorePercentage = state.syncScorePercentage,
            isSyncing = state.isSyncing,
            onGrantAllClick = {
              val allPerms = PermissionHelper.getAllRuntimePermissions()
              batchPermissionLauncher.launch(allPerms)
            },
            onSyncNowClick = { viewModel.startFullSync() },
            onOpenSettingsClick = { PermissionHelper.openAppSettings(context) },
            onTestAlertClick = { viewModel.sendTestAlert() }
          )
        }

        // Subsystem Sync Health Metrics Chart Dashboard
        item {
          SyncMetricsChartCard(
            subsystems = state.subsystems,
            lastSyncTimeFormatted = state.lastSyncTimeFormatted,
            overallPercentage = state.syncScorePercentage
          )
        }

        // Tab Row (Subsystems vs Audit Log vs Sync Telemetry)
        item {
          TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SyncBlue,
            indicator = { tabPositions ->
              TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                color = SyncBlue
              )
            },
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .testTag("sync_tabs")
          ) {
            Tab(
              selected = state.selectedTab == 0,
              onClick = { viewModel.selectTab(0) },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Subsystems (${state.totalCount})",
                    fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                  )
                }
              }
            )
            Tab(
              selected = state.selectedTab == 1,
              onClick = { viewModel.selectTab(1) },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.FactCheck,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Audit Log (${auditEvents.size})",
                    fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                  )
                }
              }
            )
            Tab(
              selected = state.selectedTab == 2,
              onClick = { viewModel.selectTab(2) },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Sync Runs (${logs.size})",
                    fontWeight = if (state.selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                  )
                }
              }
            )
          }
        }

        // Tab Content
        if (state.selectedTab == 0) {
          // Filter Chips
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val pendingCount = state.totalCount - state.grantedCount

              FilterChip(
                selected = state.filter == "ALL",
                onClick = { viewModel.setFilter("ALL") },
                label = { Text("All (${state.totalCount})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SyncBlue.copy(alpha = 0.15f),
                  selectedLabelColor = SyncBlue
                )
              )
              FilterChip(
                selected = state.filter == "GRANTED",
                onClick = { viewModel.setFilter("GRANTED") },
                label = { Text("Active (${state.grantedCount})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = StatusGranted.copy(alpha = 0.15f),
                  selectedLabelColor = StatusGranted
                )
              )
              FilterChip(
                selected = state.filter == "PENDING",
                onClick = { viewModel.setFilter("PENDING") },
                label = { Text("Needs Action ($pendingCount)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = StatusWarning.copy(alpha = 0.15f),
                  selectedLabelColor = StatusWarning
                )
              )
            }
          }

          // Subsystem Cards List
          val filteredSubsystems = state.subsystems.filter { sub ->
            when (state.filter) {
              "GRANTED" -> sub.isGranted
              "PENDING" -> !sub.isGranted
              else -> true
            }
          }

          items(filteredSubsystems, key = { it.type.name }) { subsystem ->
            SubsystemCard(
              state = subsystem,
              onRequestPermission = {
                val perms = subsystem.type.getRequiredPermissions()
                if (perms.isNotEmpty()) {
                  batchPermissionLauncher.launch(perms.toTypedArray())
                }
              },
              onOpenSettings = { PermissionHelper.openAppSettings(context) }
            )
          }

          if (filteredSubsystems.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (state.filter == "PENDING") "All permissions are granted!" else "No items match filter",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else if (state.selectedTab == 1) {
          // Tab 1: Dedicated Permission Access Audit Log
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Permission Access Audit Trail",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Chronological audit log of permission access checks and probes.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                IconButton(
                  onClick = { viewModel.clearAuditLogs() },
                  modifier = Modifier.testTag("clear_audit_button")
                ) {
                  Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear Audit Logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Export CSV Button
                Button(
                  onClick = {
                    val filteredEvents = auditEvents.filter { ev ->
                      when (state.auditFilter) {
                        "LOCATION" -> ev.subsystemType == "LOCATION"
                        "CAMERA" -> ev.subsystemType == "CAMERA"
                        "MICROPHONE" -> ev.subsystemType == "MICROPHONE"
                        "STORAGE" -> ev.subsystemType == "STORAGE"
                        "GRANTED" -> ev.status.equals("GRANTED", ignoreCase = true) || ev.status.equals("AVAILABLE", ignoreCase = true)
                        "DENIED" -> ev.status.equals("DENIED", ignoreCase = true)
                        else -> true
                      }
                    }
                    viewModel.exportAuditLogsToCsv(context, filteredEvents)
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("export_csv_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = SyncBlue),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = "Export CSV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Audit Now Button
                FilledTonalButton(
                  onClick = { viewModel.triggerPermissionAudit() },
                  enabled = !state.isAuditing,
                  modifier = Modifier.testTag("run_audit_button"),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  if (state.isAuditing) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(14.dp),
                      strokeWidth = 2.dp
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Filled.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = "Audit Now", fontSize = 12.sp)
                }
              }
            }
          }

          // Filter Chips for Audit Log
          item {
            val scrollState = rememberScrollState()
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val filterOptions = listOf(
                "ALL" to "All (${auditEvents.size})",
                "LOCATION" to "Location",
                "CAMERA" to "Camera",
                "MICROPHONE" to "Mic",
                "STORAGE" to "Storage",
                "GRANTED" to "Granted",
                "DENIED" to "Denied"
              )

              filterOptions.forEach { (filterKey, label) ->
                FilterChip(
                  selected = state.auditFilter == filterKey,
                  onClick = { viewModel.setAuditFilter(filterKey) },
                  label = { Text(label, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SyncBlue.copy(alpha = 0.15f),
                    selectedLabelColor = SyncBlue
                  )
                )
              }
            }
          }

          val filteredAuditEvents = auditEvents.filter { ev ->
            when (state.auditFilter) {
              "LOCATION" -> ev.subsystemType == "LOCATION"
              "CAMERA" -> ev.subsystemType == "CAMERA"
              "MICROPHONE" -> ev.subsystemType == "MICROPHONE"
              "STORAGE" -> ev.subsystemType == "STORAGE"
              "GRANTED" -> ev.status.equals("GRANTED", ignoreCase = true) || ev.status.equals("AVAILABLE", ignoreCase = true)
              "DENIED" -> ev.status.equals("DENIED", ignoreCase = true)
              else -> true
            }
          }

          if (filteredAuditEvents.isNotEmpty()) {
            items(filteredAuditEvents, key = { it.id }) { auditEvent ->
              PermissionAuditCard(event = auditEvent)
            }
          } else {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Filled.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "No Audit Records Matching Filter",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Tap 'Audit Now' to run fresh permission diagnostics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        } else {
          // Activity Logs Tab
          if (logs.isNotEmpty()) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Past Sync Telemetry Runs",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                  onClick = { viewModel.clearHistory() },
                  modifier = Modifier.testTag("clear_history_button")
                ) {
                  Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }

            items(logs, key = { it.id }) { log ->
              SyncLogCard(log = log)
            }
          } else {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = "No Sync Activity Yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Tap 'Sync Now' on the dashboard to run your first data synchronization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }

      // Sync Progress Dialog
      if (state.showSyncDialog) {
        SyncProgressDialog(
          isSyncing = state.isSyncing,
          progress = state.syncProgress,
          currentStep = state.currentSyncStep,
          lastLog = state.lastSyncLog,
          onDismiss = { viewModel.dismissSyncDialog() }
        )
      }
    }
  }
}
