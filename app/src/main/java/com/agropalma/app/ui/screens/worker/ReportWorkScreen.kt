/*
 * ReportWorkScreen.kt - REPORTAR TRABAJO
 * --------------------------------------
 * El trabajador elige la labor realizada, registra la cantidad con
 * botones grandes (+/- y +5/+10), adjunta una foto obligatoria como
 * evidencia y toma la ubicacion GPS. Antes de enviar muestra el pago
 * estimado (cantidad x tarifa). Al guardar, el reporte aparece en el
 * panel del dueno.
 */

// paquete de la pantalla donde el trabajador reporta su chamba
package com.agropalma.app.ui.screens.worker

// imports de Android y Compose: permisos, toasts, botones y todo el jale visual
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
// imports de AgroPalma: el VM compartido, componentes, tema, GPS y formatos
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Task
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.PhotoEvidence
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSurfaceVariant
import com.agropalma.app.util.Fmt
import com.agropalma.app.util.LocationHelper

// pantalla del formulario pa' reportar el avance de la chamba
@Composable
fun ReportWorkScreen(vm: AppViewModel, nav: NavController, parcelId: Long) {
    // el contexto de la app, pa' toasts y permisos
    val context = LocalContext.current
    // la labor que el compa eligió de la lista
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    // el texto de la cantidad, arranca en 1 porque nadie reporta cero wey
    var unitsText by remember { mutableStateOf("1") }
    // nota opcional que le llega al dueño
    var description by remember { mutableStateOf("") }
    // la ruta de la foto de evidencia
    var photoPath by remember { mutableStateOf<String?>(null) }
    // las coordenadas GPS de dónde estaba trabajando
    var location by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // aquí se guarda el mensito rojo si algo sale mal
    var error by remember { mutableStateOf<String?>(null) }

    // convertimos el texto a número, si escribió pura basura queda en 0
    val units = unitsText.replace(",", ".").toDoubleOrNull() ?: 0.0

    // al abrir la pantalla cargamos la parcela y agarramos el último GPS
    LaunchedEffect(parcelId) {
        // si la parcela activa no es esta, cargamos sus datos
        if (vm.activeParcel?.id != parcelId) vm.loadParcelData(parcelId)
        // sacamos la última ubicación conocida sin pedir permisos
        location = LocationHelper.lastKnown(context)
    }

    // si aún no elige labor, marcamos la primera pa' que no vaya vacío
    LaunchedEffect(vm.activeTasks) {
        if (selectedTask == null) selectedTask = vm.activeTasks.firstOrNull()
    }

    // lanzador pa' pedir el permiso de ubicación a lo Android
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // si el compa aceptó, agarramos su ubicación
        if (granted) location = LocationHelper.lastKnown(context)
    }

    // andamiaje de la pantalla con fondo crema, la marca de la casa
    Scaffold(
        containerColor = PalmCream,
        topBar = {
            // barra de arriba con el título y el botón de regresar
            AgroTopBar(
                "Reportar Trabajo",
                vm.activeParcel?.name ?: "Evidencia y avance",
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna scrolleable pa' que quepa todo en pantallas chiquitas
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // tarjeta 1: aquí eliges qué labor venías haciendo
            AgroCard {
                Text("1. Labor realizada", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // si la parcela no tiene labores, se lo hacemos saber
                if (vm.activeTasks.isEmpty()) {
                    Text("No hay labores disponibles en esta parcela.", color = PalmMuted)
                }
                // una opción tocable por cada labor que hay en la parcela
                vm.activeTasks.forEach { task ->
                    TaskOption(
                        task = task,
                        selected = selectedTask?.id == task.id,
                        onClick = { selectedTask = task }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // tarjeta 2: el contador de avance con sus botones
            AgroCard {
                Text("2. Conteo de avance", style = MaterialTheme.typography.titleMedium)
                // texto de ayuda pa' los guantes
                Text(
                    "Usa los botones grandes, funcionan con guantes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(10.dp))
                // fila con el botón de restar, el número y el de sumar
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // botón de restar, no baja de cero pa' que no la cagues
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PalmSurfaceVariant
                    ) {
                        IconButton(onClick = {
                            // le quitamos 1 al conteo, mínimo 0
                            val next = (units - 1).coerceAtLeast(0.0)
                            unitsText = Fmt.number(next).replace(",", ".")
                        }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Restar")
                        }
                    }
                    // el número gigante del conteo con su unidad
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // el conteo actual bien grande pa' que se vea hasta con sol
                        Text(
                            Fmt.number(units),
                            style = MaterialTheme.typography.displaySmall,
                            color = PalmPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        // la unidad de la labor (palmas, kilos, lo que sea)
                        Text(
                            selectedTask?.unit ?: "unidades",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                    }
                    // botón de sumar de uno en uno
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PalmSurfaceVariant
                    ) {
                        IconButton(onClick = {
                            // le sumamos 1 al conteo
                            unitsText = Fmt.number(units + 1).replace(",", ".")
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Sumar")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // botones +5 y +10 gigantes pa' que funcionen con guantes puestos wey
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // botón de sumar 5 de un solo toque
                    OutlinedButton(
                        onClick = { unitsText = Fmt.number(units + 5).replace(",", ".") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("+5")
                    }
                    // y el +10 pa' cuando vas bien avanzado en el conteo
                    OutlinedButton(
                        onClick = { unitsText = Fmt.number(units + 10).replace(",", ".") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("+10")
                    }
                }
                Spacer(Modifier.height(10.dp))
                // campo pa' meter la cantidad exacta a manita
                AgroField(
                    value = unitsText,
                    // solo dejamos números y puntos, nada de letras locas
                    onValueChange = { unitsText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    label = "Cantidad exacta",
                    keyboardType = KeyboardType.Decimal
                )
            }

            // sección 3: sin foto no te deja enviar, la evidencia es obligatoria pendejo
            PhotoEvidence(
                photoPath = photoPath,
                // al elegir foto guardamos la ruta y limpiamos el error
                onPhotoPath = { photoPath = it; error = null },
                label = "3. Evidencia del trabajo",
                required = true
            )

            // tarjeta 4: la nota pa'l dueño y la ubicación GPS
            AgroCard {
                Text("4. Nota y ubicación", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // campo de texto pa' dar más chisme, si quieres no más
                AgroField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Nota para el dueño (opcional)",
                    placeholder = "Ej: corte limpio verificado, palma #124",
                    singleLine = false,
                    minLines = 3
                )
                Spacer(Modifier.height(10.dp))
                // fila del GPS con su iconito de ubicación
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PalmPrimary)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        // te dice si ya hay GPS o no
                        Text(
                            if (location != null) "GPS registrado" else "GPS no disponible",
                            style = MaterialTheme.typography.labelLarge
                        )
                        // las coordenadas formateadas bien bonitas
                        Text(
                            Fmt.coordinates(location?.first, location?.second),
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                    }
                    // botón pa' ubicar: si hay permiso saca el GPS, si no lo pide
                    OutlinedButton(
                        onClick = {
                            if (LocationHelper.hasPermission(context)) {
                                // ya tenía permiso, agarramos la ubicación directo
                                location = LocationHelper.lastKnown(context)
                            } else {
                                // sin permiso se lo pedimos amablemente
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (location == null) "Ubicar" else "Actualizar")
                    }
                }
            }

            // el pago estimado en vivo, la parte buena wey
            if (selectedTask != null) {
                // cajita destacada con el dinero que vas a ganar
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pago estimado de esta labor", style = MaterialTheme.typography.bodySmall, color = PalmPrimary)
                            // cantidad por tarifa, o sea tu feria
                            Text(
                                Fmt.money(units * selectedTask!!.ratePerUnit),
                                style = MaterialTheme.typography.titleLarge,
                                color = PalmPrimary
                            )
                        }
                        // la tarifa por unidad pa' que sepas cuánto pagan
                        Text(
                            "${Fmt.money(selectedTask!!.ratePerUnit)} / ${selectedTask!!.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmPrimary
                        )
                    }
                }
            }

            // caja roja del error cuando te faltó algo
            if (error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        error!!,
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // el botón grande de enviar, aquí se hace todo el chunte
            Button(
                onClick = {
                    val task = selectedTask
                    // validamos que haya elegido una labor
                    if (task == null) {
                        error = "Selecciona una labor."
                    } else if (photoPath == null) {
                        // sin foto no te deja enviar, la evidencia es obligatoria
                        error = "La foto de evidencia es obligatoria."
                    } else {
                        // mandamos el reporte completo al VM pa' que valide y guarde
                        vm.reportWork(
                            parcelId = parcelId,
                            taskId = task.id,
                            units = units,
                            description = description,
                            photoPath = photoPath,
                            latitude = location?.first,
                            longitude = location?.second
                        ) { result ->
                            // si result viene null todo salió chido
                            if (result == null) {
                                Toast.makeText(context, "Reporte enviado al dueño", Toast.LENGTH_SHORT).show()
                                // regresamos a la pantalla anterior
                                nav.popBackStack()
                            } else {
                                // si trae texto es que algo truino
                                error = result
                            }
                        }
                    }
                },
                // deshabilitado mientras está ocupado pa' no mandar doble
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                // el texto cambia cuando está enviando
                Text(if (vm.busy) "Enviando…" else "Enviar Reporte y Evidencia", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// composable chiquito que dibuja cada opción de labor en la lista
@Composable
private fun TaskOption(task: Task, selected: Boolean, onClick: () -> Unit) {
    // tarjeta tocable que se pinta distinto si ya la elegiste
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else PalmSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) PalmPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                // el título de la labor
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                // la meta y la tarifa, pa' que veas el chiste completo
                Text(
                    "Meta ${task.targetCount} ${task.unit} • ${Fmt.money(task.ratePerUnit)}/${task.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
            }
            // etiqueta de "Elegida" cuando ya la marcaste
            if (selected) {
                Text("Elegida", style = MaterialTheme.typography.labelMedium, color = PalmPrimary)
            }
        }
    }
}
