/*
 * NetWatcher.kt - EL VIGILANTE DE LA SEÑAL
 * ----------------------------------------
 * Este vato solo hace una cosa: estar pendiente de si hay internet o no.
 * Android le avisa cuando la red cambia (wifi prendido, datos apagados, etc)
 * y de aquí le pasamos el dato a toda la app en vivo con un StateFlow.
 * Sin internet NO se cae la app: solo se guarda todo y ya.
 */

package com.agropalma.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// el vigilante: pregunta "hay internet?" y avisa cuando cambia
class NetWatcher(context: Context) {

    // el jefe de las redes de Android, sin él no sabríamos nada de nada
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    // aquí guardamos si hay internet, arranca con el estado REAL del teléfono
    private val _online = MutableStateFlow(hayRed())

    // esto es lo que ven las pantallas: true = con señal, false = sin señal
    val online: StateFlow<Boolean> = _online

    // Android nos llama aquí cada vez que la red cambia de estado
    private val callback = object : ConnectivityManager.NetworkCallback() {
        // se conectó una red (wifi o datos), o sea YA HAY internet 🎉
        override fun onAvailable(network: Network) {
            _online.value = true
        }
        // se cayó la red, pero ojo: puede que quede otra viva (wifi + datos)
        override fun onLost(network: Network) {
            // por eso volvemos a preguntar, no la cagues asumiendo
            _online.value = hayRed()
        }
    }

    // pregunta directo: ¿el teléfono tiene una red con internet AHORITA?
    fun hayRed(): Boolean {
        // si no hay ninguna red activa, claramente no hay internet wey
        val network = cm.activeNetwork ?: return false
        // sacamos las habilidades de esa red
        val caps = cm.getNetworkCapabilities(network) ?: return false
        // que la red JURE que tiene internet, si no es puro farol
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // prende el vigilante (llamarlo al abrir la app, UNA sola vez)
    fun start() {
        // try por si algún celular raro no deja registrar el callback
        try { cm.registerDefaultNetworkCallback(callback) } catch (_: Exception) { }
    }

    // apaga el vigilante (cuando la app se cierra, pa' no dejar basura)
    fun stop() {
        // otro try: si ya estaba apagado no explota la app
        try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) { }
    }
}
