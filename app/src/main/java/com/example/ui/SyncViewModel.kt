package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SyncDatabase
import com.example.data.model.PermissionAuditEvent
import com.example.data.model.SubsystemState
import com.example.data.model.SubsystemType
import com.example.data.model.SyncLog
import com.example.data.repository.SyncRepository
import com.example.util.CsvExportHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
  val subsystems: List<SubsystemState> = emptyList(),
  val isLoading: Boolean = true,
  val isSyncing: Boolean = false,
  val syncProgress: Float = 0f,
  val currentSyncStep: String = "",
  val selectedTab: Int = 0,
  val filter: String = "ALL", // "ALL", "GRANTED", "PENDING"
  val auditFilter: String = "ALL", // "ALL", "LOCATION", "CAMERA", "MICROPHONE", "GRANTED", "DENIED"
  val isAuditing: Boolean = false,
  val lastSyncLog: SyncLog? = null,
  val showSyncDialog: Boolean = false,
  val toastMessage: String? = null
) {
  val grantedCount: Int get() = subsystems.count { it.isGranted }
  val totalCount: Int get() = subsystems.size
  val syncScorePercentage: Int
    get() = if (totalCount == 0) 0 else ((grantedCount.toFloat() / totalCount) * 100).toInt()

  val lastSyncTimeFormatted: String
    get() {
      val ts = lastSyncLog?.timestamp ?: return "Not yet synced"
      val df = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
      return df.format(java.util.Date(ts))
    }
}

class SyncViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: SyncRepository
  private val _uiState = MutableStateFlow(SyncUiState())
  val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

  val syncLogs: StateFlow<List<SyncLog>>
  val auditEvents: StateFlow<List<PermissionAuditEvent>>

  init {
    val db = SyncDatabase.getDatabase(application)
    repository = SyncRepository(application, db.syncLogDao(), db.permissionAuditDao())

    syncLogs = repository.syncLogs.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    auditEvents = repository.auditEvents.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    viewModelScope.launch {
      repository.ensureBaselineAuditEvents()
    }

    viewModelScope.launch {
      syncLogs.collect { logs ->
        if (logs.isNotEmpty() && _uiState.value.lastSyncLog == null) {
          _uiState.update { it.copy(lastSyncLog = logs.first()) }
        }
      }
    }

    refreshSubsystems()
  }

  fun refreshSubsystems() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val states = SubsystemType.values().map { repository.inspectSubsystem(it) }
      _uiState.update { it.copy(subsystems = states, isLoading = false) }
    }
  }

  fun startFullSync() {
    if (_uiState.value.isSyncing) return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isSyncing = true,
          syncProgress = 0.05f,
          currentSyncStep = "Initializing sync engine...",
          showSyncDialog = true
        )
      }

      val steps = listOf(
        "Checking Network & Cloud connection..." to 0.15f,
        "Acquiring GPS & Location telemetry..." to 0.30f,
        "Probing Bluetooth peer mesh..." to 0.45f,
        "Verifying Camera & Optical sync..." to 0.55f,
        "Testing Audio & Microphone diagnostics..." to 0.65f,
        "Reading Cellular state & Call logs..." to 0.75f,
        "Syncing Contacts address book..." to 0.85f,
        "Indexing SMS sync queue & Storage capacity..." to 0.95f,
        "Finalizing payload & posting notification..." to 1.0f
      )

      for ((stepText, progress) in steps) {
        _uiState.update { it.copy(currentSyncStep = stepText, syncProgress = progress) }
        delay(220)
      }

      val log = repository.executeFullSync()

      // Refresh states
      val refreshed = SubsystemType.values().map { repository.inspectSubsystem(it) }

      _uiState.update {
        it.copy(
          isSyncing = false,
          syncProgress = 1f,
          currentSyncStep = "Synchronization complete!",
          subsystems = refreshed,
          lastSyncLog = log,
          toastMessage = "Data sync finished: ${log.status}"
        )
      }
    }
  }

  fun sendTestAlert() {
    val posted = NotificationHelper.sendSyncNotification(
      getApplication(),
      "Sync Hub Alert",
      "Manual test alert: System is actively monitoring all 10 sync subsystems."
    )
    _uiState.update {
      it.copy(
        toastMessage = if (posted) "Test notification sent!" else "Grant Notification permission first"
      )
    }
  }

  fun clearHistory() {
    viewModelScope.launch {
      repository.clearLogs()
      _uiState.update { it.copy(toastMessage = "Sync history cleared") }
    }
  }

  fun clearAuditLogs() {
    viewModelScope.launch {
      repository.clearAuditEvents()
      _uiState.update { it.copy(toastMessage = "Permission audit logs cleared") }
    }
  }

  fun triggerPermissionAudit() {
    if (_uiState.value.isAuditing) return
    viewModelScope.launch {
      _uiState.update { it.copy(isAuditing = true) }
      val types = SubsystemType.values()
      for (type in types) {
        val state = repository.inspectSubsystem(type)
        repository.logAuditEvent(
          type = type,
          eventType = "MANUAL_AUDIT_PROBE",
          isGranted = state.isGranted,
          details = "Audit probe completed. Diagnostic: ${state.diagnosticSummary}"
        )
      }
      val refreshed = SubsystemType.values().map { repository.inspectSubsystem(it) }
      _uiState.update {
        it.copy(
          subsystems = refreshed,
          isAuditing = false,
          toastMessage = "Permission audit completed across all ${types.size} subsystems"
        )
      }
    }
  }

  fun setAuditFilter(filter: String) {
    _uiState.update { it.copy(auditFilter = filter) }
  }

  fun exportAuditLogsToCsv(context: Context, eventsToExport: List<PermissionAuditEvent>) {
    if (eventsToExport.isEmpty()) {
      _uiState.update { it.copy(toastMessage = "No audit log records to export.") }
      return
    }
    val success = CsvExportHelper.exportAndShareCsv(context, eventsToExport)
    if (success) {
      _uiState.update { it.copy(toastMessage = "Exported ${eventsToExport.size} audit events to CSV.") }
    } else {
      _uiState.update { it.copy(toastMessage = "Failed to export CSV file.") }
    }
  }

  fun selectTab(index: Int) {
    _uiState.update { it.copy(selectedTab = index) }
  }

  fun setFilter(filter: String) {
    _uiState.update { it.copy(filter = filter) }
  }

  fun dismissSyncDialog() {
    _uiState.update { it.copy(showSyncDialog = false) }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }
}
