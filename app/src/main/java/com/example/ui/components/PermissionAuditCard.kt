package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PermissionAuditEvent
import com.example.ui.theme.StatusDenied
import com.example.ui.theme.StatusGranted
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SyncBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PermissionAuditCard(
  event: PermissionAuditEvent,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  val dateFormatted = remember(event.timestamp) {
    val df = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
    df.format(Date(event.timestamp))
  }

  val isGranted = event.status.equals("GRANTED", ignoreCase = true) || event.status.equals("AVAILABLE", ignoreCase = true)
  val statusColor = if (isGranted) StatusGranted else StatusDenied

  val subsystemIcon = remember(event.subsystemType) {
    when (event.subsystemType.uppercase()) {
      "LOCATION" -> Icons.Filled.LocationOn
      "BLUETOOTH" -> Icons.Filled.Bluetooth
      "CAMERA" -> Icons.Filled.CameraAlt
      "MICROPHONE" -> Icons.Filled.Mic
      "CALLS" -> Icons.Filled.Call
      "CONTACTS" -> Icons.Filled.Contacts
      "NETWORK" -> Icons.Filled.Wifi
      "SMS" -> Icons.Filled.Sms
      "STORAGE" -> Icons.Filled.SdStorage
      "NOTIFICATIONS" -> Icons.Filled.Notifications
      else -> Icons.Filled.Security
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("permission_audit_card_${event.id}")
      .animateContentSize(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          shape = RoundedCornerShape(16.dp)
        )
        .padding(14.dp)
    ) {
      // Header: Subsystem Badge + Timestamp + Status Tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(SyncBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = subsystemIcon,
              contentDescription = null,
              tint = SyncBlue,
              modifier = Modifier.size(17.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = event.subsystemTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = dateFormatted,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Status Tag (Granted / Denied)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = event.status,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Event Type + Permission identifier
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = event.eventType.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = event.permissionName.substringAfterLast("."),
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Diagnostic detail text
      Text(
        text = event.details,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      // Collapsible technical payload info
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .clickable { expanded = !expanded }
          .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (expanded) "Hide Audit Metadata" else "View Technical Details",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(15.dp)
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
        ) {
          Text(
            text = "Full Permission: ${event.permissionName}\nAudit Event ID: #${event.id}\nSource Process: ${event.appSource}\nTimestamp (Epoch): ${event.timestamp}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
