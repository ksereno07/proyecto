/*
 * LocationHelper.kt - UBICACION GPS
 * ---------------------------------
 * Pide y lee la ultima ubicacion conocida del telefono (GPS o red) para
 * adjuntarla al reporte de trabajo. Si el usuario no dio permiso, devuelve
 * null y el reporte se guarda como "Sin GPS".
 */

// paquete de la utilidad del GPS, aquí nomás va esto
package com.agropalma.app.util

// imports pa' permisos, contexto y el servicio de ubicación del sistema
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

// objeto único que se encarga de todo el rollo del GPS
object LocationHelper {

    // checa si el usuario ya dio permiso de ubicación, el fino o el aproximado
    fun hasPermission(context: Context): Boolean =
        // pregunta si tiene el permiso fino (GPS bien preciso)
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            // o el aproximado (por red); con cualquiera de los dos ya pasa
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // saca la última ubicación conocida como par (latitud, longitud), o null si no hay nada
    fun lastKnown(context: Context): Pair<Double, Double>? {
        // sin permiso no hay GPS, pa' qué fingir, devolvemos null
        if (!hasPermission(context)) return null
        // agarra el servicio de ubicación del sistema, y si no existe pues nada
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        // probamos con GPS y con red, el que tenga dato gana
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        // le preguntamos a cada proveedor si ya sabe dónde andamos
        for (provider in providers) {
            try {
                // saca la última posición que recuerda; si no recuerda, sigue al siguiente
                val location = manager.getLastKnownLocation(provider) ?: continue
                // armamos el par latitud/longitud y nos salimos con eso
                return location.latitude to location.longitude
            } catch (_: SecurityException) {
                // si nos corre por permisos revocados al vuelo, mejor null y ya
                return null
            } catch (_: Exception) {
                // Si falla este proveedor (GPS o red), se intenta el siguiente.
            }
        }
        // ningún proveedor supo dónde estamos, devolvemos null pa' "Sin GPS"
        return null
    }
}
