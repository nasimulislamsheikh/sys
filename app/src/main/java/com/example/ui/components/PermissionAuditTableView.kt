package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionAuditTableView(
  events: List<PermissionAuditEvent>,
  modifier: Modifier = Modifier,
  initialSubsystemFilter: String = "ALL"
) {
  var selectedSubsystemFilter by remember { mutableStateOf(initialSubsystemFilter) }
  var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "GRANTED", "DENIED"
  var searchQuery by remember { mutableStateOf("") }
  var sortDescending by remember { mutableStateOf(true) }
  var selectedEventForDetail by remember { mutableStateOf<PermissionAuditEvent?>(null) }

  // Quick subsystem counts
  val locationCount = remember(events) { events.count { it.subsystemType == "LOCATION" } }
  val cameraCount = remember(events) { events.count { it.subsystemType == "CAMERA" } }
  val micCount = remember(events) { events.count { it.subsystemType == "MICROPHONE" } }

  // Filtered & Sorted events
  val filteredEvents by remember(events, selectedSubsystemFilter, selectedStatusFilter, searchQuery, sortDescending) {
    derivedStateOf {
      var list = events.asSequence()

      // Subsystem filter
      if (selectedSubsystemFilter != "ALL") {
        list = list.filter { it.subsystemType.equals(selectedSubsystemFilter, ignoreCase = true) }
      }

      // Status filter
      if (selectedStatusFilter == "GRANTED") {
        list = list.filter { it.status.equals("GRANTED", ignoreCase = true) || it.status.equals("AVAILABLE", ignoreCase = true) }
      } else if (selectedStatusFilter == "DENIED") {
        list = list.filter { it.status.equals("DENIED", ignoreCase = true) }
      }

      // Search text query
      if (searchQuery.isNotBlank()) {
        val q = searchQuery.trim().lowercase()
        list = list.filter {
          it.permissionName.lowercase().contains(q) ||
            it.subsystemTitle.lowercase().contains(q) ||
            it.details.lowercase().contains(q) ||
            it.eventType.lowercase().contains(q)
        }
      }

      val sorted = if (sortDescending) {
        list.sortedByDescending { it.timestamp }.toList()
      } else {
        list.sortedBy { it.timestamp }.toList()
      }
      sorted
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("permission_audit_table_container"),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
          shape = RoundedCornerShape(18.dp)
        )
        .padding(16.dp)
    ) {
      // Header with Title & Sort Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(SyncBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.FactCheck,
              contentDescription = null,
              tint = SyncBlue,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Permission Audit Table",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Chronological log of Location, Camera, Microphone & System accesses",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }
        }

        // Sort Direction Toggle Button
        IconButton(
          onClick = { sortDescending = !sortDescending },
          modifier = Modifier.testTag("table_sort_button")
        ) {
          Icon(
            imageVector = if (sortDescending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
            contentDescription = if (sortDescending) "Sort Oldest First" else "Sort Newest First",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Filter by permission, subsystem or details...", fontSize = 12.sp) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "Clear search",
                modifier = Modifier.size(16.dp)
              )
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("table_search_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Subsystem Filter Row (Focusing on requested Location, Camera, Microphone + All)
      val filterScrollState = rememberScrollState()
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(filterScrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val primaryFilters = listOf(
          "ALL" to "All Subsystems (${events.size})",
          "LOCATION" to "Location ($locationCount)",
          "CAMERA" to "Camera ($cameraCount)",
          "MICROPHONE" to "Microphone ($micCount)",
          "STORAGE" to "Storage",
          "CALLS" to "Calls",
          "CONTACTS" to "Contacts"
        )

        primaryFilters.forEach { (key, label) ->
          FilterChip(
            selected = selectedSubsystemFilter == key,
            onClick = { selectedSubsystemFilter = key },
            label = { Text(label, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SyncBlue.copy(alpha = 0.15f),
              selectedLabelColor = SyncBlue
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Status Sub-filters (All / Granted / Denied)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf(
          "ALL" to "Any Status",
          "GRANTED" to "Granted Only",
          "DENIED" to "Denied Only"
        ).forEach { (key, label) ->
          val isSelected = selectedStatusFilter == key
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isSelected) {
                  when (key) {
                    "GRANTED" -> StatusGranted.copy(alpha = 0.18f)
                    "DENIED" -> StatusDenied.copy(alpha = 0.18f)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                  }
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                }
              )
              .clickable { selectedStatusFilter = key }
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = label,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) {
                when (key) {
                  "GRANTED" -> StatusGranted
                  "DENIED" -> StatusDenied
                  else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Table View with Horizontal Scroll
      val tableScrollState = rememberScrollState()

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            shape = RoundedCornerShape(12.dp)
          )
          .horizontalScroll(tableScrollState)
      ) {
        Column(modifier = Modifier.width(820.dp)) {
          // Table Header Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
              .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            TableHeaderCell(text = "TIMESTAMP", width = 125.dp)
            TableHeaderCell(text = "SUBSYSTEM", width = 135.dp)
            TableHeaderCell(text = "STATUS", width = 95.dp)
            TableHeaderCell(text = "PERMISSION REQUESTED", width = 210.dp)
            TableHeaderCell(text = "EVENT TYPE", width = 115.dp)
            TableHeaderCell(text = "DIAGNOSTIC SUMMARY", width = 140.dp)
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

          if (filteredEvents.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No audit log entries found matching criteria",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
              )
            }
          } else {
            filteredEvents.forEachIndexed { index, event ->
              val isEven = index % 2 == 0
              val rowBackground = if (isEven) {
                MaterialTheme.colorScheme.surface
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
              }

              val isGranted = event.status.equals("GRANTED", ignoreCase = true) || event.status.equals("AVAILABLE", ignoreCase = true)
              val statusColor = if (isGranted) StatusGranted else StatusDenied

              val timeStr = remember(event.timestamp) {
                val df = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                df.format(Date(event.timestamp))
              }

              val shortPermission = remember(event.permissionName) {
                event.permissionName.substringAfterLast(".")
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(rowBackground)
                  .clickable { selectedEventForDetail = event }
                  .padding(vertical = 10.dp, horizontal = 12.dp)
                  .testTag("table_row_${event.id}"),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Timestamp
                Text(
                  text = timeStr,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.width(125.dp)
                )

                // Subsystem with Icon
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.width(135.dp)
                ) {
                  Icon(
                    imageVector = getSubsystemIcon(event.subsystemType),
                    contentDescription = null,
                    tint = SyncBlue,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = event.subsystemTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                // Status Chip
                Box(
                  modifier = Modifier
                    .width(95.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(statusColor.copy(alpha = 0.14f))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Icon(
                      imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Close,
                      contentDescription = null,
                      tint = statusColor,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = event.status,
                      color = statusColor,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                // Permission Identifier
                Text(
                  text = shortPermission,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.width(210.dp)
                )

                // Event Type
                Text(
                  text = event.eventType.replace("_", " "),
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.width(115.dp)
                )

                // Diagnostic Details
                Text(
                  text = event.details,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.width(140.dp)
                )
              }

              if (index < filteredEvents.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Count and Quick Hint
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Showing ${filteredEvents.size} of ${events.size} audit events",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Tap any row for full technical breakdown",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }

  // Detail Dialog when tapping a row
  selectedEventForDetail?.let { event ->
    val fullDate = remember(event.timestamp) {
      val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (zzz)", Locale.getDefault())
      df.format(Date(event.timestamp))
    }
    val isGranted = event.status.equals("GRANTED", ignoreCase = true) || event.status.equals("AVAILABLE", ignoreCase = true)

    AlertDialog(
      onDismissRequest = { selectedEventForDetail = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = getSubsystemIcon(event.subsystemType),
            contentDescription = null,
            tint = SyncBlue,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${event.subsystemTitle} Audit Entry",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          DetailItem(label = "Audit ID", value = "#${event.id}")
          DetailItem(label = "Timestamp", value = fullDate)
          DetailItem(label = "Subsystem", value = "${event.subsystemTitle} (${event.subsystemType})")
          DetailItem(label = "Full Permission", value = event.permissionName, isMonospace = true)
          DetailItem(label = "Event Action", value = event.eventType)
          DetailItem(
            label = "Access Status",
            value = event.status,
            color = if (isGranted) StatusGranted else StatusDenied
          )
          DetailItem(label = "Diagnostic Detail", value = event.details)
          DetailItem(label = "Caller Process", value = event.appSource)
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedEventForDetail = null }) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
private fun TableHeaderCell(
  text: String,
  width: androidx.compose.ui.unit.Dp
) {
  Text(
    text = text,
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.width(width)
  )
}

@Composable
private fun DetailItem(
  label: String,
  value: String,
  isMonospace: Boolean = false,
  color: Color = Color.Unspecified
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontSize = 12.sp,
      fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
      color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    )
  }
}

private fun getSubsystemIcon(type: String): ImageVector {
  return when (type.uppercase()) {
    "LOCATION" -> Icons.Filled.LocationOn
    "CAMERA" -> Icons.Filled.CameraAlt
    "MICROPHONE" -> Icons.Filled.Mic
    "BLUETOOTH" -> Icons.Filled.Bluetooth
    "STORAGE" -> Icons.Filled.SdStorage
    "CALLS" -> Icons.Filled.Call
    "CONTACTS" -> Icons.Filled.Contacts
    "NETWORK" -> Icons.Filled.Wifi
    "SMS" -> Icons.Filled.Sms
    "NOTIFICATIONS" -> Icons.Filled.Notifications
    else -> Icons.Filled.Security
  }
}
