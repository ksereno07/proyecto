/*
 * Session.kt - SESION DEL USUARIO
 * -------------------------------
 * Guarda en SharedPreferences el id del usuario que inicio sesion, para
 * que la app lo recuerde al cerrarse y lo lleve directo a su panel.
 * clear() se usa al cerrar sesion.
 */

// el paquete donde vive la capa de datos, aquí nomás
package com.agropalma.app.data

// el Context pa' poder abrir las SharedPreferences wey
import android.content.Context

// esta clase acuerda quién anda logueado, pa' no pedir login cada vez que abres la app
class Session(context: Context) {
    // las prefs privadas de la app, aquí guardadito el id del user
    private val prefs = context.getSharedPreferences("agropalma_session", Context.MODE_PRIVATE)

    // el id del usuario logueado, si es null es que nadie ha entrado
    var userId: Long?
        // lee el id guardado, y si sale -1 lo vuelve null, o sea, sin sesión
        get() = prefs.getLong(KEY, -1L).takeIf { it > 0 }
        // aquí se mete el id cuando alguien inicia sesión (o se borra si mandan null)
        set(value) {
            // abre el editor de las prefs pa' escribir en ellas
            prefs.edit().apply {
                // si el valor es null borra la clave, si no guarda el id ahí nomás
                if (value == null) remove(KEY) else putLong(KEY, value)
            }.apply()
        }

    // borra la sesión, esto es pa' cerrar sesión wey, no la llames por accidente
    fun clear() {
        // quita el id guardado y aplica, sesión fuera ya
        prefs.edit().remove(KEY).apply()
    }

    // las constantes privadas de la clase, nadie más las ve
    private companion object {
        // el nombre de la clave donde vive el id del usuario
        const val KEY = "user_id"
    }
}
