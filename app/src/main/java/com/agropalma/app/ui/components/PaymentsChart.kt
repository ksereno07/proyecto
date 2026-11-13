/*
 * PaymentsChart.kt - GRÁFICA DE BARRAS DE PAGOS
 * ---------------------------------------------
 * La gráfica del dueño: 4 barras que combinan la plata de la cuadrilla
 * (pagada y pendiente) con la de los insumos/productos (pagados y por
 * pagar). Es 100% Compose, sin librerías externas: cada barra es un Box
 * que mide su proporción del máximo, chido y sin dependencias raras.
 *
 * Las barras:
 *  - Cuadrilla pagada   (verde)   = reportes con status PAID.
 *  - Cuadrilla pendiente (mostaza) = reportes aún sin pagar.
 *  - Insumos pagados     (azul)    = materiales con paid = 1.
 *  - Insumos por pagar   (gris)    = materiales con paid = 0.
 */

// el package de los componentes visuales reutilizables
package com.agropalma.app.ui.components

// layout: cajas, columnas, filas y espaciadores
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// textos e iconos de Material 3
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// estados de Compose
import androidx.compose.runtime.Composable
// alineación y modificador
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// el resumen de pagos y el formateador de plata
import com.agropalma.app.data.PaymentsSummary
import com.agropalma.app.ui.theme.PalmInfo
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmOutline
import com.agropalma.app.ui.theme.PalmSuccess
import com.agropalma.app.ui.theme.PalmTertiary
import com.agropalma.app.util.Fmt

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: PaymentsChart — Gráfica de barras de pagos
//  Propósito: mostrar de un viztazo dónde está parada la plata de la
//  finca: qué ya salió del bolsillo y qué todavía debe el dueño.
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PaymentsChart(summary: PaymentsSummary) {
    // la altura fija que tienen las barras cuando están al 100%
    val chartHeight = 150.dp

    // los 4 valores de la gráfica, en el orden en que se pintan
    val values = listOf(
        summary.workersPaid,      // cuadrilla ya pagada
        summary.workersPending,   // cuadrilla por pagar
        summary.materialsPaid,    // insumos ya pagados
        summary.materialsPending  // insumos por pagar
    )

    // el máximo de los 4: la barra más alta; las otras se escalan contra él
    val max = values.max()

    // si todo está en cero no dibujamos barritas peladas, mostramos aviso
    if (max <= 0.0) {
        // el mensajito pa' cuando aún no hay ni un peso movido
        Text(
            "Aún no hay pagos registrados. Cuando liquides reportes o marques insumos como pagados, la gráfica cobra vida.",
            style = MaterialTheme.typography.bodySmall,
            color = PalmMuted
        )
        // aquí nomás terminamos, no hay nada más que pintar
        return
    }

    // la fila de las 4 barras, repartidas a partes iguales con su aire
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // cada barrita va con su valor, color y su etiquetita de abajo
        ChartBar(
            // cuánto vale esta barra (pa' escalarla contra el máximo)
            value = summary.workersPaid, max = max,
            // la plata formateada que sale encima de la barra
            label = Fmt.money(summary.workersPaid),
            // la leyenda de abajo: a quién corresponde
            caption = "Cuadrilla\npagada",
            // verde de éxito: plata que ya salió del bolsillo
            color = PalmSuccess,
            // altura del área de barras, pa' que todas midan igual
            chartHeight = chartHeight,
            // el weight lo pasa el Row papá desde afuera: cada barra
            // ocupa un cuarto del ancho (weight solo existe en un RowScope)
            modifier = Modifier.weight(1f)
        )
        ChartBar(
            // lo que se les debe todavía a los pelados del campo
            value = summary.workersPending, max = max,
            label = Fmt.money(summary.workersPending),
            caption = "Cuadrilla\npor pagar",
            // mostaza: todavía pendiente, "espérate tantito"
            color = PalmTertiary,
            chartHeight = chartHeight,
            // el weight lo pasa el Row papá desde afuera: cada barra
            // ocupa un cuarto del ancho (weight solo existe en un RowScope)
            modifier = Modifier.weight(1f)
        )
        ChartBar(
            // lo gastado en insumos ya pagados al proveedor
            value = summary.materialsPaid, max = max,
            label = Fmt.money(summary.materialsPaid),
            caption = "Insumos\npagados",
            // azulito: la plata de los productos, pa' diferenciarla
            color = PalmInfo,
            chartHeight = chartHeight,
            // el weight lo pasa el Row papá desde afuera: cada barra
            // ocupa un cuarto del ancho (weight solo existe en un RowScope)
            modifier = Modifier.weight(1f)
        )
        ChartBar(
            // lo que costarán los insumos que faltan por pagar
            value = summary.materialsPending, max = max,
            label = Fmt.money(summary.materialsPending),
            caption = "Insumos\npor pagar",
            // gris del outline: lo que todavía no se ha tocado
            color = PalmOutline,
            chartHeight = chartHeight,
            // el weight lo pasa el Row papá desde afuera: cada barra
            // ocupa un cuarto del ancho (weight solo existe en un RowScope)
            modifier = Modifier.weight(1f)
        )
    }

    // aire entre la gráfica y la leyenda de los totales
    Spacer(Modifier.height(14.dp))
    // la leyenda de abajo: dos totales, lo que ya pagó vs lo que debe
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // el total pagado: cuadrilla + insumos ya saldados
        LegendRow("Ya pagado", Fmt.money(summary.workersPaid + summary.materialsPaid), PalmSuccess)
        // el total pendiente: lo que aún debe en la finca
        LegendRow("Por pagar", Fmt.money(summary.workersPending + summary.materialsPending), PalmTertiary)
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: ChartBar — Una sola barra de la gráfica
//  Propósito: columna con la plata arriba, la barra de color al centro
//  (su altura es la proporción del máximo) y la etiqueta abajo.
//  El modifier (con el weight pa' repartir el ancho) lo manda el Row
//  papá desde PaymentsChart, porque weight SOLO existe dentro de un
//  RowScope/ColumnScope, no en un composable suelto como este.
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun ChartBar(
    // cuánto vale esta barra y cuál es el máximo (pa' la proporción)
    value: Double,
    max: Double,
    // la plata formateada y la leyenda de dos líneas de abajo
    label: String,
    caption: String,
    // el color de la barra, cada categoría tiene el suyo
    color: Color,
    // la altura del área de barras, todas deben medir lo mismo
    chartHeight: androidx.compose.ui.unit.Dp,
    // el modifier que llega del Row papá: trae el weight(1f) que
    // reparte el ancho entre las 4 barras a partes iguales
    modifier: Modifier = Modifier
) {
    // la proporción de esta barra contra la más alta (0f a 1f)
    val fraction = if (max > 0.0) (value / max).toFloat() else 0f

    // cada columna ocupa un cuarto del ancho, con su contenido centrado.
    // El modifier del papá trae el weight; si no viniera, nomás se encaja
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // la plata que vale esta barra, chiquita y en negritas
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            // el color de la barra pa' que se relacione de una
            color = color,
            // centrado por si el texto es más ancho que la barra
            textAlign = TextAlign.Center,
            // si no cabe en una línea, que baje a la otra sin drama
            maxLines = 1,
            // y que achique la letra antes de partirse en dos
            softWrap = false
        )
        // un airecito entre la plata y la barra
        Spacer(Modifier.height(4.dp))
        // el "patio de juego" de la barra: altura fija donde crece la barra
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(chartHeight),
            // la barra crece desde el piso, o sea desde abajo
            contentAlignment = Alignment.BottomCenter
        ) {
            // la barra como tal: su altura es la proporción del patio,
            // con mínimo de 4dp pa' que ni el cero quede invisible
            Box(
                modifier = Modifier
                    .width(34.dp)
                    // la altura proporcional: entre 4dp mínimo y el 100%
                    .height(chartHeight * fraction.coerceAtLeast(0.027f))
                    // el color de la categoría, esquinas redonditas arriba
                    .background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )
        }
        // aire entre la barra y su leyenda
        Spacer(Modifier.height(6.dp))
        // la leyenda de dos líneas: "Cuadrilla\npagada" y similares
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = PalmMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: LegendRow — Totales de la leyenda
//  Propósito: filita "Ya pagado / Por pagar" con su puntito de color.
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun LegendRow(label: String, value: String, color: Color) {
    // fila centrada: puntito de color, etiqueta y la plata
    Row(verticalAlignment = Alignment.CenterVertically) {
        // el puntito redondito del color de la categoría
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.size(6.dp))
        // la etiqueta en gris tenue
        Text(label, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
        Spacer(Modifier.size(6.dp))
        // y la plata en negritas del mismo color del puntito
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}
