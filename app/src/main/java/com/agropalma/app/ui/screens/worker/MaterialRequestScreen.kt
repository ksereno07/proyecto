/*
 * MaterialRequestScreen.kt - PEDIR MATERIALES
 * -------------------------------------------
 * El trabajador elige un insumo del catalogo, indica la cantidad y una
 * nota. La app muestra el precio promedio de mercado y el total estimado
 * antes de enviar la solicitud al dueno.
 */

// paquete de la pantalla pa' pedirle insumos al dueño
package com.agropalma.app.ui.screens.worker

// imports de Android y Compose: toasts, layout, chips, iconos y todo el visual
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
// imports de AgroPalma: el VM, componentes, tema y el formato de dinero
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.PriceItem
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.PhotoEvidence
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSurfaceVariant
import com.agropalma.app.util.Fmt

// OptIn porque el FlowRow de los chips todavía es beta wey
@OptIn(ExperimentalLayoutApi::class)
// pantalla del pedido de insumos, aquí pides lo que hace falta
@Composable
fun MaterialRequestScreen(vm: AppViewModel, nav: NavController, parcelId: Long) {
    // contexto pa' los toasts
    val context = LocalContext.current
    // el material que elegiste de los chips
    var selected by remember { mutableStateOf<PriceItem?>(null) }
    // la cantidad que necesitas, arranca en 1
    var quantityText by remember { mutableStateOf("1") }
    // nota opcional pa' explicarle al dueño
    var note by remember { mutableStateOf("") }
    // la ruta de la foto, si es que le quieres echar una
    var photoPath by remember { mutableStateOf<String?>(null) }
    // pa' mostrar las broncas si algo sale mal
    var error by remember { mutableStateOf<String?>(null) }

    // el texto de la cantidad lo volteamos a número
    val quantity = quantityText.replace(",", ".").toDoubleOrNull() ?: 0.0

    // al abrir cargamos la parcela si no estaba activa
    LaunchedEffect(parcelId) {
        if (vm.activeParcel?.id != parcelId) vm.loadParcelData(parcelId)
        // pre-marcamos el primer material pa' que no vaya vacío
        if (selected == null) selected = vm.prices.firstOrNull()
    }

    // andamiaje con fondo crema y su barra de arriba
    Scaffold(
        containerColor = PalmCream,
        topBar = {
            AgroTopBar(
                "Pedir Materiales",
                vm.activeParcel?.name ?: "Insumos para la parcela",
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna scrolleable con todo el formulario
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // tarjeta 1: elegir qué material hace falta
            AgroCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = PalmPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text("¿Qué material falta?", style = MaterialTheme.typography.titleMedium)
                }
                // texto de ayuda pa' que sepa que el dueño ve los precios
                Text(
                    "El dueño verá tu solicitud con el precio promedio del mercado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(12.dp))
                // fila de chips que se acomodan solitos, bien cómodos
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // un chip por cada material del catálogo
                    vm.prices.forEach { price ->
                        FilterChip(
                            // el chip marcado es el que ya elegiste
                            selected = selected?.item == price.item,
                            onClick = { selected = price },
                            label = { Text(price.item, style = MaterialTheme.typography.bodySmall) },
                            // colores cuando el chip está seleccionado
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PalmPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // tarjeta 2: el contador de la cantidad
            AgroCard {
                Text("Cantidad", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // fila con el botón de restar, el número y el de sumar
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // botón de restar, no baja de 1 porque pedir 0 no sirve wey
                    Surface(shape = RoundedCornerShape(14.dp), color = PalmSurfaceVariant) {
                        IconButton(onClick = {
                            // le quitamos 1 a la cantidad, mínimo 1
                            quantityText = Fmt.number((quantity - 1).coerceAtLeast(1.0)).replace(",", ".")
                        }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Restar")
                        }
                    }
                    // el número grande de la cantidad
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Fmt.number(quantity),
                            style = MaterialTheme.typography.displaySmall,
                            color = PalmPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        // la unidad del material elegido
                        Text(selected?.unit ?: "unidades", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                    }
                    // botón de sumar de uno en uno
                    Surface(shape = RoundedCornerShape(14.dp), color = PalmSurfaceVariant) {
                        IconButton(onClick = {
                            // le sumamos 1 a la cantidad
                            quantityText = Fmt.number(quantity + 1).replace(",", ".")
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Sumar")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // campo pa' escribir la cantidad exacta a manita
                AgroField(
                    value = quantityText,
                    // solo dejamos números y puntos, nada raro
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    label = "Cantidad exacta (${selected?.unit ?: "unidad"})"
                )
            }

            // caja del precio de mercado y el total estimado
            if (selected != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Store, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Precio promedio de mercado", style = MaterialTheme.typography.labelLarge, color = PalmPrimary)
                        }
                        Spacer(Modifier.height(6.dp))
                        // cuánto cuesta una unidad del material
                        Text(
                            "${Fmt.money(selected!!.avgPrice)} por ${selected!!.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PalmPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        // el total: precio por cantidad, pa' que no te pases de lanza
                        Text(
                            "Total estimado: ${Fmt.money(selected!!.avgPrice * quantity)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = PalmPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // tarjeta de la nota pa'l dueño
            AgroCard {
                Text("Detalle para el dueño", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // campo de texto pa' explicar pa' qué lo necesitas
                AgroField(
                    value = note,
                    onValueChange = { note = it },
                    label = "¿Para qué se necesita? (opcional)",
                    placeholder = "Ej: se acabó el abono en el lote 2",
                    singleLine = false,
                    minLines = 3
                )
            }

            // foto del material o la labor, va si quieres no más
            PhotoEvidence(
                photoPath = photoPath,
                onPhotoPath = { photoPath = it },
                label = "Foto del material o la labor (opcional)"
            )

            // caja roja del error si te faltó algo pendejo
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

            // el botón que manda la solicitud al dueño
            Button(
                onClick = {
                    val item = selected
                    // sin material no hay pedido wey
                    if (item == null) {
                        error = "Selecciona un material."
                    } else {
                        // mandamos el pedido completo al VM con todo y detalles
                        vm.requestMaterial(
                            parcelId = parcelId,
                            item = item.item,
                            quantity = quantity,
                            unit = item.unit,
                            note = note,
                            photoPath = photoPath
                        ) { result ->
                            // si viene null todo salió chido
                            if (result == null) {
                                Toast.makeText(context, "Solicitud enviada al dueño", Toast.LENGTH_SHORT).show()
                                // regresamos a la pantalla anterior
                                nav.popBackStack()
                            } else {
                                // si trae texto es que algo truino
                                error = result
                            }
                        }
                    }
                },
                // deshabilitado mientras envía pa' que no mandes doble
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                // el texto cambia cuando está enviando
                Text(if (vm.busy) "Enviando…" else "Enviar Solicitud al Dueño", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
