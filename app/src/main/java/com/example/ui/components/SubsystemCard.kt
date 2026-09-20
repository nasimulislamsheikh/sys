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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubsystemState
import com.example.ui.theme.StatusDenied
import com.example.ui.theme.StatusGranted
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SyncBlue

@Composable
fun SubsystemCard(
  state: SubsystemState,
  onRequestPermission: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  val statusColor = when {
    state.isGranted -> StatusGranted
    state.isPartial -> StatusWarning
    else -> StatusDenied
  }

  val statusLabel = when {
    state.isGranted -> "Active"
    state.isPartial -> "Partial"
    else -> "Required"
  }

  val statusIcon = when {
    state.isGranted -> Icons.Filled.Check
    state.isPartial -> Icons.Filled.Warning
    else -> Icons.Filled.Lock
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("subsystem_card_${state.type.name.lowercase()}")
      .animateContentSize(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
          shape = RoundedCornerShape(20.dp)
        )
        .padding(16.dp)
    ) {
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Icon Box
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(statusColor.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = state.type.icon,
            contentDescription = state.type.title,
            tint = statusColor,
            modifier = Modifier.size(26.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Category
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.type.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = state.type.categoryName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
          )
        }

        // Status Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = statusLabel,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Diagnostic Summary
      Text(
        text = state.diagnosticSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.sp,
        lineHeight = 18.sp
      )

      // Telemetry Details (Collapsible or Preview)
      if (state.telemetryDetails.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (expanded) "Hide Live Telemetry" else "View Live Telemetry (${state.telemetryDetails.size} sensors/metrics)",
            style = MaterialTheme.typography.labelMedium,
            color = SyncBlue,
            fontWeight = FontWeight.SemiBold
          )
          Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = SyncBlue,
            modifier = Modifier.size(18.dp)
          )
        }

        AnimatedVisibility(visible = expanded) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            state.telemetryDetails.forEach { (label, value) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp
                )
                Text(
                  text = value,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }

      // Action Row (if not granted)
      if (!state.isGranted && !state.type.isInstallTimeOnly) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
              .height(38.dp)
              .testTag("settings_button_${state.type.name.lowercase()}"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Settings", fontSize = 12.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onRequestPermission,
            modifier = Modifier
              .height(38.dp)
              .testTag("grant_button_${state.type.name.lowercase()}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = SyncBlue,
              contentColor = Color.White
            )
          ) {
            Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}
