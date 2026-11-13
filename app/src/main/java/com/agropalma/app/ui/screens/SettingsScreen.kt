/*
 * SettingsScreen.kt - AJUSTES DE LA APP Y DE LA CUENTA
 * ---------------------------------------------------
 * La pantalla de configuración personal, pa' que el user no tenga que
 * rogarte por nada. Aquí se puede:
 *  - Cambiar el TEMA: modo claro u oscuro (se recuerda pa' siempre).
 *  - Editar los datos de la cuenta: nombre, correo y finca (si es dueño).
 *  - Cambiar la CONTRASEÑA (verificando la actual, no seas malandro).
 *  - BORRAR la cuenta con todo lo suyo (con doble confirmación, porque
 *    no hay vuelta atrás, ahí nomás vuela to').
 *
 * El celular y la cédula NO se editan porque son los identificadores
 * del login; cambiarlos sería un despelote, mejor así de sencillo.
 */

// el paquete de las pantallas, junto al login y el perfil
package com.agropalma.app.ui.screens

// layout: scroll, columnas, filas, espaciadores y tamaño
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
// iconos: tema, persona, correo, finca, candado, borrar y guardar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
// Material 3: botones, switch, diálogos, iconos y textos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// runtime de Compose pa' los estados con remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// alineación, modifiers y colores
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
// navegación y el rol del user
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Role
// componentes caseros: barra de arriba, tarjetas y campos
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroField
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.SectionTitle
// la paleta adaptativa (clara u oscura, el tema decide)
import com.agropalma.app.ui.theme.PalmError
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSuccess
import androidx.compose.ui.unit.dp

// la pantalla de ajustes: tema, datos de la cuenta, contraseña y el botón rojo
@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    // el user logueado: si por algo no hay, no pintamos nada raro
    val user = vm.currentUser ?: return
    // si es dueño mostramos el campo de la finca, si no, ni modo
    val isOwner = user.role == Role.OWNER

    // ── estado de los campos de datos personales ──
    // el remember va "amarrado" al user: cuando se actualiza el perfil,
    // los campos se refrescan solos con los datos nuevos
    var name by remember(vm.currentUser) { mutableStateOf(user.name) }
    var email by remember(vm.currentUser) { mutableStateOf(user.email ?: "") }
    var finca by remember(vm.currentUser) { mutableStateOf(user.finca ?: "") }
    // el mensajito de éxito o error del guardado del perfil
    var profileMsg by remember { mutableStateOf<String?>(null) }

    // ── estado del cambio de contraseña ──
    // la actual (pa' verificar que es él), la nueva y su repetición
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var repeatPass by remember { mutableStateOf("") }
    // el mensajito de éxito o error del cambio de clave
    var passMsg by remember { mutableStateOf<String?>(null) }
    // si el mensajito de la clave es de éxito o de error (pa' pintarlo)
    var passOk by remember { mutableStateOf(false) }

    // ── estado de la zona de peligro ──
    // bandera del diálogo de "seguro que borro to'""
    var showDelete by remember { mutableStateOf(false) }

    // andamio: fondo crema (o noche), barra de arriba con su flechita
    Scaffold(
        // el fondo se adapta al modo claro u oscuro solito
        containerColor = com.agropalma.app.ui.theme.PalmCream,
        // título "Ajustes" y el botón de volver al perfil
        topBar = { AgroTopBar("Ajustes", "Cuenta y apariencia", onBack = { nav.popBackStack() }) }
    ) { padding ->
        // columna scrolleable con TODO el contenido, bien ordenadito
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // título del bloque de apariencia, pa' que sepan qué es esto
            SectionTitle("Apariencia", "Elige cómo se ve AgroPalma")

            // tarjeta del modo oscuro: switch + explicación
            AgroCard {
                // fila: iconito del tema, textos y el switch a la derecha
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // el iconito: luna si está oscuro, sol si está claro
                    Icon(
                        if (vm.darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = null,
                        tint = PalmPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    // columna con el título y la descripción del modo
                    Column(Modifier.weight(1f)) {
                        // el nombre del modo actual, bien claro
                        Text(
                            if (vm.darkMode) "Modo oscuro" else "Modo claro",
                            style = MaterialTheme.typography.titleMedium
                        )
                        // la explicación de qué hace el cambio
                        Text(
                            "Cambia entre el look de día y el de noche. Se recuerda pa' la próxima vez que abras la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    // el switch que prende/apaga la noche, con sus colores
                    Switch(
                        checked = vm.darkMode,
                        onCheckedChange = { vm.applyDarkMode(it) },
                        // el switch pintado del verde palma cuando está activo
                        colors = SwitchDefaults.colors(checkedTrackColor = PalmPrimary)
                    )
                }
            }

            // título del bloque de datos de la cuenta
            SectionTitle("Datos de la cuenta", "Lo que pueden editar de su perfil")

            // tarjeta con el formulario de datos personales
            AgroCard {
                // el campo del nombre completo del user
                AgroField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre completo",
                    leadingIcon = Icons.Filled.Person
                )
                Spacer(Modifier.height(10.dp))
                // el correo (opcional, se puede dejar vacío)
                AgroField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Filled.Email,
                    placeholder = "opcional"
                )
                // la finca solo la puede editar el dueño, obvio
                if (isOwner) {
                    Spacer(Modifier.height(10.dp))
                    AgroField(
                        value = finca,
                        onValueChange = { finca = it },
                        label = "Nombre de la finca",
                        leadingIcon = Icons.Filled.Park,
                        placeholder = "opcional"
                    )
                }
                Spacer(Modifier.height(14.dp))
                // el botón verde de guardar los cambios del perfil
                Button(
                    onClick = {
                        // le tiramos los datos al ViewModel, él sabe qué hacer
                        vm.updateProfile(name, email, finca) { error ->
                            // si vino error lo mostramos, si no, éxito
                            profileMsg = error ?: "Datos actualizados, quedaron guardados."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    // el iconito del disquete y su texto
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Guardar cambios")
                }
                // si hay mensajito de éxito/error, lo pintamos bajo el botón
                profileMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    // en verde si salió bien, la verdad no hay caso de error acá
                    Text(it, style = MaterialTheme.typography.bodySmall, color = PalmSuccess)
                }
                // la letra chica: el celular y la cédula NO se tocan aquí
                Spacer(Modifier.height(10.dp))
                Text(
                    "El celular y la cédula no se editan porque son tus datos de entrada a la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
            }

            // título del bloque de seguridad, la puerta con candado
            SectionTitle("Seguridad", "Cambia tu contraseña de acceso")

            // tarjeta con el cambio de contraseña
            AgroCard {
                // la contraseña ACTUAL: pa' comprobar que es el dueño de la cuenta
                AgroField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = "Contraseña actual",
                    leadingIcon = Icons.Filled.Lock,
                    // teclado de contraseña y puntitos en vez de letras
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                // la contraseña nueva que quiere tener
                AgroField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = "Nueva contraseña",
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                // repetir la nueva, porque uno siempre la caga tecleando
                AgroField(
                    value = repeatPass,
                    onValueChange = { repeatPass = it },
                    label = "Repetir nueva contraseña",
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(14.dp))
                // el botón que dispara el cambio de clave
                Button(
                    onClick = {
                        // primero las validaciones de la pantalla, las fáciles
                        if (newPass != repeatPass) {
                            // no coinciden: ni pa' qué llamar al ViewModel
                            passMsg = "Las contraseñas nuevas no coinciden."
                            passOk = false
                            return@Button
                        }
                        // sí coinciden: que el ViewModel verifique la actual
                        vm.changePassword(currentPass, newPass) { error ->
                            // si hay error lo pintamos rojo, si no, verdecito
                            passMsg = error ?: "Contraseña cambiada, quedará pa' tu próximo ingreso."
                            passOk = error == null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Cambiar contraseña")
                }
                // el mensajito de resultado: verde si quedó, rojo si la cagó
                passMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (passOk) PalmSuccess else PalmError)
                }
            }

            // título de la zona de peligro, la del letrero amarillo y negro
            SectionTitle("Zona de peligro", "Aquí no hay botón de arrepentirse")

            // tarjeta con el borrado de la cuenta
            AgroCard {
                // explicación de lo que pasa si le da al botón rojo
                Text(
                    "Eliminar tu cuenta borra TODO y pa' siempre: tu perfil, tu parcela con sus labores (si eres dueño), " +
                        "y los reportes, materiales y diagnósticos que hayas hecho. Ni el profe podrá recuperar eso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(12.dp))
                // el botón rojo de la muerte, bien identificable
                Button(
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PalmError)
                ) {
                    // el iconito del basurero y su texto final
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Eliminar mi cuenta")
                }
            }

            // aire final pa' que el scroll no quede pegado al fondo
            Spacer(Modifier.height(10.dp))
        }
    }

    // el diálogo de confirmación de la eliminación, el de la doble mirada
    if (showDelete) {
        AlertDialog(
            // si lo cierra por fuera, se salva de milagro
            onDismissRequest = { showDelete = false },
            // título que da miedo sólo de leerlo
            title = { Text("¿Eliminar tu cuenta?") },
            // el texto que deja claro que NO hay vuelta atrás
            text = {
                Text("Se borrará tu cuenta y TODO su contenido de este dispositivo. Esta acción no se puede deshacer. ¿Estás segurísimo?")
            },
            // el botón de confirmar: rojo, como debe ser un botón de muerte
            confirmButton = {
                TextButton(onClick = {
                    // cerramos el diálogo primero
                    showDelete = false
                    // le decimos al ViewModel que vuele todo
                    vm.deleteAccount {
                        // y cuando termine, al login con el historial limpio
                        nav.navigate("login") {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                }) { Text("Sí, eliminar todo", color = PalmError, fontWeight = FontWeight.Bold) }
            },
            // el botón de arrepentirse y seguir viviendo
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancelar") }
            },
            // el fondo del diálogo se adapta al modo claro/oscuro
            containerColor = com.agropalma.app.ui.theme.PalmSurface
        )
    }
}
