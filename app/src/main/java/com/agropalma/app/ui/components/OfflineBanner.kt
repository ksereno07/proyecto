/*
 * OfflineBanner.kt - LA CINTA DE "SIN INTERNET"
 * ---------------------------------------------
 * Franja verde oscuro que se pega ARRIBA de toda la app cuando no hay
 * señal. Dice cuántos registros están esperando en la cola y que se van
 * a enviar solos cuando el teléfono recupere internet.
 * Si hay internet, el banner se esconde solo (con animación, pa' que
 * se vea chido).
 */

package com.agropalma.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agropalma.app.ui.theme.PalmPrimary

/**
 * Banner de sin conexión.
 * @param online true = hay internet (el banner se esconde).
 * @param pendientes cuántos registros esperando señal hay en la cola.
 */
@Composable
fun OfflineBanner(online: Boolean, pendientes: Int) {
    // solo se ve si NO hay internet; se despliega y se esconde con animación suave
    AnimatedVisibility(
        visible = !online,
        enter = expandVertically() + fadeIn(), // entra abriéndose de arriba pa' abajo
        exit = shrinkVertically() + fadeOut() // sale cerrándose, sin brincos raros
    ) {
        // la franja verde oscura que cruza todo el ancho de la pantalla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PalmPrimary)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // la nube tachada: icono universal de "no hay red"
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color.White)
            // espaciadito entre el icono y el texto, que no vaya a quedar pegado
            Spacer(Modifier.width(10.dp))
            Column {
                // linea 1: lo importante, cuantos van esperando en la cola
                Text(
                    if (pendientes > 0) "Sin internet · $pendientes registro(s) esperando señal"
                    else "Sin internet",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // linea 2: el mensajito tranquilizador
                Text(
                    if (pendientes > 0) "Se envían solos cuando vuelva la señal. No pierdes nada."
                    else "Todo lo que registres se guarda y se envía solo cuando haya señal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
