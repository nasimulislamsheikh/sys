package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_audit_events")
data class PermissionAuditEvent(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val subsystemType: String, // "LOCATION", "CAMERA", "MICROPHONE", etc.
  val subsystemTitle: String,
  val permissionName: String,
  val eventType: String, // "RUNTIME_CHECK", "TELEMETRY_PROBE", "SYNC_ACCESS"
  val status: String, // "GRANTED", "DENIED", "AVAILABLE"
  val details: String,
  val appSource: String = "Sync Hub Diagnostics"
)
