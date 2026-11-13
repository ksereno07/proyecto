/*
 * Fmt.kt - FORMATO DE TEXTOS
 * --------------------------
 * Funciones de ayuda para mostrar dinero en pesos colombianos, numeros
 * sin decimales innecesarios, fechas legibles, coordenadas GPS y el
 * nombre en espanol de cada estado (Pendiente, Pagado, Entregado...).
 */

// paquete de los formateadores, los que hacen que todo se vea bonito
package com.agropalma.app.util

// imports pa' dar formato a números, fechas y usar el locale de Colombia
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// objeto con las funciones que dan formato a los textos de la app
object Fmt {
    // formato de dinero en pesos colombianos, listo pa' usar
    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        // sin centavos, 0 decimales pa' no marear a nadie
        maximumFractionDigits = 0
    }

    // formato de fecha cortito y legible: día, mes y hora
    private val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale("es", "CO"))

    // convierte los pesos a formato bonito con separador de miles y todo
    fun money(value: Double): String = moneyFormat.format(value)

    // muestra el número sin decimales si es entero, o con uno si no
    fun number(value: Double): String =
        // si no tiene decimales lo enseñamos pelado, sin el ",0"
        if (value % 1.0 == 0.0) value.toInt().toString()
        // y si tiene, nomás un decimal pa' no exagerar
        else String.format(Locale.US, "%.1f", value)

    // pasa un timestamp a fecha legible pa' que un humano la entienda
    fun date(timestamp: Long): String = dateFormat.format(Date(timestamp))

    // muestra latitud y longitud con 4 decimales, o "Sin GPS" si no hay datos
    fun coordinates(lat: Double?, lng: Double?): String =
        // sin coordenadas mostramos que no hay GPS, ahí nomás
        if (lat == null || lng == null) "Sin GPS"
        // si hay, las ponemos con 4 decimales que es preciso suficiente
        else String.format(Locale.US, "%.4f, %.4f", lat, lng)

    // traduce los estados en inglés a español bonito pa' que se entienda
    fun statusLabel(status: String): String = when (status) {
        // pendiente de que el dueño lo revise
        "PENDING" -> "Pendiente"
        // ya lo aprobó el dueño
        "APPROVED" -> "Aprobado"
        // ya le pagaron al trabajador
        "PAID" -> "Pagado"
        // ya entregaron la cosecha
        "DELIVERED" -> "Entregado"
        // el dueño compró el producto
        "BOUGHT" -> "Comprado"
        // si llega un estado raro lo dejamos igual, no inventamos
        else -> status
    }
}
