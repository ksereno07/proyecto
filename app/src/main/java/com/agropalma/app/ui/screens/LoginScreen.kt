/*
 * LoginScreen.kt - INICIO DE SESION
 * ---------------------------------
 * Primera pantalla de la app. Permite entrar con celular o cedula y
 * contrasena, elegir entre perfil Trabajador o Dueno, mostrar/ocultar la
 * contrasena y navegar al registro. Si el login es correcto, el
 * ViewModel carga los datos y se abre el panel del rol.
 */

// paquete de la pantalla de login, la primera que ve todo mundo al abrir la app
package com.agropalma.app.ui.screens

// imports de layout pa' armar cajas, columnas, filas y espaciados
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// iconos: la palmita del logo, la cedula, el candado y los ojitos de la contrasena
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
// Material 3: botones, tarjetas, iconos y textos, toda la pinta de la app
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// runtime de Compose: remember y estados pa' que la pantalla reaccione al toque
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// alineacion y modifiers pa' acomodar y centrar las cosas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// degradados, colores y transformaciones pa' tapar la contrasena con puntitos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// navegacion, el ViewModel compartido y el rol (dueno o trabajador)
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Role
// campo de texto reutilizable de la app y su paleta de colores palmeros
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmPrimaryDark
import com.agropalma.app.ui.theme.PalmSecondary
import com.agropalma.app.ui.theme.PalmSurface

// la pantalla de login, la puerta de entrada a toda la app
@Composable
fun LoginScreen(vm: AppViewModel, nav: NavController) {
    // bandera pa' saber si esta activa la pestana de dueno
    var ownerMode by remember { mutableStateOf(false) }
    // lo que teclea el user: su celular (o cedula si es trabajador)
    var identifier by remember { mutableStateOf("") }
    // la contrasena, aqui se queda guardadita mientras la escribe
    var password by remember { mutableStateOf("") }
    // bandera del ojito: mostrar la contrasena o taparla con puntitos
    var showPassword by remember { mutableStateOf(false) }
    // aqui guardamos el mensajito de error si el login truena
    var error by remember { mutableStateOf<String?>(null) }

    // fondo crema que llena toda la pantalla, ahí nomás pa' que se vea bonito
    Box(Modifier.fillMaxSize().background(PalmCream)) {
        // columna con scroll por si la pantalla es chiquita o está girada
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // el header de arriba: cajón con degradado verde palmero
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        Brush.verticalGradient(listOf(PalmPrimaryDark, PalmPrimary, PalmSecondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                // todo lo del logo va centrado pa' que se vea pro
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // cajita blanca translúcida donde va la palmita del logo
                    Box(
                        Modifier
                            .size(74.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // el icono de palma, o sea el logo de AgroPalma
                        Icon(
                            Icons.Filled.Park,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    // espacio pa' que el logo no quede pegado al nombre
                    Spacer(Modifier.height(12.dp))
                    // nombre de la app en grande, blanco y en negritas
                    Text(
                        "AgroPalma",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    // el eslogan, bien chido por cierto
                    Text(
                        "Tu parcela, tu gente, tus cuentas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // tarjeta del formulario, montada un cacho arriba sobre el degradado
            Card(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .offset(y = (-34).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PalmSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // adentro de la tarjeta, con padding pa' que no quede apretado
                Column(Modifier.padding(20.dp)) {
                    // título del formulario de inicio de sesión
                    Text("Iniciar Sesión", style = MaterialTheme.typography.headlineSmall)
                    // subtítulo que dice con qué puedes entrar
                    Text(
                        "Entra con tu celular o cédula",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                    Spacer(Modifier.height(16.dp))

                    // fila con las dos pestañas pa' elegir tu rol
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // pestaña de trabajador, al tocarla apagamos el modo dueño
                        RoleTab("Soy Trabajador", !ownerMode, Modifier.weight(1f)) {
                            ownerMode = false
                            error = null
                        }
                        // pestaña de dueño, prende el modo dueño y limpia errores
                        RoleTab("Soy Dueño", ownerMode, Modifier.weight(1f)) {
                            ownerMode = true
                            error = null
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // campo del celular o cédula; el dueño solo entra con celular, wey
                    AgroField(
                        value = identifier,
                        onValueChange = { identifier = it; error = null },
                        label = if (ownerMode) "Número de Celular" else "Número de Celular o Cédula",
                        leadingIcon = Icons.Filled.Badge,
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(Modifier.height(12.dp))
                    // campo de la contraseña con su candadito y todo
                    AgroField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = "Contraseña",
                        leadingIcon = Icons.Filled.Lock,
                        keyboardType = KeyboardType.Password,
                        // si el ojito está activo se ve el texto, si no salen puntitos
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        // botón del ojito pa' mostrar u ocultar la contraseña
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                // el icono cambia según si la contraseña está a la vista o no
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                                    tint = PalmMuted
                                )
                            }
                        }
                    )

                    // si el login truena le avisamos al user con este mensajito rojo
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        // cajita roja de error pa' que se note feo pero bonito
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            // aquí va el texto del error que mandó el ViewModel
                            Text(
                                error!!,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    // botón gordo verde pa' entrar a la cuenta
                    Button(
                        onClick = {
                            // le tiramos celular/cédula y contraseña al ViewModel, él hace el resto
                            vm.login(identifier, password) { result ->
                                // si result viene null es que sí entró bien, yay
                                if (result == null) {
                                    // según el rol lo mandamos al panel del dueño o del trabajador
                                    val route = if (vm.currentUser?.role == Role.OWNER) "owner/home" else "worker/home"
                                    // navegamos y borramos el login del historial pa' que no regrese con atrás
                                    nav.navigate(route) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    // si truena, cachamos el mensajito de error
                                    error = result
                                }
                            }
                        },
                        // mientras carga el botón se apaga pa' evitar dobles toques
                        enabled = !vm.busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        // si está ocupado dice "Entrando…", si no el texto normal
                        Text(if (vm.busy) "Entrando…" else "Entrar a mi Cuenta", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(8.dp))
                    // preguntita por si el user aún no tiene cuenta
                    Text(
                        "¿No tienes cuenta?",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                    // fila con los dos links pa' registrarse, uno pa' cada rol
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // botón pa' irse al registro de trabajador
                        TextButton(onClick = { nav.navigate("register/worker") }) {
                            Text("Registrarme como Trabajador")
                        }
                        // botón pa' irse al registro de dueño
                        TextButton(onClick = { nav.navigate("register/owner") }) {
                            Text("Soy Dueño")
                        }
                    }
                }
            }
            // espacio final pa' que la tarjeta no quede pegada hasta abajo
            Spacer(Modifier.height(20.dp))
        }
    }
}

// pestañita reutilizable pa' elegir entre trabajador y dueño
@Composable
private fun RoleTab(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // superficie que se ve como botón: verde si está seleccionada, gris si no
    Surface(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) PalmPrimary else MaterialTheme.colorScheme.surfaceVariant,
        // si no está seleccionada le ponemos un borde tenue pa' que no se pierda
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        // cajita centradora pa' que el texto quede enmedio del botón
        Box(contentAlignment = Alignment.Center) {
            // el texto de la pestaña: blanco si está activa, gris si no
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else PalmMuted
            )
        }
    }
}
