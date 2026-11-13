/*
 * MainActivity.kt - PUNTO DE ENTRADA DE LA APP
 * -------------------------------------------
 * Android abre esta actividad cuando el usuario toca el icono de AgroPalma.
 * Aquí se carga el tema visual (colores y tipografia) y se arranca todo:
 *  1. Se crea el canal de notificaciones (pa' los avisos de sin internet).
 *  2. Se muestra PermissionSplashScreen, que pide ANTES DE ENTRAR los
 *     permisos de ubicacion, camara y notificaciones.
 *  3. Cuando el usuario acepta (o decide continuar sin ellos), recien
 *     ahi se abre la navegacion normal (AppNav) con el ViewModel compartido.
 */

// el paquete raíz de la app
package com.agropalma.app

// imports de Android: el Bundle que llega al onCreate
import android.os.Bundle
// la Activity, montar la UI de Compose y crear el ViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
// estado de Compose: bandera que recuerda si ya pasamos por los permisos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// lo propio: la pantalla de permisos, la navegación, el tema y las notis
import com.agropalma.app.ui.AppNav
import com.agropalma.app.ui.screens.PermissionSplashScreen
import com.agropalma.app.ui.theme.AgroPalmaTheme
import com.agropalma.app.util.SyncNotifier

// la actividad principal: aquí abre la app cuando tocas el icono
class MainActivity : ComponentActivity() {

    // el onCreate: lo primero que corre al abrir la app
    override fun onCreate(savedInstanceState: Bundle?) {
        // siempre se llama al papá primero, no la cagues
        super.onCreate(savedInstanceState)
        // creamos el canal de notificaciones de estado de conexión
        // (idempotente: si ya existe Android lo ignora y no truena nada)
        SyncNotifier.crearCanal(this)
        // aquí montamos toda la UI con Compose
        setContent {
            // el ViewModel compartido: cerebro de toda la app. Se crea
            // desde ya pa' que la sesión, el vigilante de la señal y el
            // modo claro/oscuro estén listos desde el primer fotograma
            val vm: AppViewModel = viewModel()
            // el tema de la app: colores palma, tipografía, todo el look.
            // Le pasamos el modo que el user eligió en Ajustes (claro/oscuro);
            // el ViewModel lo lee de SharedPreferences pa' recordarlo
            AgroPalmaTheme(darkTheme = vm.darkMode) {
                // bandera que recuerda si ya pasamos por el portón de
                // permisos; rememberSaveable pa' que sobreviva al giro
                // de pantalla (si ya diste continuar, no lo vuelve a preguntar)
                var permisosListos by rememberSaveable { mutableStateOf(false) }
                // mientras no haya pasado por los permisos, mostramos el
                // portón; después ya se abre la app de verdad
                if (!permisosListos) {
                    // la pantalla que pide ubicación, cámara y notificaciones;
                    // onListo se dispara cuando acepta (o continúa sin ellos)
                    PermissionSplashScreen(onListo = { permisosListos = true })
                } else {
                    // y la navegación entre pantallas (login, paneles, etc)
                    AppNav(vm)
                }
            }
        }
    }
}
