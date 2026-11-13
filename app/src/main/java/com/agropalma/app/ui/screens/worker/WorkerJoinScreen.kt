/*
 * WorkerJoinScreen.kt - UNIRSE CON CODIGO
 * ---------------------------------------
 * El trabajador escribe el codigo PALMA-XXXX que le da el dueno. La app
 * valida que exista y que no este vencido (1 minuto) y crea la membresia
 * que lo vincula a esa parcela.
 */

// el package, o sea la ubicación de esta pantalla en el proyecto
package com.agropalma.app.ui.screens.worker

// imports: Toast pa' los avisos, Compose pa' la UI y navegación, el combo completo
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// lo propio de AgroPalma: el ViewModel compartido y los componentes de la casa
import com.agropalma.app.AppViewModel
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary

// pantalla pa' unirse a una parcela con el código PALMA-XXXX que da el dueño
@Composable
fun WorkerJoinScreen(vm: AppViewModel, nav: NavController) {
    // aquí se guarda el código que va escribiendo el pelao
    var code by remember { mutableStateOf("") }
    // el mensaje de error si el código sale mal, arranca limpio
    var error by remember { mutableStateOf<String?>(null) }
    // contexto pa' poder lanzar el Toast con el resultado
    val context = LocalContext.current

    // esqueleto de la pantalla con su barra de arriba
    Scaffold(
        // fondo crema de siempre
        containerColor = PalmCream,
        // barra de arriba con título, subtítulo y botón pa' volver
        topBar = {
            AgroTopBar(
                "Unirme a una Parcela",
                "Código de invitación de la cuadrilla",
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna con scroll, por si la pantalla es chiquita que se pueda bajar
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            // espacio de 14 entre elementos
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // tarjeta principal donde va el formulario del código
            AgroCard {
                // título que le dice al pelao qué escribir
                Text("Escribe el código del dueño", style = MaterialTheme.typography.titleMedium)
                // explicación de que el código cambia cada minuto por seguridad
                Text(
                    "El dueño lo genera en su panel y cambia cada minuto por seguridad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                // espacio antes del campo de texto
                Spacer(Modifier.height(16.dp))
                // el campo donde escribe el código, con icono de escáner
                AgroField(
                    value = code,
                    // lo que escribe lo pone en MAYÚSCULAS, máx 14 caracteres, y borra el error anterior
                    onValueChange = { code = it.uppercase().take(14); error = null },
                    label = "Código de invitación",
                    placeholder = "PALMA-7842",
                    leadingIcon = Icons.Filled.QrCodeScanner
                )
                // espacio chiquito entre el campo y el aviso
                Spacer(Modifier.height(8.dp))
                // fila con el reloj y el aviso de que el código vence rapidito
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // iconito de reloj pa' lo del tiempo
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = PalmMuted, modifier = Modifier.size(15.dp))
                    // el aviso: si se venció, pídele otro al dueño
                    Text(
                        "  El código vence en 1 minuto. Si falla, pídele uno nuevo al dueño.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                }
                // espacio antes del botón
                Spacer(Modifier.height(16.dp))
                // el botón grande pa' vincularse a la parcela
                Button(
                    onClick = {
                        // manda el código al ViewModel, que valida que exista y que no haya vencido
                        vm.joinParcel(code) { result ->
                            // si salió bien y hay parcela, adelante
                            if (result.success && result.parcel != null) {
                                // aviso flotante de que quedó vinculado
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                // lo manda directo a la parcela y limpia la pila hasta el home
                                nav.navigate("worker/parcel/${result.parcel.id}") {
                                    popUpTo("worker/home")
                                }
                            } else {
                                // si falla, guarda el error pa' mostrarlo abajo
                                error = result.message
                            }
                        }
                    },
                    // el botón solo se activa si no está ocupado y el código tiene al menos 5 letras
                    enabled = !vm.busy && code.length >= 5,
                    // bien grandote, de lado a lado
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    // mientras valida dice "Validando…", si no, el texto normal
                    Text(if (vm.busy) "Validando…" else "Vincularme a esta parcela", fontWeight = FontWeight.SemiBold)
                }
            }

            // si hay error, sale esta cajita roja con el mensaje
            if (error != null) {
                // fondo rojito de error con esquinas redondeadas
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    // el mensaje del error, con !! porque ya sabemos que no es null
                    Text(
                        error!!,
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // tarjeta de "¿Cómo funciona?" con los pasos, pa' los despistados
            AgroCard {
                // fila del título con el icono de info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = PalmPrimary)
                    Spacer(Modifier.padding(5.dp))
                    Text("¿Cómo funciona?", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                // los 3 pasos numerados de cómo unirse, ahí nomás
                Step(1, "El dueño crea su grupo de parcela.")
                Step(2, "Te comparte el código PALMA-XXXX vigente.")
                Step(3, "Al vincularte podrás ver tus labores, reportar avances y pedir materiales.")
            }

            // cajita de abajo con el aviso de que cada parcela va por separado
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                // el texto centrado del aviso, pa' cerrar con broche de oro
                Text(
                    "Cada parcela es independiente: tu historial y tus pagos se manejan por separado.",
                    Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// una fila de paso: cuadrito verde con el número y el texto al lado
@Composable
private fun Step(number: Int, text: String) {
    // fila con poquito espacio vertical entre pasos
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        // cuadrito verde de 24 pa' meter el número del paso
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(8.dp),
            color = PalmPrimary
        ) {
            // el número centrado dentro del cuadrito, en fuente mono y negrita
            Text(
                number.toString(),
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // espacio entre el número y el texto
        Spacer(Modifier.size(10.dp))
        // el texto del paso en grisita
        Text(text, style = MaterialTheme.typography.bodyMedium, color = PalmMuted)
    }
}
