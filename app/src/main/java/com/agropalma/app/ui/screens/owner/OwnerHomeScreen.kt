/*
 * OwnerHomeScreen.kt - PANEL PRINCIPAL DEL DUENO
 * ----------------------------------------------
 * Resumen de la parcela: nombre, ubicacion, hectareas, codigo de
 * invitacion dinamico (PALMA-XXXX) con cuenta regresiva de 1 minuto y
 * botones para copiarlo o renovarlo. Muestra trabajadores vinculados,
 * totales por pagar, costo de insumos, ultimos reportes y alertas de
 * salud del cultivo. Tambien define el menu inferior del rol dueno.
 */

// el package, o sea la dirección del archivo dentro del proyecto wey
package com.agropalma.app.ui.screens.owner

// imports de Android y Compose: layouts, iconos, material 3, todo lo del UI
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// ya pa' cerrar: navegación, el ViewModel compartido, componentes y colores de la app
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Parcel
import com.agropalma.app.ui.components.AgroBottomBar
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.BottomItem
import com.agropalma.app.ui.components.EmptyState
import com.agropalma.app.ui.components.SectionTitle
import com.agropalma.app.ui.components.StatTile
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmPrimaryDark
import com.agropalma.app.ui.theme.PalmSecondary
import com.agropalma.app.ui.theme.PalmSurface
import com.agropalma.app.ui.theme.PalmTertiary
import com.agropalma.app.util.Fmt
import kotlinx.coroutines.delay

// las pestañas del menú de abajo del dueño, aquí nomás las definimos
val ownerBottomItems = listOf(
    // cada BottomItem lleva su texto y su iconito correspondiente
    BottomItem("Inicio", Icons.Filled.Home),
    BottomItem("Reportes", Icons.Filled.ReceiptLong),
    BottomItem("Costos", Icons.Filled.Payments),
    BottomItem("Perfil", Icons.Filled.Person)
)

// helper pa' navegar entre pantallas del dueño sin duplicar nada
fun ownerNavigate(nav: NavController, route: String) {
    // le decimos al nav a dónde se va
    nav.navigate(route) {
        // limpia el backstack hasta home guardando el estado, pa' que no se acumulen pantallas wey
        popUpTo("owner/home") { saveState = true }
        // no abre la misma pantalla dos veces seguidas
        launchSingleTop = true
        // y al volver restaura lo que ya tenía, ahí nomás
        restoreState = true
    }
}

// el panel principal del dueño, el corazón de la app wey
@Composable
fun OwnerHomeScreen(vm: AppViewModel, nav: NavController) {
    // jalamos la parcela activa y el user del ViewModel compartido
    val parcel = vm.ownerParcel
    val user = vm.currentUser

    // el Scaffold arma la estructura: fondo, barra arriba y menú abajo
    Scaffold(
        // fondo crema pa' que se vea fresco
        containerColor = PalmCream,
        // la barra de arriba con el nombre y el rol del dueño
        topBar = { AgroTopBar("AgroPalma", "Panel del dueño") },
        // el menú de abajo, con "Inicio" seleccionado (índice 0)
        bottomBar = {
            AgroBottomBar(ownerBottomItems, selected = 0) { index ->
                // según la pestaña que apriete lo mandamos a su pantalla
                when (index) {
                    // 1 = reportes de la cuadrilla
                    1 -> ownerNavigate(nav, "owner/reports")
                    // 2 = costos y pagos
                    2 -> ownerNavigate(nav, "owner/costs")
                    // 3 = perfil del dueño
                    3 -> ownerNavigate(nav, "owner/profile")
                }
            }
        }
    ) { padding ->
        // si el dueño aún no tiene parcela le mostramos esto pa' que cree su grupo
        if (parcel == null) {
            // caja que llena toda la pantalla, ahí centraremos el aviso
            Box(Modifier.fillMaxSize().padding(padding)) {
                // estado vacío: iconito, textos y botón pa' crear la parcela
                EmptyState(
                    icon = Icons.Filled.Groups,
                    title = "Aún no tienes una parcela",
                    message = "Crea tu grupo de parcela para generar el código de acceso y empezar a recibir reportes de tu cuadrilla.",
                    actionLabel = "Crear grupo de parcela",
                    // al apretar lo mandamos a crear su primera parcela
                    onAction = { nav.navigate("owner/create") },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // y ya, nos salimos del Scaffold, no hay nada más que pintar
            return@Scaffold
        }

        // la lista scrolleable con todo el resumen de la parcela
        LazyColumn(
            // llena la pantalla y respeta el padding del Scaffold
            modifier = Modifier.fillMaxSize().padding(padding),
            // margen pa' que el contenido no vaya pegado a los bordes
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            // separación vertical entre tarjetas
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // primer bloque: el saludo al dueño
            item {
                // "Hola, [primer nombre]", y si no hay nombre le decimos "dueño"
                Text(
                    "Hola, ${user?.name?.substringBefore(' ') ?: "dueño"}",
                    style = MaterialTheme.typography.headlineSmall
                )
                // el subtitulo gris del resumen de hoy
                Text(
                    "Este es el resumen de tu parcela hoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PalmMuted
                )
            }

            // la tarjetota hero con los datos de la parcela activa
            item { ParcelHero(parcel) }

            // la tarjeta oscura del código de invitación con su cuenta regresiva
            item { CodeCard(parcel, vm) }

            // fila con las 3 estadísticas clave del dueño
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // lo que debe pagarle a la cuadrilla (reportes sin pagar)
                    StatTile(
                        icon = Icons.Filled.Payments,
                        label = "Por pagar",
                        value = Fmt.money(vm.ownerStats.pendingPay),
                        modifier = Modifier.weight(1f)
                    )
                    // costo de los insumos/materiales pendientes
                    StatTile(
                        icon = Icons.Filled.Inventory2,
                        label = "Insumos",
                        value = Fmt.money(vm.ownerStats.materialsTotal),
                        modifier = Modifier.weight(1f)
                    )
                    // cuántos reportes metió la cuadrilla hoy
                    StatTile(
                        icon = Icons.Filled.ReceiptLong,
                        label = "Reportes hoy",
                        value = vm.ownerStats.reportCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // fila con los 2 botones rápidos: Reportes y Costos
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // botón pa' ir a ver los reportes de la cuadrilla
                    Button(
                        onClick = { ownerNavigate(nav, "owner/reports") },
                        // mitad de la fila, alto fijo y esquinas redonditas
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PalmSecondary)
                    ) {
                        // iconito + espacio + texto, pa' que se vea pro
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Reportes")
                    }
                    // botón pa' ir a la pantalla de costos y pagos
                    Button(
                        onClick = { ownerNavigate(nav, "owner/costs") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PalmTertiary, contentColor = PalmPrimaryDark)
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Costos")
                    }
                }
            }

            // título de la sección de trabajadores con sus contadores
            item {
                SectionTitle(
                    "Trabajadores en parcela",
                    "${vm.ownerWorkers.size} vinculados • ${vm.ownerStats.workerCount} activos"
                )
            }

            // si no hay trabajadores le echamos una tarjetita pa' que comparta el código
            if (vm.ownerWorkers.isEmpty()) {
                item {
                    // la tarjeta del tip pa' invitar al primer compa
                    AgroCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // iconito del QR en verde
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = PalmPrimary)
                            Spacer(Modifier.size(10.dp))
                            Text(
                                "Comparte el código de arriba para que tu primer trabajador se una.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                // si ya hay cuadrilla, pintamos una tarjeta por cada trabajador
                items(vm.ownerWorkers) { worker ->
                    AgroCard {
                        // fila: avatar, datos y su badge de activo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // el circulito con las iniciales del compa
                            Avatar(worker.name)
                            Spacer(Modifier.size(12.dp))
                            // columna con el nombre y su documento
                            Column(Modifier.weight(1f)) {
                                Text(worker.name, style = MaterialTheme.typography.titleMedium)
                                // si no tiene cédula mostramos el teléfono, sino la cédula
                                Text(
                                    if (worker.cedula.isNullOrBlank()) worker.phone else "C.C. ${worker.cedula}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                            // la pastillita redonda de "Activo", bien pímpale
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    "Activo",
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PalmPrimary
                                )
                            }
                        }
                    }
                }
            }

            // si hay reportes mostramos los más recientes de la cuadrilla
            if (vm.ownerReports.isNotEmpty()) {
                item {
                    SectionTitle("Últimos reportes", "Lo que acaba de enviar tu cuadrilla")
                }
                // nomás los primeros 3, pa' no hacer eterno el scroll wey
                items(vm.ownerReports.take(3)) { report ->
                    AgroCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // columna izquierda: qué labor hizo y quién la hizo
                            Column(Modifier.weight(1f)) {
                                // el título de la labor (poda, cosecha, lo que sea)
                                Text(report.taskTitle, style = MaterialTheme.typography.titleMedium)
                                // quién la hizo y cuántas unidades sacó
                                Text(
                                    "${report.workerName} • ${Fmt.number(report.units)} ${report.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                            // y a la derecha la plata que cuesta esa labor
                            Text(
                                Fmt.money(report.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = PalmPrimary
                            )
                        }
                    }
                }
            }

            // si hay diagnósticos del escáner los mostramos como alertas de enfermedad
            if (vm.ownerDiagnoses.isNotEmpty()) {
                item {
                    SectionTitle("Alertas de salud", "Diagnósticos enviados desde el escáner")
                }
                // nomás las 2 alertas más recientes
                items(vm.ownerDiagnoses.take(2)) { diagnosis ->
                    AgroCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // el iconito de advertencia pa' que se note que algo pasa
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = PalmTertiary)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                // la enfermedad que detectó el escáner
                                Text(diagnosis.disease, style = MaterialTheme.typography.titleMedium)
                                // quién lo escaneó y qué tan seguro estaba el modelo
                                Text(
                                    "${diagnosis.workerName} • ${diagnosis.confidence}% de certeza",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                        }
                    }
                }
            }

            // la última tarjeta: acceso directo pa' crear otra parcela
            item {
                AgroCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // iconito de más, pa' agregar otra parcela
                        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = PalmPrimary)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("¿Otra parcela?", style = MaterialTheme.typography.titleMedium)
                            // le aclaramos que cada parcela es su propio mundo
                            Text(
                                "Cada parcela maneja su propio código y cuadrilla.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PalmMuted
                            )
                        }
                        // la flechita que manda a la pantalla de crear parcela
                        IconButton(onClick = { nav.navigate("owner/create") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Crear otra parcela")
                        }
                    }
                }
            }
        }
    }
}

// la tarjetota de la parcela activa, con degradado y todo el style
@Composable
private fun ParcelHero(parcel: Parcel) {
    // caja que llena el ancho con fondo degradado y esquinas redonditas
    Box(
        Modifier
            .fillMaxWidth()
            // el degradado verde que le da el toque pro
            .background(
                Brush.linearGradient(listOf(PalmPrimary, PalmSecondary)),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        // todo el contenido va en una columna
        Column {
            // fila del encabezado: iconito + textos de la parcela
            Row(verticalAlignment = Alignment.CenterVertically) {
                // cajita translúcida pa' el icono del QR
                Box(
                    Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // el icono del escáner en blanco
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(12.dp))
                // columna con la etiqueta y el nombre de la parcela
                Column {
                    // la etiqueta clarita de "Parcela activa"
                    Text("Parcela activa", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                    // el nombre de la parcela en grande
                    Text(parcel.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            // la ubicación de la parcela
            Text(parcel.location, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(2.dp))
            // las hectáreas y quién es el encargado, todo en una línea
            Text(
                "${Fmt.number(parcel.hectares)} hectáreas • Encargado: ${parcel.ownerName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// la tarjeta oscura del código de invitación, con timer y botones
@Composable
private fun CodeCard(parcel: Parcel, vm: AppViewModel) {
    // el contexto de Android, pa' poder lanzar el Toast
    val context = LocalContext.current
    // el manager del portapapeles, pa' copiar el código
    val clipboard = LocalClipboardManager.current
    // el "ahora" en milisegundos, va cambiando cada segundo
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Reloj que se actualiza cada segundo para mostrar la cuenta regresiva
    // del codigo. Cuando llega a 0 el codigo se muestra como "VENCIDO".
    LaunchedEffect(parcel.code) {
        // bucle infinito que corre mientras la pantalla está viva
        while (true) {
            // refrescamos la hora actual
            now = System.currentTimeMillis()
            // esperamos un segundito y vuelta a empezar
            delay(1000)
        }
    }

    // Segundos restantes hasta que expire el codigo (nunca negativo).
    val remaining = ((parcel.codeExpiresAt - now) / 1000L).coerceAtLeast(0L)
    // si ya llegó a 0 el código está vencido, ya valió
    val expired = remaining <= 0

    // metemos todo dentro de la tarjeta oscura (CardDark)
    CardDark {
        // fila del encabezado con icono y textos
        Row(verticalAlignment = Alignment.CenterVertically) {
            // el icono del QR en color cálido
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = PalmTertiary)
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                // el título de la tarjeta
                Text("Código de entrada al grupo", style = MaterialTheme.typography.titleMedium, color = Color.White)
                // la explicación de por qué cambia tanto el código
                Text(
                    "Se renueva cada minuto por seguridad",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // el recuadro que muestra el código (o el aviso de vencido)
        Surface(
            // si venció se ve apagado, si no se ve en el color llamativo
            color = if (expired) Color.White.copy(alpha = 0.12f) else PalmTertiary,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // columna centrada con el código y la cuenta regresiva
            Column(
                Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // el código PALMA-XXXX en monoespaciada, bien hacker
                Text(
                    if (expired) "CÓDIGO VENCIDO" else parcel.code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    // más chico si está vencido, más grande el código vivo
                    fontSize = if (expired) 22.sp else 30.sp,
                    letterSpacing = 3.sp,
                    color = if (expired) Color.White.copy(alpha = 0.7f) else PalmPrimaryDark
                )
                // abajo la cuenta regresiva o el aviso de generar otro
                Text(
                    if (expired) "Genera uno nuevo para tu cuadrilla" else "Expira en ${remaining}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (expired) Color.White.copy(alpha = 0.7f) else PalmPrimaryDark.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // fila con los 2 botones: Copiar y Renovar
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // botón pa' copiar el código al portapapeles
            Button(
                onClick = {
                    // copia el código pa' mandarlo por WhatsApp wey
                    clipboard.setText(AnnotatedString(parcel.code))
                    // y un Toast confirmando que ya quedó copiado
                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                },
                // si ya venció el botón se apaga, no copies nada vencido pendejo
                enabled = !expired,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                // colores del botón fantasma: blanco translúcido y apagado cuando está deshabilitado
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.07f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Copiar")
            }
            // botón pa' generar un código nuevo (o renovar el vigente)
            Button(
                onClick = { vm.regenerateCode() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PalmTertiary, contentColor = PalmPrimaryDark)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                // el texto cambia: si venció dice "Generar código", si no "Renovar"
                Text(if (expired) "Generar código" else "Renovar")
            }
        }
    }
}

// helper pa' la tarjeta oscura, la usa la del código de invitación
@Composable
private fun CardDark(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    // superficie verde oscuro que llena el ancho con esquinas redondas
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PalmPrimary,
        shape = RoundedCornerShape(22.dp)
    ) {
        // el contenido va en una columna con su padding
        Column(Modifier.padding(18.dp), content = content)
    }
}

// el avatar circulito con las iniciales del trabajador
@Composable
private fun Avatar(name: String) {
    // sacamos las iniciales: las primeras 2 palabras del nombre en mayúsculas
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
    // cajita con fondo suave donde va el texto centrado
    Box(
        Modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        // las iniciales en el color del tema
        Text(initials, style = MaterialTheme.typography.labelLarge, color = PalmPrimary)
    }
}
