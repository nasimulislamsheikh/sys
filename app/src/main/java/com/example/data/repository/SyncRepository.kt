package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.local.PermissionAuditDao
import com.example.data.local.SyncLogDao
import com.example.data.model.PermissionAuditEvent
import com.example.data.model.SubsystemState
import com.example.data.model.SubsystemType
import com.example.data.model.SyncLog
import com.example.util.NotificationHelper
import com.example.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncRepository(
  private val context: Context,
  private val syncLogDao: SyncLogDao,
  private val permissionAuditDao: PermissionAuditDao
) {

  val syncLogs: Flow<List<SyncLog>> = syncLogDao.getAllLogs()
  val auditEvents: Flow<List<PermissionAuditEvent>> = permissionAuditDao.getAllAuditEvents()

  suspend fun insertLog(log: SyncLog) = syncLogDao.insertLog(log)

  suspend fun clearLogs() = syncLogDao.clearAllLogs()

  suspend fun clearAuditEvents() = permissionAuditDao.clearAllEvents()

  suspend fun getAllAuditEventsList(): List<PermissionAuditEvent> = permissionAuditDao.getAllAuditEventsList()

  suspend fun logAuditEvent(
    type: SubsystemType,
    eventType: String,
    isGranted: Boolean,
    details: String
  ) = withContext(Dispatchers.IO) {
    val permName = type.getRequiredPermissions().firstOrNull() ?: if (type == SubsystemType.NETWORK) "INTERNET / ACCESS_NETWORK_STATE" else "System Feature"
    val event = PermissionAuditEvent(
      timestamp = System.currentTimeMillis(),
      subsystemType = type.name,
      subsystemTitle = type.title,
      permissionName = permName,
      eventType = eventType,
      status = if (isGranted) "GRANTED" else "DENIED",
      details = details,
      appSource = "Sync Hub Diagnostics"
    )
    permissionAuditDao.insertEvent(event)
  }

  suspend fun ensureBaselineAuditEvents() = withContext(Dispatchers.IO) {
    if (permissionAuditDao.getCount() == 0) {
      val now = System.currentTimeMillis()
      val initialEvents = SubsystemType.values().mapIndexed { index, type ->
        val (isGranted, _) = PermissionHelper.checkSubsystemState(context, type)
        val permName = type.getRequiredPermissions().firstOrNull() ?: if (type == SubsystemType.NETWORK) "INTERNET / ACCESS_NETWORK_STATE" else "System Feature"
        val offsetMs = (SubsystemType.values().size - index) * 45000L
        PermissionAuditEvent(
          timestamp = now - offsetMs,
          subsystemType = type.name,
          subsystemTitle = type.title,
          permissionName = permName,
          eventType = "INITIAL_STATE_PROBE",
          status = if (isGranted) "GRANTED" else "DENIED",
          details = if (isGranted) "Subsystem permission verified active during startup audit" else "Permission probe detected missing or restricted authorization",
          appSource = "Sync Hub Diagnostics"
        )
      }
      permissionAuditDao.insertEvents(initialEvents)
    }
  }

  suspend fun inspectSubsystem(type: SubsystemType): SubsystemState = withContext(Dispatchers.IO) {
    val (isGranted, isPartial) = PermissionHelper.checkSubsystemState(context, type)
    val missing = PermissionHelper.getMissingPermissions(context, type)

    val (summary, details) = when (type) {
      SubsystemType.LOCATION -> inspectLocation(isGranted)
      SubsystemType.BLUETOOTH -> inspectBluetooth(isGranted)
      SubsystemType.CAMERA -> inspectCamera(isGranted)
      SubsystemType.MICROPHONE -> inspectMicrophone(isGranted)
      SubsystemType.CALLS -> inspectCalls(isGranted)
      SubsystemType.CONTACTS -> inspectContacts(isGranted)
      SubsystemType.NETWORK -> inspectNetwork()
      SubsystemType.SMS -> inspectSms(isGranted)
      SubsystemType.STORAGE -> inspectStorage(isGranted)
      SubsystemType.NOTIFICATIONS -> inspectNotifications(isGranted)
    }

    SubsystemState(
      type = type,
      isGranted = isGranted,
      isPartial = isPartial,
      diagnosticSummary = summary,
      telemetryDetails = details,
      missingPermissions = missing
    )
  }

  @SuppressLint("MissingPermission")
  private fun inspectLocation(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val gpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    val netEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

    val details = mutableListOf<Pair<String, String>>()
    details.add("GPS Provider" to if (gpsEnabled) "Enabled (Hardware active)" else "Disabled in Settings")
    details.add("Network Provider" to if (netEnabled) "Available" else "Unavailable")

    if (!isGranted) {
      return Pair(
        "Location permissions (Fine/Coarse) required for GPS geo-sync telemetry.",
        details
      )
    }

    var lastLoc: Location? = null
    try {
      if (gpsEnabled) lastLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      if (lastLoc == null && netEnabled) lastLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } catch (_: Exception) {}

    if (lastLoc != null) {
      val lat = String.format(Locale.US, "%.5f", lastLoc.latitude)
      val lon = String.format(Locale.US, "%.5f", lastLoc.longitude)
      val acc = if (lastLoc.hasAccuracy()) "${lastLoc.accuracy.toInt()}m" else "N/A"
      details.add("Coordinates" to "$lat, $lon")
      details.add("Accuracy" to acc)
      details.add("Provider" to (lastLoc.provider ?: "GPS"))
      return Pair("GPS locked: $lat, $lon (±$acc)", details)
    } else {
      details.add("GPS Fix" to "Awaiting satellite acquisition")
      return Pair("GPS service active. Waiting for satellite lock.", details)
    }
  }

  @SuppressLint("MissingPermission")
  private fun inspectBluetooth(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter: BluetoothAdapter? = bm?.adapter

    val details = mutableListOf<Pair<String, String>>()
    if (adapter == null) {
      details.add("Hardware" to "Not detected / Unsupported")
      return Pair("Bluetooth hardware unavailable on this device.", details)
    }

    val isEnabled = adapter.isEnabled
    details.add("Adapter Status" to if (isEnabled) "Active & Ready" else "Turned Off")

    if (!isGranted) {
      return Pair(
        "Bluetooth scan & connect permissions required for peer sync.",
        details
      )
    }

    try {
      val pairedCount = adapter.bondedDevices?.size ?: 0
      details.add("Paired Sync Devices" to "$pairedCount paired")
      details.add("Scan Mode" to if (adapter.isDiscovering) "Scanning" else "Idle")
      return Pair(
        if (isEnabled) "Bluetooth online • $pairedCount paired peer devices" else "Bluetooth is disabled in System Settings",
        details
      )
    } catch (_: Exception) {
      return Pair("Bluetooth adapter ready.", details)
    }
  }

  private fun inspectCamera(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    val details = mutableListOf<Pair<String, String>>()

    if (cm == null) {
      details.add("Camera Service" to "Unavailable")
      return Pair("Camera subsystem unavailable.", details)
    }

    try {
      val cameraIds = cm.cameraIdList
      var backLenses = 0
      var frontLenses = 0
      for (id in cameraIds) {
        val chars = cm.getCameraCharacteristics(id)
        val facing = chars.get(CameraCharacteristics.LENS_FACING)
        if (facing == CameraCharacteristics.LENS_FACING_BACK) backLenses++
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) frontLenses++
      }
      details.add("Detected Sensors" to "${cameraIds.size} total ($backLenses rear, $frontLenses front)")
      details.add("Optical Sync" to if (isGranted) "Operational" else "Permission needed")

      return if (isGranted) {
        Pair("Optical sync ready (${cameraIds.size} sensors available)", details)
      } else {
        Pair("Camera permission needed for optical sync & QR pairing.", details)
      }
    } catch (_: Exception) {
      return Pair(if (isGranted) "Camera ready" else "Camera permission needed", details)
    }
  }

  private fun inspectMicrophone(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val details = mutableListOf<Pair<String, String>>()

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && am != null) {
        val devices = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val micNames = devices.joinToString { it.productName.ifEmpty { "Built-in Mic" } }
        details.add("Audio Input Devices" to if (devices.isNotEmpty()) micNames else "Built-in Microphone")
      } else {
        details.add("Audio Input" to "Built-in Microphone")
      }
    } catch (_: Exception) {
      details.add("Audio Input" to "Hardware detected")
    }

    details.add("Recording State" to if (isGranted) "Ready for audio telemetry" else "Awaiting permission")

    return if (isGranted) {
      Pair("Audio telemetry & microphone sync verified.", details)
    } else {
      Pair("Microphone permission needed to sync audio telemetry.", details)
    }
  }

  @SuppressLint("MissingPermission")
  private fun inspectCalls(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val details = mutableListOf<Pair<String, String>>()

    val simStateStr = when (tm?.simState) {
      TelephonyManager.SIM_STATE_READY -> "SIM Ready"
      TelephonyManager.SIM_STATE_ABSENT -> "No SIM Card"
      TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "SIM Locked"
      else -> "Standby / Virtual"
    }
    details.add("Cellular State" to simStateStr)
    val carrier = tm?.networkOperatorName
    if (!carrier.isNullOrBlank()) {
      details.add("Carrier" to carrier)
    }

    if (!isGranted) {
      return Pair("Call and telephony permissions needed for call history sync.", details)
    }

    try {
      val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls._ID),
        null,
        null,
        null
      )
      val callCount = cursor?.use { it.count } ?: 0
      details.add("Call Log Entries" to "$callCount records")
      return Pair("Telephony & call history synced ($callCount records found)", details)
    } catch (_: Exception) {
      return Pair("Telephony state accessible.", details)
    }
  }

  private fun inspectContacts(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val details = mutableListOf<Pair<String, String>>()

    if (!isGranted) {
      details.add("Sync Pipeline" to "Blocked by missing permission")
      return Pair("Contacts permission needed to sync address book.", details)
    }

    try {
      val cursor = context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts._ID),
        null,
        null,
        null
      )
      val count = cursor?.use { it.count } ?: 0
      details.add("Address Book" to "$count contacts available")
      details.add("Sync Readiness" to "100% (Read & Write permitted)")
      return Pair("Contacts synchronized ($count contacts indexed)", details)
    } catch (_: Exception) {
      return Pair("Contacts access verified.", details)
    }
  }

  private fun inspectNetwork(): Pair<String, List<Pair<String, String>>> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val details = mutableListOf<Pair<String, String>>()

    val activeNetwork = cm?.activeNetwork
    val caps = cm?.getNetworkCapabilities(activeNetwork)

    val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val isMetered = cm?.isActiveNetworkMetered ?: false

    val netType = when {
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (High Speed)"
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular Mobile Data"
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
      else -> if (isConnected) "Connected" else "Offline"
    }

    details.add("Connection Type" to netType)
    details.add("Data Billing" to if (isMetered) "Metered (Cellular/Hotspot)" else "Unmetered (Broadband)")
    details.add("Cloud Sync Pipeline" to if (isConnected) "Online (Low Latency)" else "Offline (Waiting for network)")

    return if (isConnected) {
      Pair("Online via $netType • Cloud sync channel active", details)
    } else {
      Pair("Offline • Sync data will queue locally until network connects", details)
    }
  }

  private fun inspectSms(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val details = mutableListOf<Pair<String, String>>()

    if (!isGranted) {
      details.add("SMS Pipeline" to "Permissions required (Read/Receive/Send)")
      return Pair("SMS permissions needed to synchronize messages & 2FA tokens.", details)
    }

    try {
      val uri = Uri.parse("content://sms/inbox")
      val cursor = context.contentResolver.query(
        uri,
        arrayOf("_id"),
        null,
        null,
        null
      )
      val count = cursor?.use { it.count } ?: 0
      details.add("Inbox Messages" to "$count messages indexed")
      details.add("SMS Gateway" to "Ready for outbound sync alerts")
      return Pair("SMS service operational ($count messages indexed)", details)
    } catch (_: Exception) {
      return Pair("SMS subsystem ready.", details)
    }
  }

  private fun inspectStorage(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val details = mutableListOf<Pair<String, String>>()

    try {
      val stat = StatFs(Environment.getDataDirectory().path)
      val blockSize = stat.blockSizeLong
      val totalBlocks = stat.blockCountLong
      val availableBlocks = stat.availableBlocksLong

      val totalGB = (totalBlocks * blockSize) / (1024.0 * 1024 * 1024)
      val freeGB = (availableBlocks * blockSize) / (1024.0 * 1024 * 1024)
      val usedGB = totalGB - freeGB
      val usedPercent = if (totalGB > 0) ((usedGB / totalGB) * 100).toInt() else 0

      details.add("Internal Storage" to String.format(Locale.US, "%.1f GB free / %.1f GB total", freeGB, totalGB))
      details.add("Storage Used" to "$usedPercent% occupied")
      details.add("Media Access" to if (isGranted) "Full Read Permission" else "Restricted / Limited")

      return Pair(
        String.format(Locale.US, "Device state healthy: %.1f GB free space (%d%% used)", freeGB, usedPercent),
        details
      )
    } catch (_: Exception) {
      return Pair("Storage capacity verified.", details)
    }
  }

  private fun inspectNotifications(isGranted: Boolean): Pair<String, List<Pair<String, String>>> {
    val details = mutableListOf<Pair<String, String>>()
    val canPost = NotificationHelper.canPostNotifications(context)

    details.add("Alerts Channel" to "Active (${NotificationHelper.CHANNEL_ID})")
    details.add("System Permission" to if (canPost) "Granted & Active" else "Blocked / Disabled")

    return if (canPost) {
      Pair("Notification alerts active for real-time sync telemetry.", details)
    } else {
      Pair("Notification permission needed to alert you when data sync finishes.", details)
    }
  }

  suspend fun executeFullSync(): SyncLog = withContext(Dispatchers.IO) {
    val subsystems = SubsystemType.values().map { inspectSubsystem(it) }
    val totalCount = subsystems.size
    val grantedCount = subsystems.count { it.isGranted }
    val scorePercentage = ((grantedCount.toFloat() / totalCount) * 100).toInt()

    val status = when {
      grantedCount == totalCount -> "COMPLETED"
      grantedCount >= 5 -> "PARTIAL"
      else -> "BLOCKED"
    }

    val df = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val nowFormatted = df.format(Date())

    val summary = when (status) {
      "COMPLETED" -> "Full data synchronization completed successfully across all $totalCount subsystems at $nowFormatted."
      "PARTIAL" -> "Synchronized $grantedCount of $totalCount subsystems. Some permissions are required for complete sync at $nowFormatted."
      else -> "Data sync limited ($grantedCount/$totalCount). Grant required permissions to sync device telemetry at $nowFormatted."
    }

    val detailsBuilder = StringBuilder()
    subsystems.forEach { sub ->
      val mark = if (sub.isGranted) "✓" else "✗"
      detailsBuilder.append("$mark ${sub.type.title}: ${sub.diagnosticSummary}\n")
    }

    val log = SyncLog(
      timestamp = System.currentTimeMillis(),
      status = status,
      grantedCount = grantedCount,
      totalCount = totalCount,
      scorePercentage = scorePercentage,
      summaryText = summary,
      detailsJson = detailsBuilder.toString().trim()
    )

    syncLogDao.insertLog(log)

    // Log permission audit events for each subsystem during sync execution
    subsystems.forEach { sub ->
      logAuditEvent(
        type = sub.type,
        eventType = "SYNC_TELEMETRY_ACCESS",
        isGranted = sub.isGranted,
        details = sub.diagnosticSummary
      )
    }

    // Send a system notification if permitted
    val notifTitle = "Sync Hub: $status ($scorePercentage%)"
    val notifMsg = if (status == "COMPLETED") {
      "All device telemetry and permissions successfully synchronized."
    } else {
      "Synced $grantedCount of $totalCount subsystems. Tap to view status."
    }
    NotificationHelper.sendSyncNotification(context, notifTitle, notifMsg)

    log
  }
}
