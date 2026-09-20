package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.PermissionAuditEvent
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

  fun generateCsvContent(events: List<PermissionAuditEvent>): String {
    val sb = StringBuilder()
    // CSV Header RFC 4180
    sb.append("Event ID,Timestamp,Formatted Date Time,Subsystem,Permission,Event Type,Status,Details,Source\n")

    for (event in events) {
      val formattedDate = dateFormat.format(Date(event.timestamp))
      sb.append(escapeCsvField(event.id.toString())).append(",")
      sb.append(escapeCsvField(event.timestamp.toString())).append(",")
      sb.append(escapeCsvField(formattedDate)).append(",")
      sb.append(escapeCsvField(event.subsystemTitle)).append(",")
      sb.append(escapeCsvField(event.permissionName)).append(",")
      sb.append(escapeCsvField(event.eventType)).append(",")
      sb.append(escapeCsvField(event.status)).append(",")
      sb.append(escapeCsvField(event.details)).append(",")
      sb.append(escapeCsvField(event.appSource)).append("\n")
    }

    return sb.toString()
  }

  fun exportAndShareCsv(context: Context, events: List<PermissionAuditEvent>): Boolean {
    return try {
      val csvContent = generateCsvContent(events)
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val fileName = "permission_audit_log_$timeStamp.csv"

      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val csvFile = File(exportDir, fileName)

      FileWriter(csvFile).use { writer ->
        writer.write(csvContent)
      }

      val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        csvFile
      )

      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Permission Access Audit Log ($fileName)")
        putExtra(Intent.EXTRA_TEXT, "Exported ${events.size} permission access audit log records from Sync Hub.")
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(sendIntent, "Export Permission Audit Logs CSV").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  private fun escapeCsvField(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"") || escaped.contains("\r")) {
      "\"$escaped\""
    } else {
      escaped
    }
  }
}
