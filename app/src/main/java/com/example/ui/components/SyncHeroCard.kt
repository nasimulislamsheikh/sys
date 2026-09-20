package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusDenied
import com.example.ui.theme.StatusGranted
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SyncBlue
import com.example.ui.theme.SyncCyan
import com.example.ui.theme.SyncIndigo

@Composable
fun SyncHeroCard(
  grantedCount: Int,
  totalCount: Int,
  scorePercentage: Int,
  isSyncing: Boolean,
  onGrantAllClick: () -> Unit,
  onSyncNowClick: () -> Unit,
  onOpenSettingsClick: () -> Unit,
  onTestAlertClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val animatedProgress by animateFloatAsState(
    targetValue = scorePercentage / 100f,
    animationSpec = tween(durationMillis = 800),
    label = "score_progress"
  )

  val statusColor = when {
    scorePercentage == 100 -> StatusGranted
    scorePercentage >= 50 -> StatusWarning
    else -> StatusDenied
  }

  val statusBadgeText = when {
    scorePercentage == 100 -> "FULL ACCESS • READY TO SYNC"
    scorePercentage >= 50 -> "PARTIAL ACCESS • $grantedCount/$totalCount GRANTED"
    else -> "PERMISSIONS NEEDED • $grantedCount/$totalCount"
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("sync_hero_card"),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    shadowElevation = 6.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          brush = Brush.horizontalGradient(
            listOf(
              MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              SyncCyan.copy(alpha = 0.3f),
              MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
          ),
          shape = RoundedCornerShape(24.dp)
        )
        .padding(20.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Top row: Status Badge & Settings icon
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(statusColor.copy(alpha = 0.15f))
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = if (scorePercentage == 100) Icons.Filled.CheckCircle else Icons.Filled.Warning,
              contentDescription = null,
              tint = statusColor,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = statusBadgeText,
              color = statusColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onTestAlertClick,
              modifier = Modifier.size(36.dp).testTag("test_alert_button")
            ) {
              Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = "Test Notification Alert",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
            IconButton(
              onClick = onOpenSettingsClick,
              modifier = Modifier.size(36.dp).testTag("open_settings_button")
            ) {
              Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Open App Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Center: Circular Progress & Overview
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Sync Health Score",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (scorePercentage == 100) {
                "All 10 hardware & data subsystems are authorized for complete synchronization."
              } else {
                "Grant remaining permissions to enable GPS, Bluetooth, Call, SMS, and Storage sync."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 18.sp
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Circular Score Indicator
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(84.dp)
          ) {
            CircularProgressIndicator(
              progress = { 1f },
              modifier = Modifier.size(84.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              strokeWidth = 8.dp,
              strokeCap = StrokeCap.Round
            )
            CircularProgressIndicator(
              progress = { animatedProgress },
              modifier = Modifier.size(84.dp),
              color = statusColor,
              strokeWidth = 8.dp,
              strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$scorePercentage%",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "$grantedCount/$totalCount",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (scorePercentage < 100) {
            Button(
              onClick = onGrantAllClick,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("grant_all_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = SyncBlue,
                contentColor = Color.White
              )
            ) {
              Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Grant Missing",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
              )
            }
          }

          Button(
            onClick = onSyncNowClick,
            enabled = !isSyncing,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("sync_now_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (scorePercentage == 100) SyncBlue else MaterialTheme.colorScheme.primaryContainer,
              contentColor = if (scorePercentage == 100) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isSyncing) "Syncing..." else "Sync Now",
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
          }
        }
      }
    }
  }
}
