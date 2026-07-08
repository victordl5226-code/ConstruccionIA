package com.construccionia.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.construccionia.app.util.ConnectivityObserver
import com.construccionia.app.util.ConnectivityStatus

/**
 * Banner de conectividad que se muestra cuando no hay internet.
 * Se desliza desde arriba cuando cambia el estado.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val observer = remember { ConnectivityObserver(context) }
    val connectivityStatus by observer.observe.collectAsState(initial = ConnectivityStatus.AVAILABLE)

    val isOffline = connectivityStatus == ConnectivityStatus.UNAVAILABLE

    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(),
        exit = slideOutVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFB71C1C)) // Rojo oscuro
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SignalWifiOff,
                contentDescription = "Sin conexión",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Sin conexión a internet. Algunas funciones pueden no estar disponibles.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}
