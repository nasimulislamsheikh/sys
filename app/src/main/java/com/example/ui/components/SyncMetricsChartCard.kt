package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubsystemState
import com.example.ui.theme.StatusDenied
import com.example.ui.theme.StatusGranted
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SyncBlue
import com.example.ui.theme.SyncCyan
import com.example.ui.theme.SyncIndigo

@Composable
fun SyncMetricsChartCard(
  subsystems: List<SubsystemState>,
  lastSyncTimeFormatted: String,
  overallPercentage: Int,
  modifier: Modifier = Modifier
) {
  val animatedProgress by animateFloatAsState(
    targetValue = overallPercentage / 100f,
    animationSpec = tween(durationMillis = 1000),
    label = "overall_progress"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("sync_metrics_chart_card"),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 3.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
          shape = RoundedCornerShape(22.dp)
        )
        .padding(20.dp)
    ) {
      // Header with Title and Last Sync Time
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Sync Health Analytics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Live status & subsystem telemetry",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = SyncBlue,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = lastSyncTimeFormatted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Multi-bar Category Distribution Canvas
      Text(
        text = "Subsystem Readiness Distribution",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Custom Compose Bar Chart
      SubsystemBarChart(subsystems = subsystems)

      Spacer(modifier = Modifier.height(20.dp))

      // Category Summary Legend
      val activeCount = subsystems.count { it.isGranted }
      val partialCount = subsystems.count { it.isPartial }
      val pendingCount = subsystems.count { !it.isGranted && !it.isPartial }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(vertical = 10.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        LegendItem(color = StatusGranted, label = "Active ($activeCount)")
        LegendItem(color = StatusWarning, label = "Partial ($partialCount)")
        LegendItem(color = StatusDenied, label = "Restricted ($pendingCount)")
      }
    }
  }
}

@Composable
fun SubsystemBarChart(
  subsystems: List<SubsystemState>,
  modifier: Modifier = Modifier
) {
  val trackColor = MaterialTheme.colorScheme.surfaceVariant

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    subsystems.forEach { sub ->
      val score = when {
        sub.isGranted -> 1f
        sub.isPartial -> 0.5f
        else -> 0.15f
      }

      val barColor = when {
        sub.isGranted -> StatusGranted
        sub.isPartial -> StatusWarning
        else -> StatusDenied
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = sub.type.categoryName,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.width(82.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier
            .weight(1f)
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(trackColor)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(score)
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(barColor)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
          text = if (sub.isGranted) "100%" else if (sub.isPartial) "50%" else "0%",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = barColor,
          modifier = Modifier.width(36.dp)
        )
      }
    }
  }
}

@Composable
private fun LegendItem(
  color: Color,
  label: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
