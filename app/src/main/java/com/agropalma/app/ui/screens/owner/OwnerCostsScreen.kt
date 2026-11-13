/*
 * OwnerCostsScreen.kt - COSTOS Y MATERIALES
 * -----------------------------------------
 * Calcula cuanto debe pagar el dueno:
 *  - Total pendiente a trabajadores (reportes no pagados).
 *  - Costo de insumos solicitados (cantidad x precio promedio).
 *  - Total estimado general.
 * Tambien muestra el saldo por trabajador (pendiente/pagado), los
 * insumos por comprar y el catalogo de precios promedio de mercado.
 */

// el paquete: la pantalla de costos también vive en la zona del dueño
package com.agropalma.app.ui.screens.owner

// imports de Compose y Material 3: layout, iconos, texto y todo el kit visual
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// imports caseros de AgroPalma: ViewModel, componentes, colores y el formato Fmt
import com.agropalma.app.AppViewModel
import com.agropalma.app.ui.components.AgroBottomBar
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.EmptyState
import com.agropalma.app.ui.components.HDivider
import com.agropalma.app.ui.components.PaymentsChart
import com.agropalma.app.ui.components.SectionTitle
import com.agropalma.app.ui.components.StatusPill
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmPrimaryDark
import com.agropalma.app.ui.theme.PalmSurface
import com.agropalma.app.ui.theme.PalmTertiary
import com.agropalma.app.util.Fmt

// la pantalla de la cartera: cuánta plata se debe por trabajos y por insumos
@Composable
fun OwnerCostsScreen(vm: AppViewModel, nav: NavController) {
    // la parcela activa del dueño, pa' poner su nombre en el título
    val parcel = vm.ownerParcel

    // al entrar, refrescamos todo el data del dueño, bien fresquito
    LaunchedEffect(Unit) { vm.loadOwnerData() }

    // andamio: fondo crema, barra de título arriba y tab bar abajo
    Scaffold(
        // el fondo crema de siempre, no la cagues con otro color
        containerColor = PalmCream,
        // título "Costos y Materiales" con el nombre de la parcela
        topBar = { AgroTopBar("Costos y Materiales", parcel?.name ?: "Tu parcela") },
        // la barra de abajo, aquí "costs" es la posición 2
        bottomBar = {
            AgroBottomBar(ownerBottomItems, selected = 2) { index ->
                // según el toque: inicio, reportes o perfil del dueño
                when (index) {
                    0 -> ownerNavigate(nav, "owner/home")
                    1 -> ownerNavigate(nav, "owner/reports")
                    3 -> ownerNavigate(nav, "owner/profile")
                }
            }
        }
    ) { padding ->
        // sin parcela activa: aviso pa' crearla y chao, no hay nada que calcular
        if (parcel == null) {
            // columna con el padding pa' que el aviso quede bien puesto
            Column(Modifier.fillMaxSize().padding(padding)) {
                // estado vacío con el iconito de billetes y botón de crear
                EmptyState(
                    icon = Icons.Filled.Payments,
                    title = "Sin parcela activa",
                    message = "Crea tu grupo de parcela para empezar a calcular costos.",
                    actionLabel = "Crear parcela",
                    onAction = { nav.navigate("owner/create") }
                )
            }
            // aquí nomás nos salimos del Scaffold
            return@Scaffold
        }

        // el gran total: lo pendiente de la cuadrilla más el costo de insumos
        val total = vm.ownerStats.pendingPay + vm.ownerStats.materialsTotal

        // lista perezosa con todo: tarjeta grande, tabla de saldos e insumos
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // primer bloque: la tarjeta verde del gran total
            item {
                // le pasamos lo pendiente por pagar, insumos y el total
                BalanceCard(
                    pendingPay = vm.ownerStats.pendingPay,
                    materials = vm.ownerStats.materialsTotal,
                    total = total
                )
            }

            // la GRÁFICA DE PAGOS: barras que combinan cuadrilla e insumos,
            // lo pagado contra lo pendiente, todo en una sola mirada
            item {
                // la tarjeta que contiene la gráfica de barras
                AgroCard {
                    // el título de la gráfica, pa' que se sepa qué es
                    Text(
                        "Gráfica de pagos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // el subtítulo: qué combina esta gráfica
                    Text(
                        "Pagos de trabajadores y de insumos, lo ya pagado vs lo pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                    // aire entre el título y las barras
                    Spacer(Modifier.height(16.dp))
                    // las barras salen del resumen de pagos del ViewModel
                    PaymentsChart(vm.paymentsSummary)
                }
            }

            // título de la sección de la cuadrilla con su conteo de pelados
            item {
                SectionTitle(
                    "Por pagar a trabajadores",
                    "${vm.ownerTotals.size} personas en la cuadrilla"
                )
            }

            // si no hay nadie en la cuadrilla, un aviso pa' compartir el código de acceso
            if (vm.ownerTotals.isEmpty()) {
                item {
                    AgroCard {
                        Text(
                            "Aún no hay trabajadores vinculados. Comparte el código de acceso de tu parcela.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PalmMuted
                        )
                    }
                }
            } else {
                // cada pelao de la cuadrilla se pinta en su tarjetita de saldo
                items(vm.ownerTotals) { worker ->
                    AgroCard {
                        // fila: avatar, nombre y lo pendiente a la derecha
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // el avatar cuadradito del trabajador
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = RoundedCornerShape(13.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                // columna centrada pa' que el muñequito quede bonito
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // el iconito de persona verde del avatar
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.size(12.dp))
                            // columna del nombre y su resumen de reportes
                            Column(Modifier.weight(1f)) {
                                Text(worker.workerName, style = MaterialTheme.typography.titleMedium)
                                // "12 reportes • pagado $500": lo que ya se le liquidó
                                Text(
                                    "${worker.reports} reportes • pagado ${Fmt.money(worker.paid)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                            // columna derecha: lo que aún se le debe al pelao
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Pendiente", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                                // la plata pendiente: verde si debe, gris si ya está al corriente
                                Text(
                                    Fmt.money(worker.pending),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (worker.pending > 0) PalmPrimary else PalmMuted
                                )
                            }
                        }
                    }
                }
            }

            // título de la sección de insumos, ahora con control de pagos
            item {
                SectionTitle("Insumos y sus pagos", "Marca los que ya pagaste: salen en la gráfica")
            }

            // sin pedidos ni viejos ni nuevos: tarjetita con el aviso
            if (vm.ownerMaterials.isEmpty()) {
                item {
                    AgroCard {
                        Text(
                            "Todavía no hay solicitudes de materiales de tu cuadrilla.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PalmMuted
                        )
                    }
                }
            } else {
                // AHORA se muestran TODOS los pedidos (pendientes y entregados),
                // cada uno con su botón de "pagado" pa' la gráfica de pagos
                items(vm.ownerMaterials) { material ->
                    // la tarjeta del insumo con su estado de pago
                    AgroCard {
                        // fila: el insumo y su pastilla de estado (pendiente/entregado)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // columna del nombre y quién lo pidió
                            Column(Modifier.weight(1f)) {
                                Text(material.item, style = MaterialTheme.typography.titleMedium)
                                // "5 kg • Juanito": la cantidad y el pelao que lo pidió
                                Text(
                                    "${Fmt.number(material.quantity)} ${material.unit} • ${material.workerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                            // la pastillita de estado del pedido
                            StatusPill(material.status)
                        }
                        Spacer(Modifier.height(8.dp))
                        // fila del dinero: el costo y el botón de pagado
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // columna del costo del insumo
                            Column(Modifier.weight(1f)) {
                                Text("Costo", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                                // cantidad por precio: lo que cuesta la nota completa
                                Text(
                                    Fmt.money(material.quantity * material.unitPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PalmPrimary
                                )
                            }
                            // el toggle de pago: verde lleno si falta pagar,
                            // con borde y palomita si ya quedó saldado
                            if (material.paid) {
                                // ya pagado: outlined con palomita, se puede desmarcar
                                OutlinedButton(
                                    onClick = {
                                        // el dueño se equivocó? desmarca y vuelve a la cola
                                        vm.setMaterialPaid(material.id, false)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    // la palomita de que ya está saldado
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Pagado")
                                }
                            } else {
                                // sin pagar: botón verde lleno pa' marcarlo de una
                                Button(
                                    onClick = {
                                        // se marca como pagado y la gráfica se actualiza
                                        vm.setMaterialPaid(material.id, true)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Marcar pagado")
                                }
                            }
                        }
                    }
                }
            }

            // título del catálogo de precios de referencia de la zona
            item {
                SectionTitle("Precios promedio de mercado", "Referencia para tu zona y proveedores")
            }

            // sin catálogo: tarjetita corta con el aviso
            if (vm.prices.isEmpty()) {
                item {
                    AgroCard { Text("Sin catálogo de precios.", color = PalmMuted) }
                }
            } else {
                item {
                    AgroCard {
                        // recorremos todos los precios con su índice pa' las líneas divisorias
                        vm.prices.forEachIndexed { index, price ->
                            // fila: iconito, nombre del insumo y su precio
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // la tiendita gris del insumo
                                Icon(
                                    Icons.Filled.Store,
                                    contentDescription = null,
                                    tint = PalmMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(10.dp))
                                // nombre del insumo y su unidad de venta
                                Column(Modifier.weight(1f)) {
                                    Text(price.item, style = MaterialTheme.typography.bodyMedium)
                                    Text(price.unit, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                                }
                                // el precio promedio en verde, a la derecha
                                Text(
                                    Fmt.money(price.avgPrice),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PalmPrimary
                                )
                            }
                            // entre precio y precio va una línea divisoria (menos en el último)
                            if (index < vm.prices.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                                HDivider()
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // la letra chica: los precios son referencia, no la biblia
            item {
                Text(
                    "Los precios son promedios de referencia y pueden variar según el proveedor y la zona. " +
                        "El total estimado se calcula con las solicitudes pendientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
            }
        }
    }
}

// la tarjeta verde grande: el total estimado a pagar del dueño
@Composable
private fun BalanceCard(pendingPay: Double, materials: Double, total: Double) {
    // superficie verde palma que ocupa todo el ancho, esquinas bien redondeadas
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PalmPrimary,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // la etiqueta chiquita que va arriba del número
            Text("Total estimado a pagar", style = MaterialTheme.typography.bodySmall, color = PalmSurface.copy(alpha = 0.7f))
            // el número gordo de la cuenta, imposible de ignorar
            Text(
                Fmt.money(total),
                style = MaterialTheme.typography.displaySmall,
                color = PalmSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            // fila con los dos recuadritos: trabajadores e insumos
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // lo que se debe a la cuadrilla
                SmallBalance("Trabajadores", pendingPay, Icons.Filled.Payments, Modifier.weight(1f))
                // lo que costarían los materiales pedidos
                SmallBalance("Insumos", materials, Icons.Filled.Inventory2, Modifier.weight(1f))
            }
        }
    }
}

// los recuadritos translúcidos dentro de la tarjeta verde: una mini cuenta
@Composable
private fun SmallBalance(label: String, value: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    // superficie blanca con 10% de transparencia, o sea vidrio esmerilado
    Surface(
        modifier = modifier,
        color = PalmSurface.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // el iconito de la categoría (billetes o cajita)
            Icon(icon, contentDescription = null, tint = PalmTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            // la etiqueta de la mini cuenta, medio transparente
            Text(label, style = MaterialTheme.typography.bodySmall, color = PalmSurface.copy(alpha = 0.7f))
            // y su cantidad en blanco, pa' que resalte sobre el verde
            Text(Fmt.money(value), style = MaterialTheme.typography.titleMedium, color = PalmSurface)
        }
    }
}
