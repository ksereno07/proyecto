/*
 * CreateParcelScreen.kt - CREAR GRUPO DE PARCELA
 * ----------------------------------------------
 * El dueno escribe nombre, ubicacion y hectareas. Al guardar se crea la
 * parcela en SQLite, se genera un codigo PALMA-XXXX y se insertan cuatro
 * labores base (poda, cosecha, plateo y aplicacion fitosanitaria) con
 * tarifas por unidad.
 */

// el package, o sea la dirección del archivo dentro del proyecto
package com.agropalma.app.ui.screens.owner

// imports de Compose: layouts, material 3, estado y navegación pa' el formulario
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
// imports nuestros: el ViewModel compartido, componentes y colores de AgroPalma
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary

// la pantalla del formulario pa' crear el grupo de parcela
@Composable
fun CreateParcelScreen(vm: AppViewModel, nav: NavController) {
    // El nombre de la parcela se prellena con la finca que el dueño puso
    // al registrarse, para que no tenga que escribirla dos veces.
    var name by remember { mutableStateOf(vm.currentUser?.finca ?: "") }
    // la ubicación o municipio, empieza vacía
    var location by remember { mutableStateOf("") }
    // las hectáreas de la parcela, también vacías al inicio
    var hectares by remember { mutableStateOf("") }
    // aquí guardamos el mensaje de error si el user la caga en el formulario
    var error by remember { mutableStateOf<String?>(null) }

    // el Scaffold con fondo crema y su barra de arriba
    Scaffold(
        // fondo crema pa' que se vea fresco
        containerColor = PalmCream,
        topBar = {
            // la barra superior con título, subtítulo y botón de atrás
            AgroTopBar(
                title = "Crear Grupo de Parcela",
                subtitle = "Paso 1 de 2 • Información general",
                // pa' volver nomás retrocedemos en el backstack
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna scrolleable, por si el teclado tapa los campos wey
        Column(
            Modifier
                .fillMaxSize()
                // respetamos el padding del Scaffold
                .padding(padding)
                // y activamos el scroll vertical
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            // espacio entre tarjetas pa' que respire
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // primera tarjeta: el formulario con los datos de la parcela
            AgroCard {
                // el título de la sección
                Text("Datos de la parcela", style = MaterialTheme.typography.titleMedium)
                // la explicación de pa' qué sirven estos datos
                Text(
                    "Con estos datos se crea el grupo y el código que compartirás con tu cuadrilla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(12.dp))
                // campo del nombre (viene pre-llenado con la finca)
                AgroField(
                    value = name,
                    // al escribir limpiamos el error pa' no espantar al user
                    onValueChange = { name = it; error = null },
                    label = "Nombre de la parcela o finca *",
                    leadingIcon = Icons.Filled.Park
                )
                Spacer(Modifier.height(10.dp))
                // campo de la ubicación o municipio
                AgroField(
                    value = location,
                    onValueChange = { location = it; error = null },
                    label = "Ubicación o municipio *",
                    leadingIcon = Icons.Filled.LocationOn
                )
                Spacer(Modifier.height(10.dp))
                // campo de las hectáreas, numérico con decimales
                AgroField(
                    value = hectares,
                    onValueChange = { hectares = it; error = null },
                    label = "Extensión en hectáreas *",
                    leadingIcon = Icons.Filled.Straighten,
                    // teclado decimal, pa' que escriba 42.5 si quiere
                    keyboardType = KeyboardType.Decimal,
                    // el placeholder de ejemplo
                    placeholder = "42"
                )
            }

            // segunda tarjeta: los avisos de lo que pasa al crear la parcela
            AgroCard {
                // primera fila: el aviso de las 4 labores base
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // iconito del grupo
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = PalmPrimary)
                    Spacer(Modifier.padding(5.dp))
                    Column {
                        // el título del aviso
                        Text("Se crearán 4 labores base", style = MaterialTheme.typography.titleMedium)
                        // y la lista de labores que se crean solas
                        Text(
                            "Poda sanitaria, cosecha de racimos, plateo y aplicación fitosanitaria. Podrás ajustar las tarifas después.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // segunda fila: el aviso del código dinámico
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // iconito del QR
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = PalmPrimary)
                    Spacer(Modifier.padding(5.dp))
                    Column {
                        // el título del aviso
                        Text("Código dinámico de 1 minuto", style = MaterialTheme.typography.titleMedium)
                        // cómo funciona el código pa' invitar a la cuadrilla
                        Text(
                            "El dueño comparte el código y el trabajador lo usa para unirse a la parcela.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                    }
                }
            }

            // si hay error lo pintamos en rojo pa' que el user sepa que la cagó
            if (error != null) {
                // superficie con el color de error del tema
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    // el mensaje de error con su padding
                    Text(
                        error!!,
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // el botón grande pa' crear la parcela y arrancar todo
            Button(
                onClick = {
                    // le pasamos los 3 datos al ViewModel: guarda en SQLite, crea las 4 labores y el primer código
                    vm.createParcel(name, location, hectares) { result ->
                        // si result es null todo salió chido
                        if (result == null) {
                            // regresamos a la pantalla anterior, ya con parcela
                            nav.popBackStack()
                        } else {
                            // si trae texto es el error, pa' mostrarlo arriba
                            error = result
                        }
                    }
                },
                // mientras crea desactivamos el botón, no la cagues con doble click
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                // el texto cambia según si está ocupado creando o no
                Text(if (vm.busy) "Creando parcela…" else "Crear Grupo y Activar Cuadrilla", fontWeight = FontWeight.SemiBold)
            }
            // botón outline pa' cancelar y volver sin crear nada
            OutlinedButton(
                onClick = { nav.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancelar y volver")
            }
        }
    }
}
