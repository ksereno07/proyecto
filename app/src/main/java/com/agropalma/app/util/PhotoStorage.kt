/*
 * PhotoStorage.kt - ARCHIVOS DE FOTOS
 * -----------------------------------
 * Guarda las fotos de evidencia en la carpeta privada de la app y crea
 * las Uri seguras para abrir la camara del telefono mediante FileProvider.
 * saveFromUri copia una foto elegida de la galeria a nuestra carpeta.
 */

// paquete de la utilidad que guarda las fotos de evidencia de la app
package com.agropalma.app.util

// imports pa' el contexto de la app y las Uri de los archivos
import android.content.Context
import android.net.Uri
// FileProvider pa' compartir la foto con la cámara de forma segura, y File pa' rutas
import androidx.core.content.FileProvider
import java.io.File

// objeto único que guarda las fotos en la carpeta privada pa' que nadie las borre
object PhotoStorage {

    // crea el archivo nuevo donde va a caer la foto que saques con la cámara
    fun newCameraFile(context: Context): File {
        // busca la carpeta "photos" de la app, y si no hay usa la interna, plan B
        val dir = context.getExternalFilesDir("photos") ?: File(context.filesDir, "photos")
        // si la carpeta no existe la crea, porque Android no lo hace solo, pendejo
        if (!dir.exists()) dir.mkdirs()
        // nombre único con la hora actual pa' que ninguna foto pise a otra
        return File(dir, "foto_${System.currentTimeMillis()}.jpg")
    }

    // convierte el archivo en una Uri segura pa' que la cámara pueda escribir en él
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    // copia una foto de la galería a nuestra carpeta privada pa' que no se borre solita
    fun saveFromUri(context: Context, uri: Uri): String? {
        // si algo sale mal (foto inválida o lo que sea) regresamos null y a otra cosa
        return try {
            // misma carpeta de fotos de siempre, la externa si se puede o la interna si no
            val dir = context.getExternalFilesDir("photos") ?: File(context.filesDir, "photos")
            // crea la carpeta si no existe, no la cagues con rutas raras
            if (!dir.exists()) dir.mkdirs()
            // archivo destino con nombre único por hora, pa' no repetir fotos
            val file = File(dir, "foto_${System.currentTimeMillis()}.jpg")
            // abre la foto de la galería y la copia a nuestro archivo byte por byte
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            // devolvemos la ruta completa donde quedó guardada la foto
            file.absolutePath
        } catch (e: Exception) {
            // cualquier desmadre aquí significa que no se pudo, devolvemos null
            null
        }
    }
}
