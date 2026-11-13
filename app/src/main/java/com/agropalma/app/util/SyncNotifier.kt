/*
 * SyncNotifier.kt - EL QUE MANDA LAS NOTIFICACIONES
 * -------------------------------------------------
 * Este archivo es el que agarra el megáfono y te avisa en la barra de
 * notificaciones dos cosas clave:
 *  1. Cuando NO hay internet ("guarda todo tranquilo, se envía solo").
 *  2. Cuando VOLVIÓ la señal y ya se enviaron los registros que estaban
 *     esperando en la cola.
 * Ojo: las notificaciones solo salen si el usuario le dio permiso
 * (Android 13+ lo exige, antes no la cagues preguntando por qué no salen).
 */

package com.agropalma.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agropalma.app.R

// objeto simple: no necesita que lo construyan, se usa directo (SyncNotifier.sinInternet(...))
object SyncNotifier {

    // el "canal" donde van a vivir estas notificaciones (Android 8+ lo exige sí o sí)
    private const val CHANNEL_ID = "estado_conexion"

    // id fijo del aviso de "sin internet" (fijo pa' que se actualice solo y no se llene de clones)
    private const val ID_SIN_INTERNET = 1001

    // id fijo del aviso de "ya se enviaron los pendientes"
    private const val ID_ENVIADOS = 1002

    /**
     * Crea el canal de notificaciones. Llamarlo UNA vez al abrir la app
     * (si ya existe Android lo ignora, no hay problema).
     */
    fun crearCanal(context: Context) {
        // los canales solo existen desde Android 8, en los viejos esto no existe
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // el canal: nombre visible + importancia ALTA pa' que se vea arriba bien visible
        val canal = NotificationChannel(
            CHANNEL_ID,
            "Estado de la conexión",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            // la descripción que se ve en la configuración del teléfono
            description = "Avisa cuando no hay internet y cuándo se envían los registros pendientes."
        }
        // le pedimos al sistema que registre el canal, sin esto no sale nada wey
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /**
     * Aviso de "no hay internet". No asusta al usuario: le recuerda que
     * TODO queda guardado en el celular y se envía solo cuando vuelva la señal.
     */
    fun sinInternet(context: Context) {
        // el texto exacto que pidió el jefe: tranquilo, se manda solo cuando haya señal
        mostrar(
            context,
            id = ID_SIN_INTERNET,
            icono = R.drawable.ic_noti_warn,
            titulo = "Sin internet",
            texto = "Todo lo que registres queda guardado y se envía solo cuando vuelva la señal."
        )
    }

    /**
     * Aviso de "ya se enviaron los que estaban en cola". Sale cuando
     * vuelve la señal y la cola se vacía.
     */
    fun enviados(context: Context, cantidad: Int) {
        // plural bonito: "1 registro" vs "3 registros", pa' que no suene raro
        val plural = if (cantidad == 1) "" else "s"
        mostrar(
            context,
            id = ID_ENVIADOS,
            icono = R.drawable.ic_noti_ok,
            titulo = "Se enviaron $cantidad registro$plural",
            texto = "Volviste a tener señal y todo lo que estaba en cola ya viajó. Bien ahí. 🌴"
        )
    }

    /**
     * Muestra la notificación (si hay permiso, si no, ni modos).
     */
    private fun mostrar(context: Context, id: Int, icono: Int, titulo: String, texto: String) {
        // Android 13+ exige permiso pa' las notificaciones, checalo antes o truena
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        // armamos la notificación con el builder viejito pero confiable
        val noti = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icono) // el iconito que se ve en la barranita
            .setContentTitle(titulo) // el titulo en negrilla
            .setContentText(texto) // el mensajito debajo del titulo
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto)) // texto largo sin recortes
            .setPriority(NotificationCompat.PRIORITY_HIGH) // alta pa' que salga arriba
            .setAutoCancel(true) // se borra sola cuando el user la toca
            .build()
        // y la disparamos al sistema con el id fijo
        NotificationManagerCompat.from(context).notify(id, noti)
    }
}
