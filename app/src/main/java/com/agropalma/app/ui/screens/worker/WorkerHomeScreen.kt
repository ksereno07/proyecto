/*
 * WorkerHomeScreen.kt - PANEL DEL TRABAJADOR
 * ------------------------------------------
 * Lista las parcelas a las que el trabajador se ha unido con codigo.
 * Cada tarjeta abre el detalle de la parcela (labores, reportes y
 * escaner). Si no tiene ninguna, invita a unirse con el codigo del dueno.
 */

// el package, o sea la dirección de esta pantalla dentro del proyecto
package com.agropalma.app.ui.screens.worker

// imports de Compose: layout, iconos, Material 3 y navegación, el combo completo wey
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// lo propio de AgroPalma: el ViewModel compartido, los datos y componentes de la casa
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Parcel
import com.agropalma.app.ui.components.AgroBottomBar
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.BottomItem
import com.agropalma.app.ui.components.EmptyState
import com.agropalma.app.ui.components.SectionTitle
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSecondary
import com.agropalma.app.util.Fmt
import androidx.compose.foundation.clickable

// los botones de la barra de abajo del trabajador: inicio, unirse y perfil
val workerBottomItems = listOf(
    BottomItem("Inicio", Icons.Filled.Home),
    BottomItem("Unirse", Icons.Filled.QrCodeScanner),
    BottomItem("Perfil", Icons.Filled.Person)
)

// helper pa' moverse entre pantallas del trabajador sin duplicar nada
fun workerNavigate(nav: NavController, route: String) {
    // navega a la ruta que le des, fácil
    nav.navigate(route) {
        // limpia la pila hasta el home del trabajador pero guardando su estado
        popUpTo("worker/home") { saveState = true }
        // que no abra la misma pantalla dos veces, no la cagues
        launchSingleTop = true
        // y al volver restaura lo que ya tenía, puro detalle
        restoreState = true
    }
}

// la pantalla de inicio del trabajador: su saludo, sus parcelas y todo el rollo
@Composable
fun WorkerHomeScreen(vm: AppViewModel, nav: NavController) {
    // el usuario logueado, o sea el pelao que está usando la app
    val user = vm.currentUser

    // apenas entra a la pantalla carga sus datos de trabajador (parcelas y demás)
    LaunchedEffect(Unit) { vm.loadWorkerData() }

    // el esqueleto de la pantalla: fondo crema y barras arriba y abajo
    Scaffold(
        // fondo color crema, pa' que se vea fresquito
        containerColor = PalmCream,
        // barra de arriba con el nombre de la app y el título del panel
        topBar = { AgroTopBar("AgroPalma", "Panel del trabajador") },
        // barra de abajo con los botones de navegación del trabajador
        bottomBar = {
            // al tocar un botón mira el índice y lo manda a su pantalla
            AgroBottomBar(workerBottomItems, selected = 0) { index ->
                when (index) {
                    // índice 1: ir a unirse a una parcela con código
                    1 -> workerNavigate(nav, "worker/join")
                    // índice 2: ir a su perfil
                    2 -> workerNavigate(nav, "worker/profile")
                }
            }
        }
    ) { padding ->
        // lista con scroll donde va todo el contenido de la pantalla
        LazyColumn(
            // ocupa toda la pantalla respetando el espacio de las barras
            modifier = Modifier.fillMaxSize().padding(padding),
            // margen de 16 por dentro pa' que no se pegue a los bordes
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            // espacio de 14 entre bloques, pa' que respiren
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // primer bloque: el saludo al trabajador
            item {
                // el hola con el primer nombre del pelao; si no hay nombre, "trabajador"
                Text(
                    "Hola, ${user?.name?.substringBefore(' ') ?: "trabajador"}",
                    style = MaterialTheme.typography.headlineSmall
                )
                // texto gris que cambia según si ya tiene parcelas o todavía no
                Text(
                    if (vm.workerParcels.isEmpty()) "Aún no estás vinculado a una parcela."
                    else "Estas son tus parcelas activas. Toca una para ver tus labores.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PalmMuted
                )
            }

            // si el pelao no tiene parcelas vinculadas, sale pantallita invitándolo a unirse
            if (vm.workerParcels.isEmpty()) {
                // estado vacío: le dice que le pida el código al dueño
                item {
                    EmptyState(
                        icon = Icons.Filled.QrCodeScanner,
                        title = "Únete a tu primera parcela",
                        message = "Pídele al dueño el código de entrada de 1 minuto y úsalo para vincularte a la cuadrilla.",
                        actionLabel = "Tengo un código de invitación",
                        // al tocar el botón lo manda a la pantalla del código
                        onAction = { nav.navigate("worker/join") }
                    )
                }
            } else {
                // si sí tiene parcelas, primero sale la tarjeta verde de la jornada
                item {
                    // tarjeta verde bien grande con esquinas redondeadas
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PalmPrimary,
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        // fila con icono, textos y pill de estado, todo centrado
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // la palmita en blanco bien grande
                            Icon(Icons.Filled.Park, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                            // espacio entre el icono y el texto
                            Spacer(Modifier.size(12.dp))
                            // columna con el título y el detalle de la jornada
                            Column(Modifier.weight(1f)) {
                                // título que dice que hay jornada en curso
                                Text("Jornada en curso", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                // cuántas parcelas tiene vinculadas el pelao
                                Text(
                                    "${vm.workerParcels.size} parcela(s) vinculada(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                            // pill redondito que dice "Activo", puro estilo
                            Surface(color = PalmSecondary, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    "Activo",
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // título de la sección de sus parcelas
                item { SectionTitle("Mis parcelas", "Vinculadas con tu código de cuadrilla") }

                // aquí el pelao ve sus parcelas, toca una y entra a ver sus labores
                items(vm.workerParcels) { parcel ->
                    // cada parcela se dibuja con la fila reutilizable
                    ParcelRow(parcel) {
                        // al tocar lo manda al detalle de esa parcela con su id
                        nav.navigate("worker/parcel/${parcel.id}")
                    }
                }

                // tarjeta pa' unirse a otra parcela más, por si trabaja en varias
                item {
                    // tarjeta clickeable que lo manda a la pantalla del código
                    AgroCard(modifier = Modifier.clickable { nav.navigate("worker/join") }) {
                        // fila con icono, textos y flechita
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // icono de más, obvio pa' agregar algo nuevo
                            Icon(Icons.Filled.AddCircle, contentDescription = null, tint = PalmPrimary)
                            // espacio entre el icono y el texto
                            Spacer(Modifier.size(10.dp))
                            // columna con el título y su descripción
                            Column(Modifier.weight(1f)) {
                                Text("Unirme a otra parcela", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Usa el código vigente que te dé el dueño.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalmMuted
                                )
                            }
                            // flechita pa' que se vea que es tocable
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PalmMuted)
                        }
                    }
                }
            }
        }
    }
}

// fila reutilizable que dibuja una parcela en la lista, tocable pa' entrar
@Composable
private fun ParcelRow(parcel: Parcel, onClick: () -> Unit) {
    // tarjeta clickeable que ejecuta el onClick cuando la tocan
    AgroCard(modifier = Modifier.clickable(onClick = onClick)) {
        // fila principal: icono, info de la parcela y botón de entrar
        Row(verticalAlignment = Alignment.CenterVertically) {
            // cuadrito redondeado con el icono de palma
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                // columna que centra el icono dentro del cuadrito
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // la palmita en color primario
                    Icon(Icons.Filled.Park, contentDescription = null, tint = PalmPrimary)
                }
            }
            // espacio entre el cuadrito y la info
            Spacer(Modifier.size(12.dp))
            // columna con nombre, ubicación y patrón de la parcela
            Column(Modifier.weight(1f)) {
                // nombre de la parcela en grande
                Text(parcel.name, style = MaterialTheme.typography.titleMedium)
                // fila con el pin de ubicación y los datos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // iconito de ubicación bien discreto
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PalmMuted, modifier = Modifier.size(13.dp))
                    // ubicación y hectáreas de la parcela, todo junto
                    Text(
                        " ${parcel.location} • ${Fmt.number(parcel.hectares)} ha",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                }
                // quién es el patrón, pa' saber a quién le trabaja
                Text(
                    "Patrón: ${parcel.ownerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
            }
            // lado derecho con el "Entrar" y la flechita
            Column(horizontalAlignment = Alignment.End) {
                // el Entrar en verde, pa' invitar a que lo toque
                Text("Entrar", style = MaterialTheme.typography.labelLarge, color = PalmPrimary, fontWeight = FontWeight.SemiBold)
                // flechita hacia adelante
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PalmPrimary)
            }
        }
    }
}
