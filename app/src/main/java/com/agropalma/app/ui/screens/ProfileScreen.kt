/*
 * ProfileScreen.kt - PERFIL DEL USUARIO
 * -------------------------------------
 * Muestra los datos de la cuenta (celular, cedula, correo, finca), la
 * parcela activa si es dueno y la version de la app. Incluye el boton
 * para cerrar sesion con confirmacion.
 */

// paquete donde vive esta pantalla, el perfil del user
package com.agropalma.app.ui.screens

// imports de layout: cajas, columnas, filas y scroll pa' la pantalla
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
// iconos: cédula, correo, salir, palma, persona, teléfono y ajustes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
// Material 3: diálogo pa' cerrar sesión, botones y textos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// runtime de Compose pa' estados con remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// alineación, modifiers y grosor de letra
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// navegación, ViewModel y el rol pa' saber si es dueño o trabajador
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Role
// componentes reutilizables: barras, tarjetas, divisores y filas de info
import com.agropalma.app.ui.components.AgroBottomBar
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.HDivider
import com.agropalma.app.ui.components.LabelValue
// items y navegación de las barras de abajo de cada rol
import com.agropalma.app.ui.screens.owner.ownerBottomItems
import com.agropalma.app.ui.screens.owner.ownerNavigate
import com.agropalma.app.ui.screens.worker.workerBottomItems
import com.agropalma.app.ui.screens.worker.workerNavigate
// paleta: crema, rojo de error, tenue, verde primario y superficie
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmError
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSurface

// pantalla del perfil, donde el user ve sus datos y puede cerrar sesión
@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavController) {
    // el usuario logueado, sacado del ViewModel compartido
    val user = vm.currentUser
    // bandera pa' saber si ya le dio al botón de cerrar sesión
    var showLogout by remember { mutableStateOf(false) }
    // rápida verificación: ¿es el dueño o es un trabajador?
    val isOwner = user?.role == Role.OWNER

    // scaffold con fondo crema, barra de arriba y la barra de navegación de abajo
    Scaffold(
        containerColor = PalmCream,
        // el subtítulo cambia según el rol del user
        topBar = { AgroTopBar("Mi Perfil", if (isOwner) "Dueño / Administrador" else "Trabajador de cuadrilla") },
        // barra de abajo, cada rol tiene la suya
        bottomBar = {
            // si es dueño le ponemos su barrita con sus opciones
            if (isOwner) {
                AgroBottomBar(ownerBottomItems, selected = 3) { index ->
                    // según el toque lo mandamos a la pantalla del dueño
                    when (index) {
                        0 -> ownerNavigate(nav, "owner/home")
                        1 -> ownerNavigate(nav, "owner/reports")
                        2 -> ownerNavigate(nav, "owner/costs")
                    }
                }
            } else {
                // si es trabajador, su barrita más corta
                AgroBottomBar(workerBottomItems, selected = 3) { index ->
                    // navegación del trabajador: home o unirse a parcela
                    when (index) {
                        0 -> workerNavigate(nav, "worker/home")
                        1 -> workerNavigate(nav, "worker/join")
                    }
                }
            }
        }
    ) { padding ->
        // columna scrolleable con todo el contenido del perfil
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // encabezado del perfil: avatar, nombre y chapita de rol, todo centrado
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // cajita redondeada del avatar, como un círculo cuadrado bonito
                Box(
                    Modifier
                        .size(82.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // iniciales del user: agarra las primeras 2 palabras y su primera letra
                    Text(
                        user?.name?.trim()?.split(" ")?.filter { it.isNotBlank() }?.take(2)
                            ?.joinToString("") { it.first().uppercase() } ?: "AP",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PalmPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                // nombre completo del user (o "Usuario" si por algo no hay)
                Text(user?.name ?: "Usuario", style = MaterialTheme.typography.headlineSmall)
                // chapita redondita con el rol, pillona de 999dp pa' que quede pastilla
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isOwner) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    // el texto de la chapita según el rol
                    Text(
                        if (isOwner) "Dueño de parcela" else "Trabajador",
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = PalmPrimary
                    )
                }
            }

            // tarjeta con los datos de la cuenta, el DNI del user
            AgroCard {
                // título de la sección
                Text("Datos de la cuenta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                // su celular, guion si no hay nada
                InfoRow(Icons.Filled.Phone, "Celular", user?.phone ?: "—")
                Spacer(Modifier.height(8.dp))
                HDivider()
                Spacer(Modifier.height(8.dp))
                // la cédula, si es dueño dice que no aplica
                InfoRow(Icons.Filled.Badge, "Cédula", user?.cedula ?: "No aplica")
                Spacer(Modifier.height(8.dp))
                HDivider()
                Spacer(Modifier.height(8.dp))
                // su correo registrado
                InfoRow(Icons.Filled.Email, "Correo", user?.email ?: "No registrado")
                // la fila de la finca solo sale si es el dueño
                if (isOwner) {
                    Spacer(Modifier.height(8.dp))
                    HDivider()
                    Spacer(Modifier.height(8.dp))
                    // nombre de la finca (o la parcela activa si no tiene)
                    InfoRow(Icons.Filled.Park, "Finca", user?.finca ?: vm.ownerParcel?.name ?: "—")
                }
            }

            // tarjeta de la parcela activa, solo pa' dueños que ya crearon una
            if (isOwner && vm.ownerParcel != null) {
                AgroCard {
                    // título de la sección de la parcela
                    Text("Parcela activa", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    // nombre de la parcela
                    LabelValue("Nombre", vm.ownerParcel!!.name)
                    Spacer(Modifier.height(6.dp))
                    // dónde queda la parcela
                    LabelValue("Ubicación", vm.ownerParcel!!.location)
                    Spacer(Modifier.height(6.dp))
                    // hectáreas formateadas con el util Fmt pa' que se vea bonito
                    LabelValue("Extensión", "${com.agropalma.app.util.Fmt.number(vm.ownerParcel!!.hectares)} ha")
                    Spacer(Modifier.height(6.dp))
                    // cuántos trabajadores tiene contratados
                    LabelValue("Trabajadores", vm.ownerWorkers.size.toString())
                }
            }

            // tarjeta "acerca de", la info de la app y su versión
            AgroCard {
                // título de la sección
                Text("Acerca de AgroPalma", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                // texto largo sobre la versión y que todo se guarda local
                Text(
                    "Versión 1.0 • Datos guardados de forma local en este dispositivo. " +
                        "La sincronización en la nube se puede activar más adelante sin cambiar las pantallas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
            }

            // botón con borde pa' los ajustes: tema, datos, contraseña y borrar cuenta
            OutlinedButton(
                onClick = { nav.navigate("settings") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PalmPrimary)
            ) {
                // el iconito de la llave inglesa, el clásico de ajustes
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                // texto del botón que lleva a la pantalla de ajustes
                Text("Ajustes de cuenta")
            }
            // botón rojo de cerrar sesión, el que nadie toca por accidente... o sí
            Button(
                onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PalmError)
            ) {
                // iconito de salir
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                // texto del botón que abre el diálogo
                Text("Cerrar sesión")
            }
            // espacio final
            Spacer(Modifier.height(8.dp))
        }
    }

    // diálogo de confirmación pa' cerrar sesión, no sea que lo toque sin querer
    if (showLogout) {
        // el AlertDialog con su título, pregunta y botones
        AlertDialog(
            // si lo cierra por fuera se cancela nomás
            onDismissRequest = { showLogout = false },
            title = { Text("Cerrar sesión") },
            // la pregunta de si está seguro
            text = { Text("¿Seguro que quieres salir de tu cuenta en este teléfono?") },
            // botón de confirmar, el rojo de "Sí, salir"
            confirmButton = {
                TextButton(onClick = {
                    // cerramos el diálogo primero
                    showLogout = false
                    // le decimos al ViewModel que cierre la sesión
                    vm.logout()
                    // y lo mandamos al login, borrando todo el historial de pantallas
                    nav.navigate("login") {
                        popUpTo(nav.graph.id) { inclusive = true }
                    }
                }) { Text("Sí, salir", color = PalmError) }
            },
            // botón de arrepentirse y quedarse
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Cancelar") }
            },
            containerColor = PalmSurface
        )
    }
}

// filita reutilizable pa' mostrar icono + etiqueta + valor, como en los datos
@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    // fila centrada con icono, etiqueta tenue y valor en negritas
    Row(verticalAlignment = Alignment.CenterVertically) {
        // iconito de la fila
        Icon(icon, contentDescription = null, tint = PalmMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        // la etiqueta (ej. "Celular") se estira pa' empujar el valor a la derecha
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PalmMuted, modifier = Modifier.weight(1f))
        // el valor en negritas pa' que resalte
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
