/*
 * AppSettings.kt - AJUSTES GENERALES DE LA APP
 * --------------------------------------------
 * Guarda en SharedPreferences las preferencias que NO son datos del
 * usuario, sino de cómo quiere que se vea la app. Por ahora:
 *  - darkMode: el modo oscuro/claro que el user elige en Ajustes.
 *
 * Así como Session recuerda quién anda logueado, esto recuerda si la
 * app debe abrirse de noche o de día. Sobrevive a cerrar la app, obvio.
 */

// el paquete de datos, aquí viven Session y este su hermanito
package com.agropalma.app.data

// el Context pa poder abrir las SharedPreferences wey
import android.content.Context

// recuerda los ajustes visuales de la app, hoy el modo oscuro nomás
class AppSettings(context: Context) {
    // las prefs privadas de la app, aquí guardadito el modo elegido
    private val prefs = context.getSharedPreferences("agropalma_settings", Context.MODE_PRIVATE)

    // true = modo oscuro (noche), false = modo claro (de siempre)
    var darkMode: Boolean
        // lee lo que el user dejó elegido la última vez (claro por defecto)
        get() = prefs.getBoolean(KEY_DARK, false)
        // guarda el modo elegido pa' que la próxima apertura lo respete
        set(value) {
            // abre el editor, mete el booleano y aplica, ahí nomás
            prefs.edit().putBoolean(KEY_DARK, value).apply()
        }

    // las constantes privadas, nadie más las necesita ver
    private companion object {
        // la clave donde vive el modo oscuro dentro de las prefs
        const val KEY_DARK = "dark_mode"
    }
}
