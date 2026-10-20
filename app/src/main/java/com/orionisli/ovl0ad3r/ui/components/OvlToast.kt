package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var type by mutableStateOf(ToastType.INFO)
        private set
    private var job: Job? = null

    fun show(
        msg: String,
        toastType: ToastType = ToastType.INFO,
        durationMs: Long = 2400L,
        scope: CoroutineScope
    ) {
        job?.cancel()
        message = msg
        type = toastType
        job = scope.launch {
            delay(durationMs)
            message = null
        }
    }

    fun dismiss() {
        job?.cancel()
        message = null
    }
}

val LocalToastState = compositionLocalOf { ToastState() }

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

@Composable
fun OvlToastHost(
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    val msg = toastState.message

    AnimatedVisibility(
        visible = msg != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) +
                scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)
        ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) +
                scaleOut(targetScale = 0.92f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)),
        modifier = modifier
    ) {
        if (msg != null) {
            val colors = OvlTheme.colors
            val isDark = colors.isDark

            val (bgColor, borderColor, iconTint, textColor, iconRes) = when (toastState.type) {
                ToastType.ERROR -> {
                    val bg = if (isDark) Color(0xFF2C1518) else Color(0xFFFFECEE)
                    val border = colors.error.copy(alpha = 0.55f)
                    val icon = colors.error
                    val text = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
                    Tuple5(bg, border, icon, text, R.drawable.ic_warning)
                }
                ToastType.SUCCESS -> {
                    val bg = if (isDark) Color(0xFF1E2330) else Color(0xFFFFFFFF)
                    val border = colors.primary.copy(alpha = 0.42f)
                    val icon = colors.primary
                    val text = colors.textPrimary
                    Tuple5(bg, border, icon, text, R.drawable.ic_check_circle)
                }
                ToastType.WARNING -> {
                    val bg = if (isDark) Color(0xFF2C2214) else Color(0xFFFFF8E1)
                    val border = Color(0xFFFFA000).copy(alpha = 0.55f)
                    val icon = Color(0xFFFFA000)
                    val text = if (isDark) Color(0xFFFFE082) else Color(0xFFE65100)
                    Tuple5(bg, border, icon, text, R.drawable.ic_warning)
                }
                ToastType.INFO -> {
                    val bg = if (isDark) Color(0xFF1E2330) else Color(0xFFFFFFFF)
                    val border = colors.outlineVariant
                    val icon = colors.primary
                    val text = colors.textPrimary
                    Tuple5(bg, border, icon, text, R.drawable.ic_info)
                }
            }

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.22f),
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.12f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { toastState.dismiss() }
                    )
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
