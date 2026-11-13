/*
 * WorkerParcelScreen.kt - DETALLE DE LA PARCELA (TRABAJADOR)
 * ----------------------------------------------------------
 * Muestra los datos de la parcela, el avance de cada labor (porcentaje
 * segun lo reportado), los reportes del trabajador con su estado de pago
 * y los ultimos diagnosticos. Desde aqui se abre reportar trabajo, pedir
 * materiales o usar el escaner de palma.
 */

// el package, o sea dónde vive esta pantalla dentro del proyecto
package com.agropalma.app.ui.screens.worker

// imports de Compose: layout, iconos, Material 3 y navegación, el combo completo
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// lo propio de AgroPalma: ViewModel, datos de las labores y componentes de la casa
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Task
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.EmptyState
import com.agropalma.app.ui.components.SectionTitle
import com.agropalma.app.ui.components.StatusPill
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmPrimaryDark
import com.agropalma.app.ui.theme.PalmSecondary
import com.agropalma.app.ui.theme.PalmTertiary
import com.agropalma.app.util.Fmt

// la parcela activa del trabajador: totales, botones de acción y sus labores
@Composable
fun WorkerParcelScreen(vm: AppViewModel, nav: NavController, parcelId: Long) {
    // la parcela que está viendo el pelao en este momento
    val parcel = vm.activeParcel
    // el usuario logueado, o sea el trabajador
    val user = vm.currentUser
    // de todos los reportes de la parcela, nomás los que hizo este pelao
    val myReports = vm.activeReports.filter { it.workerId == user?.id }
    // la plata que le deben: reportes no pagados, sumados ahí nomás
    val pendingPay = myReports.filter { it.status != "PAID" }.sumOf { it.amount }

    // apenas entra carga los datos de esta parcela (labores, reportes, diagnósticos)
    LaunchedEffect(parcelId) { vm.loadParcelData(parcelId) }

    // esqueleto de la pantalla con su barra de arriba
    Scaffold(
        // fondo crema de la app
        containerColor = PalmCream,
        // barra de arriba con nombre y ubicación de la parcela
        topBar = {
            AgroTopBar(
                // si ya cargó, el nombre; si no, "Parcela" mientras tanto
                parcel?.name ?: "Parcela",
                // la ubicación o un "Cargando…" pa' no dejarla vacía
                parcel?.location ?: "Cargando…",
                // botón de volver: limpia la parcela activa y regresa
                onBack = {
                    vm.clearActiveParcel()
                    nav.popBackStack()
                }
            )
        }
    ) { padding ->
        // si la parcela todavía no carga, mostramos pantalla de carga y ya
        if (parcel == null) {
            // columna que llena la pantalla mientras carga
            Column(Modifier.fillMaxSize().padding(padding)) {
                // estado vacío que dice que estamos leyendo las labores
                EmptyState(
                    icon = Icons.Filled.Assignment,
                    title = "Cargando parcela…",
                    message = "Estamos leyendo las labores asignadas a tu cuadrilla."
                )
            }
            // nos salimos del Scaffold, aquí no hay nada más que hacer
            return@Scaffold
        }

        // lista con scroll con todo el contenido de la parcela
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            // margen de 16 pa' que no se pegue a los bordes
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            // espacio de 14 entre bloques
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // la tarjeta héroe de la parcela, la de hasta arriba
            item {
                // surface transparente que nomás da la forma de las esquinas
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    // columna con fondo degradado bien chido
                    Column(
                        Modifier
                            .fillMaxWidth()
                            // el degradado de verde oscuro a verde secundario
                            .background(
                                Brush.linearGradient(listOf(PalmPrimaryDark, PalmSecondary)),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        // etiqueta chiquita de "Parcela activa"
                        Text("Parcela activa", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                        // el nombre de la parcela en grande
                        Text(parcel.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        // fila con la ubicación y las hectáreas
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // iconito de pin en blanco medio transparente
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                            // ubicación y hectáreas de la parcela
                            Text(
                                " ${parcel.location} • ${Fmt.number(parcel.hectares)} ha",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        // quién es el patrón de esta parcela
                        Text(
                            "Patrón: ${parcel.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(14.dp))
                        // fila de los 3 mini totales, los de abajo de la tarjeta
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // cuántos reportes ha hecho el pelao aquí
                            StatMini("Mis reportes", myReports.size.toString(), Modifier.weight(1f))
                            // la plata que le deben hasta ahora
                            StatMini("Por cobrar", Fmt.money(pendingPay), Modifier.weight(1f))
                            // los diagnósticos del lote, o sea las alertas
                            StatMini("Alertas", vm.activeDiagnoses.size.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            // el botón grande de reportar el avance del trabajo
            item {
                Button(
                    // al tocarlo lo manda a la pantalla de reporte de esta parcela
                    onClick = { nav.navigate("worker/report/$parcelId") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    // icono de tarea con su espacio al lado
                    Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Reportar avance del trabajo", fontWeight = FontWeight.SemiBold)
                }
            }
            // fila con los dos botones secundarios: materiales y escáner IA
            item {
                // la fila parte el espacio a la mitad pa' cada botón
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // botón delineado pa' pedir materiales
                    OutlinedButton(
                        // al tocarlo lo manda a pedir materiales de la parcela
                        onClick = { nav.navigate("worker/materials/$parcelId") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        // icono de caja, o sea los materiales
                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Materiales")
                    }
                    // botón del escáner IA con colores bien llamativos
                    Button(
                        // al tocarlo abre el escáner de palmas con IA
                        onClick = { nav.navigate("worker/scanner/$parcelId") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PalmTertiary, contentColor = PalmPrimaryDark)
                    ) {
                        // icono de matraz, ciencia pura wey
                        Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Escáner IA")
                    }
                }
            }

            // título de la sección de labores que le asignó el dueño
            item { SectionTitle("Labores asignadas", "Toca reportar avance para registrar tu trabajo") }

            // si no hay labores, tarjeta que avisa que el patrón no ha creado nada
            if (vm.activeTasks.isEmpty()) {
                item { AgroCard { Text("El dueño aún no ha creado labores.", color = PalmMuted) } }
            } else {
                // cada labor va en su tarjetita con su progreso
                items(vm.activeTasks) { task ->
                    // le pasamos la labor y su porcentaje de avance ya calculado
                    TaskCard(task, progressFor(task, vm))
                }
            }

            // título de la sección de sus reportes enviados
            item { SectionTitle("Mis reportes", "Trabajo enviado y su estado de pago") }

            // si no ha reportado nada, tarjeta que se lo recuerda
            if (myReports.isEmpty()) {
                item {
                    AgroCard {
                        Text(
                            "Todavía no has enviado reportes en esta parcela.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PalmMuted
                        )
                    }
                }
            } else {
                // cada reporte del pelao en su tarjetita
                items(myReports) { report ->
                    AgroCard {
                        // fila principal: info del reporte y su pill de estado
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // columna con la labor y los detalles del reporte
                            Column(Modifier.weight(1f)) {
                                // la labor que reportó
                                Text(report.taskTitle, style = MaterialTheme.typography.titleMedium)
                                // unidades reportadas y cuándo lo mandó
                                Text(
                                    "${Fmt.number(report.units)} ${report.unit} • ${Fmt.date(report.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                            // pill que dice si está pendiente o ya pagado
                            StatusPill(report.status)
                        }
                        Spacer(Modifier.height(6.dp))
                        // fila de la plata que ganó con ese reporte
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // icono de billetes
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(16.dp))
                            // la plata ganada en verde, lo que todos quieren ver
                            Text(
                                "  ${Fmt.money(report.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = PalmPrimary
                            )
                        }
                    }
                }
            }

            // nomás si hay diagnósticos del escáner mostramos esta sección
            if (vm.activeDiagnoses.isNotEmpty()) {
                // título de los análisis del lote
                item { SectionTitle("Diagnósticos del lote", "Últimos análisis del escáner") }
                // nomás los 2 más recientes, pa' no rellenar la pantalla
                items(vm.activeDiagnoses.take(2)) { diagnosis ->
                    AgroCard {
                        // fila con el icono y la info del diagnóstico
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // icono de ciencia, esto viene del escáner IA
                            Icon(Icons.Filled.Science, contentDescription = null, tint = PalmPrimary)
                            Spacer(Modifier.size(10.dp))
                            // columna con la enfermedad y su certeza
                            Column(Modifier.weight(1f)) {
                                // la plaga o enfermedad detectada
                                Text(diagnosis.disease, style = MaterialTheme.typography.titleMedium)
                                // qué tan seguro está la IA y cuándo lo detectó
                                Text(
                                    "${diagnosis.confidence}% de certeza • ${Fmt.date(diagnosis.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
 * Calcula el avance de una labor: suma lo reportado por todos los
 * trabajadores y lo divide entre la meta. Se limita a 0..1 para que la
 * barra nunca se pase del 100%.
 */
private fun progressFor(task: Task, vm: AppViewModel): Float {
    // suma las unidades reportadas por toda la cuadrilla en esa labor
    val done = vm.activeReports.filter { it.taskId == task.id }.sumOf { it.units }
    // si no hay meta definida, el avance es cero, ahí nomás
    if (task.targetCount <= 0) return 0f
    // divide lo hecho entre la meta y lo deja entre 0 y 1 pa' que no se pase del 100%
    return (done / task.targetCount).toFloat().coerceIn(0f, 1f)
}

// tarjetita de cada labor: nombre, descripción, porcentaje y barra de avance
@Composable
private fun TaskCard(task: Task, progress: Float) {
    AgroCard {
        // fila de arriba: info de la labor y el porcentaje a la derecha
        Row(verticalAlignment = Alignment.Top) {
            // columna con el nombre y la descripción de la labor
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(task.description, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
            }
            // pill con el porcentaje de avance bien visible
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                // el porcentaje en enteros, ej: 45%
                Text(
                    "${(progress * 100).toInt()}%",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = PalmPrimary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // la barrita de progreso de la labor: cuántas palmas lleva del total
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = PalmPrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        // fila de abajo: la meta a la izquierda y la tarifa a la derecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            // la meta de la labor, ej: 500 palmas
            Text(
                "Meta: ${task.targetCount} ${task.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = PalmMuted,
                modifier = Modifier.weight(1f)
            )
            // lo que pagan por unidad, o sea la tarifa
            Text(
                "${Fmt.money(task.ratePerUnit)} / ${task.unit}",
                style = MaterialTheme.typography.labelMedium,
                color = PalmPrimary
            )
        }
    }
}

// mini estadística de la tarjeta héroe: etiqueta arriba y valor abajo
@Composable
private fun StatMini(label: String, value: String, modifier: Modifier = Modifier) {
    // fondo blanco medio transparente pa' que se vea encima del degradado
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp)
    ) {
        // columna con la etiqueta y el número
        Column(Modifier.padding(10.dp)) {
            // la etiqueta chiquita, ej: "Por cobrar"
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            // el valor grande en blanco
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}
