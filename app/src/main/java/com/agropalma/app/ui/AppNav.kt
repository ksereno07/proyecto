        /*
 * AppNav.kt - NAVEGACION ENTRE PANTALLAS
 * --------------------------------------
 * Define todas las rutas de la app y que pantalla se abre en cada una:
 *  - login, register/worker, register/owner
 *  - owner/home, owner/create, owner/reports, owner/costs, owner/profile
 *  - worker/home, worker/join, worker/parcel/{id}, worker/report/{id},
 *    worker/materials/{id}, worker/scanner/{id}
 *
 * Al iniciar, si hay una sesion guardada arranca en el panel del rol
 * correspondiente; si no, muestra el login.
 *
 * ADEMAS: envuelve toda la app con el banner de "sin internet" que se
 * pega arriba de cualquier pantalla cuando no hay señal (OfflineBanner)
 * y muestra cuántos registros van en la cola de envío.
 */

// el paquete de la UI: aquí vive la navegación
package com.agropalma.app.ui

// imports de Compose: layout y estados observables
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
// imports de Navigation Compose: rutas, NavHost y argumentos
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// el cerebro (ViewModel), los roles y el banner de sin internet
import com.agropalma.app.AppViewModel
import com.agropalma.app.data.Role
import com.agropalma.app.ui.components.OfflineBanner
// y TODAS las pantallas de la app (login, registros, paneles, escáner...)
import com.agropalma.app.ui.screens.LoginScreen
import com.agropalma.app.ui.screens.ProfileScreen
import com.agropalma.app.ui.screens.RegisterOwnerScreen
import com.agropalma.app.ui.screens.RegisterWorkerScreen
import com.agropalma.app.ui.screens.SettingsScreen
import com.agropalma.app.ui.screens.owner.CreateParcelScreen
import com.agropalma.app.ui.screens.owner.OwnerCostsScreen
import com.agropalma.app.ui.screens.owner.OwnerHomeScreen
import com.agropalma.app.ui.screens.owner.OwnerReportsScreen
import com.agropalma.app.ui.screens.worker.MaterialRequestScreen
import com.agropalma.app.ui.screens.worker.ReportWorkScreen
import com.agropalma.app.ui.screens.worker.ScannerScreen
import com.agropalma.app.ui.screens.worker.WorkerHomeScreen
import com.agropalma.app.ui.screens.worker.WorkerJoinScreen
import com.agropalma.app.ui.screens.worker.WorkerParcelScreen

// composable raíz de la navegación, aquí se arma toda la app
@Composable
// recibe el cerebro (ViewModel) y pinta las rutas según el rol
fun AppNav(vm: AppViewModel) {
    // el controlador que sabe en qué pantalla estamos y a dónde ir
    val nav = rememberNavController()
    // Si ya hay sesion guardada, arranca en el panel del rol correspondiente.
    // Si no hay usuario, arranca en el login.
    val start = when (vm.currentUser?.role) {
        // dueño → arranca directo en su panel
        Role.OWNER -> "owner/home"
        // trabajador → arranca en sus parcelas
        Role.WORKER -> "worker/home"
        // nadie logueado → al login a conseguir cuenta
        null -> "login"
    }

    // leemos en vivo si hay internet (true = con señal) para el banner
    val online by vm.online.collectAsState()

    // columna padre de TODA la app: primero el banner de sin internet
    // (arriba de todo, visible en cualquier pantalla) y debajo la navegación
    Column(modifier = Modifier.fillMaxSize()) {
        // la cinta verde de "sin internet": se muestra sola cuando no hay
        // red, dice cuántos registros esperan señal y se esconde al volver
        OfflineBanner(online = online, pendientes = vm.pendientesEnvio)

        // el NavHost: el contenedor que cambia de pantalla según la ruta
        NavHost(navController = nav, startDestination = start, modifier = Modifier.weight(1f)) {
            // ── RUTAS PÚBLICAS (sin sesión) ──────────────────────────────
            composable("login") { LoginScreen(vm, nav) }                         // Iniciar sesión
            composable("register/worker") { RegisterWorkerScreen(vm, nav) }       // Registro del trabajador
            composable("register/owner") { RegisterOwnerScreen(vm, nav) }         // Registro del dueño

            // ── RUTAS DEL DUEÑO ─────────────────────────────────────────
            composable("owner/home") { OwnerHomeScreen(vm, nav) }                 // Panel: código, totales, alertas
            composable("owner/create") { CreateParcelScreen(vm, nav) }            // Crear grupo de parcela
            composable("owner/reports") { OwnerReportsScreen(vm, nav) }           // Reportes / materiales / salud
            composable("owner/costs") { OwnerCostsScreen(vm, nav) }               // Costos, gráfica de pagos y precios
            composable("owner/profile") { ProfileScreen(vm, nav) }               // Perfil y cerrar sesión

            // ── RUTAS DEL TRABAJADOR ────────────────────────────────────
            composable("worker/home") { WorkerHomeScreen(vm, nav) }               // Mis parcelas
            composable("worker/join") { WorkerJoinScreen(vm, nav) }               // Unirse con código PALMA-XXXX
            composable("worker/profile") { ProfileScreen(vm, nav) }               // Perfil y cerrar sesión

            // ── RUTAS COMPARTIDAS (cualquier rol logueado) ──────────────
            // ajustes: modo oscuro, editar cuenta, contraseña y eliminar cuenta
            composable("settings") { SettingsScreen(vm, nav) }                   // Ajustes de la app y la cuenta

            // ── RUTAS DEL TRABAJADOR QUE DEPENDEN DE UNA PARCELA ────────
            // El {parcelId} llega desde la tarjeta de la parcela.
            composable(
                // la ruta con el id de la parcela pegado en la URL
                route = "worker/parcel/{parcelId}",
                // el argumento viaja como número largo (Long)
                arguments = listOf(navArgument("parcelId") { type = NavType.LongType })
            ) { entry ->
                WorkerParcelScreen(vm, nav, entry.arguments?.getLong("parcelId") ?: 0L) // Labores y avance
            }

            // la ruta pa reportar el avance, mismo rollo del {parcelId}
            composable(
                route = "worker/report/{parcelId}",
                arguments = listOf(navArgument("parcelId") { type = NavType.LongType })
            ) { entry ->
                ReportWorkScreen(vm, nav, entry.arguments?.getLong("parcelId") ?: 0L)   // Reportar avance
            }

            // la ruta pa pedir materiales, también con su parcelId
            composable(
                route = "worker/materials/{parcelId}",
                arguments = listOf(navArgument("parcelId") { type = NavType.LongType })
            ) { entry ->
                MaterialRequestScreen(vm, nav, entry.arguments?.getLong("parcelId") ?: 0L) // Pedir materiales
            }

            // y la ruta del escáner de palmas enfermas, con su parcelId
            composable(
                route = "worker/scanner/{parcelId}",
                arguments = listOf(navArgument("parcelId") { type = NavType.LongType })
            ) { entry ->
                ScannerScreen(vm, nav, entry.arguments?.getLong("parcelId") ?: 0L)      // Escáner de palma
            }
        }
    }
}
