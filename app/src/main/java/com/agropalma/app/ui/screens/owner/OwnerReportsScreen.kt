/*
 * OwnerReportsScreen.kt - REPORTES, MATERIALES Y SALUD
 * ----------------------------------------------------
 * Pantalla con tres pestanas para el dueno:
 *  - Reportes: avances de trabajo con foto, GPS, cantidad y valor; boton
 *    para marcar cada reporte como pagado.
 *  - Materiales: pedidos de la cuadrilla, costo estimado con el precio
 *    promedio y boton para marcarlos como entregados.
 *  - Salud: diagnosticos del escaner de palma con certeza y recomendacion.
 */

// el paquete: esta pantalla vive en la carpeta de pantallas del dueño, obvio
package com.agropalma.app.ui.screens.owner

// imports de Compose y Material 3: layout, chips, botones, iconos, todo el kit visual
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
// imports caseros de AgroPalma: el ViewModel, los datos, componentes y colores
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.MaterialRequest
import com.agropalma.app.data.WorkReport
import com.agropalma.app.ui.components.AgroBottomBar
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.EmptyState
import com.agropalma.app.ui.components.StatusPill
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSurfaceVariant
import com.agropalma.app.util.Fmt
import java.io.File

// la pantalla estrella del dueño: reportes, materiales y salud, todo junto aquí
@Composable
fun OwnerReportsScreen(vm: AppViewModel, nav: NavController) {
    // qué pestaña está activa: 0 reportes, 1 materiales, 2 salud (arranca en reportes).
    // rememberSaveable pa' que la pestaña sobreviva si Android recrea la activity
    // (giro, memoria, o que el emulador se ponga travieso), sin volver a reportes
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // la parcela activa del dueño, pa' poner su nombre en la barra de arriba
    val parcel = vm.ownerParcel

    // apenas entras, jalamos data fresca del dueño, nada de info vieja wey
    LaunchedEffect(Unit) { vm.loadOwnerData() }

    // el andamio de la pantalla: fondo crema, barra arriba y tab bar abajo
    Scaffold(
        // fondo crema, el look de siempre de AgroPalma
        containerColor = PalmCream,
        // la barra de arriba: título y el nombre de la parcela (o "Tu parcela")
        topBar = {
            AgroTopBar(
                "Reportes y Alertas",
                parcel?.name ?: "Tu parcela"
            )
        },
        // la tab bar de abajo pa' brincar entre pantallas (reports es la posición 1)
        bottomBar = {
            AgroBottomBar(ownerBottomItems, selected = 1) { index ->
                // según lo que toque: inicio, costos o su perfil
                when (index) {
                    0 -> ownerNavigate(nav, "owner/home")
                    2 -> ownerNavigate(nav, "owner/costs")
                    3 -> ownerNavigate(nav, "owner/profile")
                }
            }
        }
    ) { padding ->
        // columna madre: los chips de pestañas arriba y el contenido abajo
        Column(Modifier.fillMaxSize().padding(padding)) {
            // fila donde van las 3 pastillas de pestañas
            Row(
                // la fila ocupa todo el ancho con su padding pa' no chocar con la orilla
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                // 8dp entre chip y chip pa' que respiren
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // chip 1: REPORTES, con el conteo de reportes entre paréntesis
                FilterChip(
                    // está activo si tab vale 0, y al tocarlo nos cambia ahí mismo
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("Reportes (${vm.ownerReports.size})") },
                    // colores cuando está elegido: verde palma con letra blanca
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PalmPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    // y un borde gris tenue pa' que se marque la pastilla
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = tab == 0,
                        borderColor = PalmMuted.copy(alpha = 0.4f)
                    )
                )
                // chip 2: MATERIALES, los pedidos de la cuadrilla con su conteo
                FilterChip(
                    // activo si tab vale 1, o sea la pestañita de materiales
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("Materiales (${vm.ownerMaterials.size})") },
                    // mismos colores verde palma que el chip anterior
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PalmPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = tab == 1,
                        borderColor = PalmMuted.copy(alpha = 0.4f)
                    )
                )
                // chip 3: SALUD, los diagnósticos del escáner de palma
                FilterChip(
                    // activo si tab vale 2, el último de la tanda
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    label = { Text("Salud (${vm.ownerDiagnoses.size})") },
                    // y el mismo estilo verde palma, ahí nomás igualito
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PalmPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = tab == 2,
                        borderColor = PalmMuted.copy(alpha = 0.4f)
                    )
                )
            }

            // si el dueño no tiene parcela activa, le enseñamos el aviso pa' crear una
            if (parcel == null) {
                // estado vacío con botón directo a crear su grupo de parcela
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "Sin parcela activa",
                    message = "Crea tu grupo de parcela para recibir reportes.",
                    actionLabel = "Crear parcela",
                    onAction = { nav.navigate("owner/create") }
                )
                // y ya, aquí nomás nos salimos, no hay nada que mostrar
                return@Scaffold
            }

            // según la pestaña activa, pintamos su contenido
            when (tab) {
                0 -> ReportsTab(vm)
                1 -> MaterialsTab(vm)
                else -> DiagnosesTab(vm)
            }
        }
    }
}

// la pestaña de REPORTES: la lista de trabajos que mandó la cuadrilla
@Composable
private fun ReportsTab(vm: AppViewModel) {
    // si no hay reportes, aviso de "aún no hay nada" y nos vamos
    if (vm.ownerReports.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.ReceiptLong,
            title = "Aún no hay reportes",
            message = "Cuando tu cuadrilla envíe un trabajo con foto, aparecerá aquí."
        )
        return
    }
    // lista perezosa: solo pinta lo visible, el celular lo agradece wey
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // cada reporte de la lista se vuelve su tarjetita
        items(vm.ownerReports) { report ->
            ReportCard(report) {
                // al tocar "Marcar pagado": queda PAID y se recarga todo el panel
                vm.setReportStatus(report.id, "PAID")
            }
        }
    }
}

// la tarjetita de cada reporte: foto, chisme del trabajo y el botón de pagar
@Composable
private fun ReportCard(report: WorkReport, onPay: () -> Unit) {
    // tarjeta estilo AgroPalma con su sombrita y esquinas
    AgroCard {
        // fila principal: la foto a la izquierda y los datos a la derecha
        Row(verticalAlignment = Alignment.Top) {
            // la foto de evidencia que subió el trabajador (si la tomó, claro)
            ReportPhoto(report.photoPath)
            // airecito entre la foto y el texto
            Spacer(Modifier.size(12.dp))
            // columna con todo el chisme del reporte
            Column(Modifier.weight(1f)) {
                // fila del título con las pastillas de estado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // el título del trabajo, empujado a la izquierda con weight
                    Text(report.taskTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    // la pastillita de estado: PENDIENTE o PAGADO
                    StatusPill(report.status)
                    // pastillita extra cuando el reporte aún no viaja al servidor
                    if (!report.synced) {
                        // un airecito antes de la pastillita gris
                        Spacer(Modifier.size(6.dp))
                        // la pastilla "⏳ Falta enviar", o sea que aún no viaja al servidor
                        SyncPill()
                    }
                }
                // quién hizo el trabajo y cuándo, en chiquito y gris
                Text(
                    "${report.workerName} • ${Fmt.date(report.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                // aire entre líneas pa' que no se amontonen
                Spacer(Modifier.height(4.dp))
                // la cantidad reportada: "5 kg reportadas" por ejemplo
                Text(
                    "${Fmt.number(report.units)} ${report.unit} reportadas",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // si el trabajador dejó nota, ahí nomás la mostramos también
                if (report.description.isNotBlank()) {
                    Text(report.description, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                }
                // otro airecito antes del GPS
                Spacer(Modifier.height(4.dp))
                // fila de la ubicación con su pin
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // el iconito de ubicación gris, chiquitito
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PalmMuted, modifier = Modifier.size(13.dp))
                    // las coordenadas exactas de dónde se hizo el jale
                    Text(
                        " ${Fmt.coordinates(report.latitude, report.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                }
            }
        }
        // separación antes de la fila del dinero, pa' el dramatismo
        Spacer(Modifier.height(10.dp))
        // fila final: la plata a la izquierda y el botón de pagar a la derecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            // cuánto vale el trabajo, en grande y verde pa' que no pase desapercibido
            Text(
                Fmt.money(report.amount),
                style = MaterialTheme.typography.titleLarge,
                color = PalmPrimary,
                modifier = Modifier.weight(1f)
            )
            // si aún no está pagado, le damos el botón pa' liquidarlo
            if (report.status != "PAID") {
                // botón verde "Marcar pagado" con su palomita
                Button(
                    onClick = onPay,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PalmPrimary)
                ) {
                    // la palomita de "ya quedó"
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Marcar pagado")
                }
            } else {
                // si ya está pagado, nomás decimos "Liquidado", sin botón
                Text("Liquidado", style = MaterialTheme.typography.labelLarge, color = PalmPrimary)
            }
        }
    }
}

// la foto de evidencia del reporte, o el placeholder si no existe el archivo
@Composable
private fun ReportPhoto(path: String?) {
    // cajita cuadradita de 92dp, esquinas redondeadas y borde tenue
    Surface(
        modifier = Modifier.size(92.dp),
        shape = RoundedCornerShape(14.dp),
        color = PalmSurfaceVariant,
        border = BorderStroke(1.dp, PalmMuted.copy(alpha = 0.2f))
    ) {
        // solo mostramos la foto si hay ruta Y el archivo existe en el cel, no la cagues
        if (path != null && File(path).exists()) {
            // cargamos la imagen con Coil, recortada pa' que quede chula
            AsyncImage(
                model = File(path),
                contentDescription = "Evidencia",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // si no hay foto, un iconito gris y el texto "Sin foto", ahí nomás
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = PalmMuted)
                Text("Sin foto", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
            }
        }
    }
}

// la pestaña de MATERIALES: lo que la cuadrilla pide pa' trabajar
@Composable
private fun MaterialsTab(vm: AppViewModel) {
    // sin pedidos aún: mostramos el aviso y nos salimos
    if (vm.ownerMaterials.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Inventory2,
            title = "Sin solicitudes de materiales",
            message = "Aquí verás cuando un trabajador pida abono, herramientas o insumos."
        )
        return
    }
    // lista perezosa con las tarjetas de cada pedido
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // cada pedido de material se pinta en su tarjetita
        items(vm.ownerMaterials) { material ->
            // le pasamos las dos acciones: entregar y marcar pagado
            MaterialCard(
                material = material,
                onDeliver = {
                    // al tocar "Marcar como entregado" queda como DELIVERED, fin del pedido
                    vm.setMaterialStatus(material.id, "DELIVERED")
                },
                onTogglePaid = {
                    // al tocar el botón de pago se voltea el estado paid del insumo
                    vm.setMaterialPaid(material.id, !material.paid)
                }
            )
        }
    }
}

// la tarjetita de cada pedido de material con su costo estimado y su pago
@Composable
private fun MaterialCard(material: MaterialRequest, onDeliver: () -> Unit, onTogglePaid: () -> Unit) {
    // tarjeta AgroPalma con el pedido completo
    AgroCard {
        // fila: iconito de tiendita, los datos y las pastillas de estado
        Row(verticalAlignment = Alignment.CenterVertically) {
            // el iconito de tienda verde del insumo pedido
            Icon(Icons.Filled.Store, contentDescription = null, tint = PalmPrimary)
            Spacer(Modifier.size(10.dp))
            // columna con el nombre del material y quién lo pidió
            Column(Modifier.weight(1f)) {
                Text(material.item, style = MaterialTheme.typography.titleMedium)
                // "5 kg • Juanito": la cantidad y el pelao que lo pidió
                Text(
                    "${Fmt.number(material.quantity)} ${material.unit} • ${material.workerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
            }
            // la pastillita de estado: PENDIENTE o ENTREGADO
            StatusPill(material.status)
            // y el aviso de "todavía no se envía" si se creó sin señal
            if (!material.synced) {
                // airecito antes de la pastilla gris
                Spacer(Modifier.size(6.dp))
                // la pastillita "⏳ Falta enviar" del pedido sin señal
                SyncPill()
            }
        }
        // si el trabajador dejó nota en su pedido, la enseñamos aquí
        if (material.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(material.note, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
        }
        // separación antes de la fila de costos
        Spacer(Modifier.height(8.dp))
        // fila del dinero: costo estimado a la izquierda y promedio a la derecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            // columna del costo estimado del pedido
            Column(Modifier.weight(1f)) {
                Text("Costo estimado", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                // cantidad por precio promedio: lo que saldría la nota
                Text(
                    Fmt.money(material.quantity * material.marketAvg),
                    style = MaterialTheme.typography.titleMedium,
                    color = PalmPrimary
                )
            }
            // el precio promedio por unidad pa' que compare el dueño
            Text(
                "Promedio: ${Fmt.money(material.marketAvg)}",
                style = MaterialTheme.typography.bodySmall,
                color = PalmMuted
            )
        }
        // si el pedido aún no se entrega, mostramos el botón hasta abajo
        if (material.status != "DELIVERED") {
            Spacer(Modifier.height(10.dp))
            // botón ancho de "Marcar como entregado" con su palomita
            Button(
                onClick = onDeliver,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                // palomita de "ya quedó entregado"
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Marcar como entregado")
            }
        }
        // el botón del PAGO del insumo: pa' que el dueño diga qué ya saldó.
        // Lo que marque aquí alimenta la gráfica de pagos de la app
        Spacer(Modifier.height(8.dp))
        if (material.paid) {
            // ya pagado: botón con borde y palomita, se puede desmarcar
            androidx.compose.material3.OutlinedButton(
                onClick = onTogglePaid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                // la palomita de que ya quedó saldado con el proveedor
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Pagado al proveedor")
            }
        } else {
            // sin pagar: botón verde lleno pa' marcarlo como pagado de una
            Button(
                onClick = onTogglePaid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Marcar como pagado")
            }
        }
    }
}

// la pestaña de SALUD: los diagnósticos del escáner de palma de la cuadrilla
@Composable
private fun DiagnosesTab(vm: AppViewModel) {
    // sin análisis todavía: aviso vacío y nos vamos
    if (vm.ownerDiagnoses.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Science,
            title = "Sin diagnósticos aún",
            message = "Los análisis del escáner de palma que envíe tu cuadrilla aparecerán aquí."
        )
        return
    }
    // lista perezosa con cada diagnóstico en su tarjetita
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.ownerDiagnoses) { diagnosis ->
            // tarjeta del diagnóstico: foto, enfermedad, certeza y recomendación
            AgroCard {
                // fila: la foto de la palma a la izquierda y los datos a la derecha
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // la foto de la hoja analizada (reusamos el ReportPhoto)
                    ReportPhoto(diagnosis.photoPath)
                    Spacer(Modifier.size(12.dp))
                    // columna con el chisme del diagnóstico
                    Column(Modifier.weight(1f)) {
                        // el nombre de la enfermedad que detectó el escáner
                        Text(diagnosis.disease, style = MaterialTheme.typography.titleMedium)
                        // quién lo escaneó y cuándo
                        Text(
                            "${diagnosis.workerName} • ${Fmt.date(diagnosis.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        // fila de la certeza: qué tan seguro está el escáner de su diagnóstico
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // el iconito del botiquín, chiquito y verde
                            Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(15.dp))
                            // "87% de certeza • severidad", pa' saber si es grave el asunto
                            Text(
                                " ${diagnosis.confidence}% de certeza • ${diagnosis.severity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PalmPrimary
                            )
                        }
                        // si el diagnóstico sigue en la cola esperando señal, avisamos
                        if (!diagnosis.synced) {
                            Spacer(Modifier.height(4.dp))
                            SyncPill()
                        }
                    }
                }
                // aire antes del hallazgo del escáner
                Spacer(Modifier.height(8.dp))
                // lo que encontró el escáner en la palma, en texto plano
                Text(diagnosis.finding, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                Spacer(Modifier.height(6.dp))
                // cajita destacada con la recomendación de qué hacer con la palma
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        diagnosis.recommendation,
                        Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmPrimary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: SyncPill — Pastilla de "esperando señal"
//  Propósito: avisarle al dueño que ese registro se guardó sin internet
//  y todavía no llega al servidor (se envía solo cuando vuelva la red).
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun SyncPill() {
    // pastilla gris discreta: no es error, solo está en cola wey
    Surface(shape = RoundedCornerShape(999.dp), color = PalmSurfaceVariant) {
        // el texto de la pastilla, chiquito y gris pa' no espantar a nadie
        Text(
            "⏳ Falta enviar",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = PalmMuted
        )
    }
}
