/*
 * RegisterOwnerScreen.kt - REGISTRO DEL DUENO
 * -------------------------------------------
 * Formulario con nombre, celular, correo, contrasena y nombre de la finca.
 * Al crear la cuenta se abre el panel del dueno, donde podra crear el
 * grupo de parcela y compartir el codigo con su cuadrilla.
 */

// paquete de esta pantalla, la del registro del dueño de la finca
package com.agropalma.app.ui.screens

// imports de layout: columnas, filas, espaciadores y scroll
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
// iconos: persona, correo, candado, palma (finca), teléfono y ojitos
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
// Material 3: botón, checkbox, scaffold, textos, lo de siempre
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// runtime de Compose pa' guardar estados con remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// alineación, modifiers y transformaciones pa' ocultar la contraseña
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
// navegación, ViewModel compartido y componentes reutilizables de la app
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
// colores crema y tenue pa' la pinta de la pantalla
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted

// pantalla pa' crear la cuenta del dueño, el jefe de la parcela
@Composable
fun RegisterOwnerScreen(vm: AppViewModel, nav: NavController) {
    // nombre del dueño (o razón social si es empresa)
    var name by remember { mutableStateOf("") }
    // celular del dueño, con este va a entrar siempre al login
    var phone by remember { mutableStateOf("") }
    // correo del dueño, aquí sí es obligatorio wey
    var email by remember { mutableStateOf("") }
    // contraseña maestra (el ViewModel la cifra con Security.hash, ojo)
    var password by remember { mutableStateOf("") }
    // nombre de la finca o parcela, lo que lo distingue de los demás
    var finca by remember { mutableStateOf("") }
    // ojito pa' mostrar u ocultar la contraseña
    var showPassword by remember { mutableStateOf(false) }
    // checkbox de aceptar términos, sin esto no deja crear nada
    var accepted by remember { mutableStateOf(false) }
    // errorcito rojo si algo sale mal en el registro
    var error by remember { mutableStateOf<String?>(null) }

    // scaffold crema con su barra de arriba bien puesta
    Scaffold(
        containerColor = PalmCream,
        // barra superior con título y flechita pa' volver
        topBar = {
            AgroTopBar(
                title = "Cuenta de Dueño",
                subtitle = "Administra tu parcela y tu cuadrilla",
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna scrolleable con todo el formulario del dueño
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // tarjeta con los datos del dueño
            AgroCard {
                // título de la sección
                Text("Datos del propietario", style = MaterialTheme.typography.titleMedium)
                // le explica pa' qué sirve la cuenta, por el código de la cuadrilla
                Text(
                    "Crea tu cuenta para generar el código de acceso de tu cuadrilla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(12.dp))
                // campo del nombre del dueño o su empresa
                AgroField(name, { name = it; error = null }, "Nombre completo o razón social *", leadingIcon = Icons.Filled.Person)
                Spacer(Modifier.height(10.dp))
                // campo del celular del dueño, su usuario pa' entrar siempre
                AgroField(
                    phone, { phone = it; error = null },
                    "Número de celular / WhatsApp *",
                    leadingIcon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                    placeholder = "+57 300 000 0000"
                )
                Spacer(Modifier.height(10.dp))
                // campo del correo, pa' el dueño este sí es obligatorio
                AgroField(
                    email, { email = it; error = null },
                    "Correo electrónico *",
                    leadingIcon = Icons.Filled.Email,
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(10.dp))
                // campo de la contraseña maestra, la del jefe
                AgroField(
                    password, { password = it; error = null },
                    "Crear contraseña maestra *",
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    // puntitos si está oculta, texto normal si la muestra
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    // botón del ojito pa' verla o taparla
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            // icono que cambia según el estado
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Mostrar contraseña",
                                tint = PalmMuted
                            )
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                // campo del nombre de la finca, la cereza del pastel del dueño
                AgroField(
                    finca, { finca = it; error = null },
                    "Nombre de tu parcela o finca *",
                    leadingIcon = Icons.Filled.Park
                )
            }

            // tarjeta del checkbox de términos, la parte aburrida pero necesaria
            AgroCard {
                // fila con checkbox y texto legal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // palomita de aceptar, obligatoria pa' poder crear la cuenta
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                    // el texto de términos que nadie se lee completo
                    Text(
                        "Acepto los términos del servicio agrícola y la política de tratamiento de datos.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // si algo truena (celular duplicado, campos vacíos...) sale esto en rojo
            if (error != null) {
                // cajita roja de error
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    // texto del error que regresó el ViewModel
                    Text(
                        error!!,
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // botón gordo pa' crear la cuenta y abrir la parcela
            Button(
                onClick = {
                    // si no aceptó los términos le damos el mensajito y ya
                    if (!accepted) {
                        error = "Debes aceptar los términos para continuar."
                    } else {
                        // le mandamos todo al ViewModel, él valida y cifra la contraseña
                        vm.registerOwner(name, phone, email, password, finca) { result ->
                            // null = cuenta creada, sin errores
                            if (result == null) {
                                // lo mandamos a su panel de dueño, borrando el login del historial
                                nav.navigate("owner/home") { popUpTo("login") { inclusive = true } }
                            } else {
                                // si no, mostramos en qué la cagó el user
                                error = result
                            }
                        }
                    }
                },
                // apagado mientras carga pa' evitar dobles registros
                enabled = !vm.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                // el texto cambia mientras está creando la cuenta
                Text(if (vm.busy) "Creando cuenta…" else "Crear Cuenta y Abrir Parcela", fontWeight = FontWeight.SemiBold)
            }
            // espacio final abajo
            Spacer(Modifier.height(8.dp))
        }
    }
}
