/*
 * RegisterWorkerScreen.kt - REGISTRO DEL TRABAJADOR
 * -------------------------------------------------
 * Formulario con nombre, cedula, celular, correo opcional y contrasena.
 * La cedula es obligatoria porque se usa para la planilla y los pagos.
 * Al crear la cuenta, el trabajador entra directo a su panel para unirse
 * a una parcela con el codigo del dueno.
 */

// paquete de esta pantalla, el registro del trabajador de campo
package com.agropalma.app.ui.screens

// imports de layout pa' armar columnas, filas y darle scroll al formulario
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
// iconos del formulario: persona, cédula, teléfono, correo, candado y ojitos
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
// Material 3: botón, checkbox, scaffold y textos, la pintura de la pantalla
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// runtime de Compose pa' los estados con remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// alineación, modifiers y cositas de teclado/contraseña oculta
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
// navegación y el ViewModel compartido de toda la app
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
// componentes reutilizables de AgroPalma: tarjeta, campo y barra de arriba
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
// la paleta crema y el color tenue pa' textos secundarios
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted

// pantalla pa' crear la cuenta del trabajador, la que llena el compa nuevo
@Composable
fun RegisterWorkerScreen(vm: AppViewModel, nav: NavController) {
    // nombre completo del compa, sin apodos wey
    var name by remember { mutableStateOf("") }
    // la cédula es OBLIGATORIA aquí, pa' la planilla y los pagos
    var cedula by remember { mutableStateOf("") }
    // número de celular o WhatsApp del trabajador
    var phone by remember { mutableStateOf("") }
    // correo, este sí es opcional, ahí nomás si quiere
    var email by remember { mutableStateOf("") }
    // contraseña del compa (el ViewModel la cifra, no la cagues mandándola pelada)
    var password by remember { mutableStateOf("") }
    // bandera del ojito pa' mostrar u ocultar la contraseña
    var showPassword by remember { mutableStateOf(false) }
    // checkbox de aceptar el tratamiento de datos
    var accepted by remember { mutableStateOf(false) }
    // mensajito de error si algo sale mal en el registro
    var error by remember { mutableStateOf<String?>(null) }

    // scaffold con fondo crema y su barra de arriba
    Scaffold(
        containerColor = PalmCream,
        // barra superior con título y flechita pa' volver
        topBar = {
            AgroTopBar(
                title = "Cuenta de Trabajador",
                subtitle = "Tus datos para liquidar jornales",
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna scrolleable pa' que quepa todo el formulario en pantallas chiquitas
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // tarjeta con los datos personales del compa
            AgroCard {
                // título de la sección
                Text("Datos personales", style = MaterialTheme.typography.titleMedium)
                // aclaración de pa' qué sirve la cédula, pa' que nadie chille después
                Text(
                    "El dueño usará tu cédula para los comprobantes de pago y la planilla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(12.dp))
                // campo del nombre completo, con su iconito de persona
                AgroField(name, { name = it; error = null }, "Nombre y apellidos completos *", leadingIcon = Icons.Filled.Person)
                Spacer(Modifier.height(10.dp))
                // campo de la cédula, solo números y obligatoria pa' el trabajador
                AgroField(
                    cedula, { cedula = it; error = null },
                    "Cédula de ciudadanía *",
                    leadingIcon = Icons.Filled.Badge,
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(10.dp))
                // campo del celular/WhatsApp con su placeholder de ejemplo
                AgroField(
                    phone, { phone = it; error = null },
                    "Número de celular / WhatsApp *",
                    leadingIcon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                    placeholder = "+57 300 000 0000"
                )
                Spacer(Modifier.height(10.dp))
                // campo del correo, este sí es opcional (por eso no borra el error)
                AgroField(
                    email, { email = it },
                    "Correo electrónico (opcional)",
                    leadingIcon = Icons.Filled.Email,
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(10.dp))
                // campo pa' crear la contraseña, mínimo 4 caracteres o truena
                AgroField(
                    password, { password = it; error = null },
                    "Crear contraseña *",
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    // puntitos si está oculta, texto pelón si la muestra
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    // botón del ojito pa' mostrar u ocultar
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            // icono que cambia según si se ve o no la contraseña
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Mostrar contraseña",
                                tint = PalmMuted
                            )
                        }
                    }
                )
            }

            // tarjeta del checkbox de consentimiento, el trámite legal wey
            AgroCard {
                // fila con el checkbox y su texto ladillado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // palomita de aceptar, si no la marcas no te deja crear la cuenta
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                    // el texto legal que nadie lee pero hay que aceptar
                    Text(
                        "Acepto el tratamiento de mis datos para el registro de labores y la liquidación de pagos.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // si algo salió mal (campos vacíos, cédula repetida...) sale este mensajito rojo
            if (error != null) {
                // cajita roja de error bien visible
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    // el texto del error que mandó el ViewModel
                    Text(
                        error!!,
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // botón gordo pa' crear la cuenta del trabajador
            Button(
                onClick = {
                    // primero checa que haya aceptado el tratamiento de datos
                    if (!accepted) {
                        error = "Debes aceptar el tratamiento de datos."
                    } else {
                        // le tiramos todo al ViewModel, él valida y cifra la contraseña
                        vm.registerWorker(name, cedula, phone, email, password) { result ->
                            // si result es null, cuenta creada, bien
                            if (result == null) {
                                // directo al panel del trabajador, borrando el login del historial
                                nav.navigate("worker/home") { popUpTo("login") { inclusive = true } }
                            } else {
                                // si truena (cédula duplicada, campos vacíos...) mostramos el error
                                error = result
                            }
                        }
                    }
                },
                // desactivado mientras carga pa' que no le den cien veces
                enabled = !vm.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                // el texto cambia si está creando la cuenta o no
                Text(if (vm.busy) "Creando cuenta…" else "Crear Cuenta de Trabajador", fontWeight = FontWeight.SemiBold)
            }
            // espacio final abajo del botón
            Spacer(Modifier.height(8.dp))
        }
    }
}
