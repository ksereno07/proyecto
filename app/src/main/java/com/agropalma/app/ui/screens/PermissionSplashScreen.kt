/*
 * PermissionSplashScreen.kt - PORTÓN DE PERMISOS AL ARRANCAR
 * ----------------------------------------------------------
 * Esta es la primera pantalla que ves al abrir AgroPalma. Antes de dejarte
 * entrar a la app te pide los 3 permisos que necesita para trabajar de una:
 *  - Ubicación: pa' pegarle el GPS a los reportes de trabajo.
 *  - Cámara: pa' la foto de evidencia y el escáner de palma.
 *  - Notificaciones: pa' avisarte cuando no hay internet y cuándo se envían
 *    los registros que estaban en la cola esperando señal.
 * Si ya diste todos los permisos antes, esta pantalla se salta SOLA (ni la
 * ves). Y si no querés darlos todavía, podés entrar igual con "Continuar
 * sin permisos" — la app te los vuelve a pedir justo cuando los necesite.
 */

// el package de las pantallas, junto al login y todas las demás
package com.agropalma.app.ui.screens

// imports de Android: los permisos y la versión del sistema operativo
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
// el lanzador de permisos de la Activity API (el que pide varios de un golpe)
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// imports de Compose: fondos, layout y scroll
import androidx.compose.foundation.background
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
// imports de Material 3: botones, tarjeta, iconos y textos
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// runtime de Compose: el composable, el efecto y el contexto local
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
// lo visual: alineación, modificador, degradado, color y negritas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// el chequeador de permisos de la librería de compatibilidad de AndroidX
import androidx.core.content.ContextCompat
// los colores palma de siempre, los de todo el look de la app
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmPrimaryDark
import com.agropalma.app.ui.theme.PalmSecondary
import com.agropalma.app.ui.theme.PalmSurface

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: PANTALLA DE PERMISOS
//  Propósito: pedir ubicación, cámara y notificaciones ANTES de entrar
//  a la app, pa' que el usuario no ande dando permisos a media jornada.
//  Si ya los dio todos, se brinca esta pantalla automáticamente.
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PermissionSplashScreen(onListo: () -> Unit) {
    // el context de Android, pa' preguntarle al sistema por los permisos
    val context = LocalContext.current

    // la lista completa de permisos que la app necesita: los dos de
    // ubicación (preciso y aproximado), la cámara y, SOLO si el teléfono
    // es Android 13+, las notificaciones (los viejos ni las preguntan)
    val permisos = buildList {
        // GPS fino y GPS braceado, los dos juntos como manda la documentación
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        // la cámara pa' las fotos de evidencia y el escáner de palma
        add(Manifest.permission.CAMERA)
        // las notificaciones solo se piden desde Android 13 (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // de toda la lista, los que el usuario TODAVÍA no ha concedido;
    // si ya los dio todos, esta lista sale vacía y nos brincamos la pantalla
    val faltantes = permisos.filter {
        // le pregunta al sistema si ESE permiso ya está concedido
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    // el lanzador que pide TODOS los permisos faltantes de un solo golpe;
    // Android va sacando los diálogos uno detrás de otro, bien cómodo
    val pedirPermisos = rememberLauncherForActivityResult(
        // el contrato pa' pedir VARIOS permisos, no de uno en uno wey
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // llegue como llegue la respuesta (aunque hayas dicho que no a todo),
        // dejamos pasar a la app: lo que falte se vuelve a pedir al usarlo
        onListo()
    }

    // efecto que corre una vez al entrar (y si cambia cuántos faltan):
    // si no falta NINGUNO, nos pasamos directo a la app sin mostrar nada
    LaunchedEffect(faltantes.size) {
        // lista vacía = todo concedido = pa' qué mostrar esto, adiós
        if (faltantes.isEmpty()) onListo()
    }

    // fondo crema que llena toda la pantalla, como el login
    Box(Modifier.fillMaxSize().background(PalmCream)) {
        // columna con scroll por si la pantalla es chiquita o está girada
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // el header de arriba: cajón con el degradado verde palmero
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        // degradado de verde oscurito a verde medio, el clásico
                        Brush.verticalGradient(listOf(PalmPrimaryDark, PalmPrimary, PalmSecondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                // todo el logo va centrado pa' que se vea pro
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

            // tarjeta de los permisos, montada un cachito arriba sobre el degradado
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
                    // título de la pantalla, pa' que sepan qué pasa aquí
                    Text("Antes de empezar", style = MaterialTheme.typography.headlineSmall)
                    // subtítulo que explica por qué los pedimos de una
                    Text(
                        "AgroPalma necesita estos permisos para funcionar de una:",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                    // espacio entre el subtítulo y la primera fila de permiso
                    Spacer(Modifier.height(16.dp))

                    // fila del permiso de ubicación (GPS de los reportes)
                    PermisoFila(
                        icono = Icons.Filled.LocationOn,
                        titulo = "Ubicación",
                        descripcion = "Para adjuntar el GPS a tus reportes de trabajo."
                    )
                    // espacio entre fila y fila
                    Spacer(Modifier.height(10.dp))
                    // fila del permiso de cámara (evidencia y escáner)
                    PermisoFila(
                        icono = Icons.Filled.PhotoCamera,
                        titulo = "Cámara",
                        descripcion = "Para la foto de evidencia y el escáner de palma."
                    )
                    // otro espacio, pa' el ritmo
                    Spacer(Modifier.height(10.dp))
                    // fila del permiso de notificaciones (avisos offline)
                    PermisoFila(
                        icono = Icons.Filled.Notifications,
                        titulo = "Notificaciones",
                        descripcion = "Para avisarte cuando no hay internet y cuándo se envían tus registros."
                    )

                    // espacio antes de los botones, que respiren
                    Spacer(Modifier.height(20.dp))

                    // botón gordo verde que dispara los diálogos de permisos
                    Button(
                        onClick = {
                            // le tiramos TODOS los faltantes de un golpe; el
                            // sistema saca los diálogos uno por uno solito
                            pedirPermisos.launch(faltantes.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        // el texto del botón principal, bien claro
                        Text("Dar permisos y continuar")
                    }

                    // y el botón discretito pa' los que quieren entrar sin dar nada;
                    // la app no se cierra ni se enoja, solo pide luego cuando toque
                    TextButton(
                        onClick = { onListo() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // el texto en grisita pa' que no compita con el botón verde
                        Text("Continuar sin permisos", color = PalmMuted)
                    }
                }
            }

            // espacio final abajo pa' que el scroll no quede pegado
            Spacer(Modifier.height(30.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: PermisoFila — fila de UN permiso
//  Propósito: la filita con el icono y la explicación de cada permiso,
//  pa' que el usuario sepa pa' qué es cada cosa antes de aceptar.
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PermisoFila(icono: ImageVector, titulo: String, descripcion: String) {
    // fila con icono + textos, alineados al centro vertical
    Row(verticalAlignment = Alignment.CenterVertically) {
        // cajita verde clarito con el icono del permiso adentro
        Box(
            Modifier
                .size(44.dp)
                .background(PalmPrimary.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            // el icono que llega por parámetro, pintado de verde palma
            Icon(icono, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(22.dp))
        }
        // espaciado entre la cajita y los textos
        Spacer(Modifier.size(12.dp))
        // título en negrita y la descripción chiquita debajo
        Column {
            // el nombre del permiso, pa' que se distinga de los demás
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            // pa' qué sirve, en lenguaje humano y sin tecnicismos
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = PalmMuted)
        }
    }
}
