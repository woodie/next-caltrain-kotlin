package com.netpress.nextcaltrain

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AboutScreen(
    scheduleDate: Long?,
    isLoading: Boolean = false,
    loadFailed: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current

    val scheduleDateText = scheduleDate?.let {
        SimpleDateFormat("MMM d yyyy", Locale.US).format(Date(it))
    } ?: "Unknown"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Toolbar — back button (hidden when loading)
        if (!isLoading && onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .size(AppStyle.iconButtonSizeDp.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.appText,
                )
            }
        }

        // Center content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nextcaltrain),
                contentDescription = "Next Caltrain icon",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
            )

            Text(
                text = "Next Caltrain",
                fontSize = AppStyle.fontBlurb,
                color = colors.appText,
            )
            Text(
                text = "for Android",
                fontSize = AppStyle.fontOrigin,
                color = colors.appText,
            )
            Text(
                text = "© 2026 John Woodell",
                fontSize = AppStyle.fontOrigin,
                color = colors.appText,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (isLoading) {
                Text(
                    text = if (loadFailed) "Unable to load schedule" else "Loading schedule data",
                    fontSize = AppStyle.fontOrigin,
                    color = colors.appText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(text = "Schedule data:", fontSize = AppStyle.fontOrigin, color = colors.appText)
                    Text(text = scheduleDateText, fontSize = AppStyle.fontOrigin, color = colors.appText)
                }
            }
        }
    }
}
