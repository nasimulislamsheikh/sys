package com.example.data.model

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class SubsystemType(
  val title: String,
  val categoryName: String,
  val description: String,
  val icon: ImageVector,
  val isInstallTimeOnly: Boolean = false
) {
  LOCATION(
    title = "GPS & Location",
    categoryName = "Locations",
    description = "Acquires live GPS coordinates, altitude, and precision for geo-tagging sync packets.",
    icon = Icons.Filled.LocationOn
  ),
  BLUETOOTH(
    title = "Bluetooth Sync",
    categoryName = "Bluetooth",
    description = "Scans and connects to peer sync devices and local telemetry beacons.",
    icon = Icons.Filled.Bluetooth
  ),
  CAMERA(
    title = "Camera Hardware",
    categoryName = "Camera",
    description = "Provides optical sync pairing, QR synchronization, and visual telemetry capture.",
    icon = Icons.Filled.CameraAlt
  ),
  MICROPHONE(
    title = "Audio & Microphone",
    categoryName = "Mic",
    description = "Captures audio diagnostics, voice notes, and acoustic telemetry for data sync.",
    icon = Icons.Filled.Mic
  ),
  CALLS(
    title = "Calls & Phone State",
    categoryName = "Call",
    description = "Monitors cellular carrier state, SIM readiness, and syncs call history logs.",
    icon = Icons.Filled.Call
  ),
  CONTACTS(
    title = "Contacts Directory",
    categoryName = "Contact",
    description = "Accesses and synchronizes device contacts database with remote repository.",
    icon = Icons.Filled.Contacts
  ),
  NETWORK(
    title = "Network & Cloud",
    categoryName = "Network",
    description = "Monitors Wi-Fi and Cellular connectivity for payload transmission.",
    icon = Icons.Filled.Wifi,
    isInstallTimeOnly = true
  ),
  SMS(
    title = "SMS Messaging",
    categoryName = "SMS",
    description = "Synchronizes SMS messages, verifies sync tokens, and enables outbound SMS alerts.",
    icon = Icons.Filled.Sms
  ),
  STORAGE(
    title = "State & Storage",
    categoryName = "State Storage",
    description = "Reads storage capacities, media cache availability, and file sync spaces.",
    icon = Icons.Filled.SdStorage
  ),
  NOTIFICATIONS(
    title = "Notifications Alert",
    categoryName = "Notification",
    description = "Dispatches real-time background sync alerts, progress updates, and completion flags.",
    icon = Icons.Filled.Notifications
  );

  fun getRequiredPermissions(): List<String> {
    return when (this) {
      LOCATION -> listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
      BLUETOOTH -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
          )
        } else {
          listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
          )
        }
      }
      CAMERA -> listOf(Manifest.permission.CAMERA)
      MICROPHONE -> listOf(Manifest.permission.RECORD_AUDIO)
      CALLS -> listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
      )
      CONTACTS -> listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
      )
      NETWORK -> emptyList() // Normal install-time permissions: INTERNET, ACCESS_NETWORK_STATE
      SMS -> listOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS
      )
      STORAGE -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
          )
        } else {
          listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      }
      NOTIFICATIONS -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          emptyList()
        }
      }
    }
  }
}

data class SubsystemState(
  val type: SubsystemType,
  val isGranted: Boolean,
  val isPartial: Boolean = false,
  val diagnosticSummary: String = "",
  val telemetryDetails: List<Pair<String, String>> = emptyList(),
  val missingPermissions: List<String> = emptyList()
)
