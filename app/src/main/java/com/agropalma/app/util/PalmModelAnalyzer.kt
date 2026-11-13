/*
 * PalmModelAnalyzer.kt - EL MODELO DE IA DE VERDAD (offline)
 * ----------------------------------------------------------
 * Aquí vive el modelo entrenado con TensorFlow y metido dentro del APK
 * como `palm_disease.tflite`. A diferencia de las reglas de color, este
 * modelo SÍ aprendió de fotos reales:
 *
 *  - Dataset: "Image Dataset of Oil Palm Leaf Diseases" (1.583 fotos).
 *  - Autores: Oky Rahmanto, Veri Julianto, Ahmad Rusadi Arrahimi (2025).
 *  - DOI: 10.17632/cp65skff89.1 - Licencia CC BY 4.0.
 *  - Arquitectura: MobileNetV3Small con transfer learning (ImageNet).
 *
 * El modelo solo responde una cosa: ¿la hoja está sana o enferma?
 * El TIPO de enfermedad lo sigue decidiendo PalmAnalyzer con las reglas
 * de color. Si el modelo no carga (celular viejo, archivo raro), todo
 * sigue funcionando con las reglas de siempre.
 */

// paquete de utilidades, igual que el analizador de colores
package com.agropalma.app.util

// contexto pa' abrir el archivo del modelo que va dentro del APK
import android.content.Context
// Bitmap y su fábrica pa' leer y encoger la foto
import android.graphics.Bitmap
import android.graphics.BitmapFactory
// el intérprete de TensorFlow Lite, que corre el modelo sin internet
import org.tensorflow.lite.Interpreter
// buffers pa' meterle los píxeles al modelo
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Carga el modelo `.tflite` una sola vez y devuelve la probabilidad de que
 * la hoja de la foto esté enferma. Todo local: no hay red ni servidor.
 */
object PalmModelAnalyzer {

    // nombre del archivo que va en app/src/main/assets/
    private const val MODEL_FILE = "palm_disease.tflite"
    // el modelo entrenó con fotos de 224x224, hay que respetarlo
    private const val SIZE = 224

    // el intérprete se guarda pa' no cargarlo en cada foto (tarda un tris)
    @Volatile
    private var interpreter: Interpreter? = null
    // si ya falló una vez, no seguimos intentando ni llenando el log
    @Volatile
    private var failed = false

    /**
     * Devuelve la probabilidad de "enferma" entre 0 y 1, o null si el modelo
     * no se pudo cargar o la foto no se pudo leer.
     */
    fun diseaseProbability(context: Context, photoPath: String): Float? {
        // primero el modelo; si no hay, chao, el que llama usa las reglas
        val engine = getInterpreter(context) ?: return null
        // leemos la foto ya encogida a 224x224 pa' no reventar la memoria
        val bitmap = decodeScaled(photoPath) ?: return null
        return try {
            // pasamos los píxeles al buffer de entrada del modelo
            val input = toInputBuffer(bitmap)
            // la salida es un solo número: probabilidad de enferma
            val output = Array(1) { FloatArray(1) }
            // ¡corre el modelo! (esto es lo que de verdad es "IA")
            engine.run(input, output)
            // lo devolvemos simple, sin adornos
            output[0][0]
        } catch (t: Throwable) {
            // si algo raro pasa, que no se caiga la app: sin modelo, con reglas
            null
        } finally {
            // liberamos la foto chiquita
            bitmap.recycle()
        }
    }

    // carga perezosa del modelo: la primera foto lo carga, luego queda listo
    @Synchronized
    private fun getInterpreter(context: Context): Interpreter? {
        // si ya está cargado, pa' afuera rápido
        interpreter?.let { return it }
        // si ya sabemos que falla, ni lo intentes
        if (failed) return null
        return try {
            // el .tflite vive dentro de los assets del APK
            val bytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
            // TensorFlow Lite exige un buffer directo (memoria nativa)
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            // 2 hilos es buen equilibrio entre velocidad y batería
            val options = Interpreter.Options().apply { setNumThreads(2) }
            // crea el intérprete y lo deja guardado
            Interpreter(buffer, options).also { interpreter = it }
        } catch (t: Throwable) {
            // modelo ausente o incompatible: marcamos fallo y seguimos con reglas
            failed = true
            null
        }
    }

    // lee la foto del disco y la deja de 224x224, igual que en el entrenamiento
    private fun decodeScaled(path: String): Bitmap? {
        // primero medimos la foto sin cargarla entera (pa' no llenar la RAM)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        // si no hay dimensiones, el archivo no sirve
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // bajamos la foto por potencias de 2 hasta quedar cerca de 224
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / 2 >= SIZE && h / 2 >= SIZE) {
            w /= 2
            h /= 2
            sample *= 2
        }

        // ahora sí decodificamos ya reducida
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        // y la dejamos exactamente en 224x224 con suavizado
        return Bitmap.createScaledBitmap(decoded, SIZE, SIZE, true).also {
            // si el bitmap escalado es otro, liberamos el original
            if (it !== decoded) decoded.recycle()
        }
    }

    // mete los píxeles RGB (0..255) en el buffer float que espera el modelo
    private fun toInputBuffer(bitmap: Bitmap): ByteBuffer {
        // saca todos los píxeles de una sola pasada, es más rápido
        val pixels = IntArray(SIZE * SIZE)
        bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        // 1 foto x 224 x 224 x 3 canales x 4 bytes por float
        val buffer = ByteBuffer.allocateDirect(1 * SIZE * SIZE * 3 * 4).order(ByteOrder.nativeOrder())
        // el modelo normaliza adentro, así que mandamos 0..255 tal cual
        for (p in pixels) {
            buffer.putFloat(((p shr 16) and 0xFF).toFloat())  // rojo
            buffer.putFloat(((p shr 8) and 0xFF).toFloat())   // verde
            buffer.putFloat((p and 0xFF).toFloat())           // azul
        }
        // dejamos el cursor al inicio pa' que el modelo lea desde el pixel 0
        buffer.rewind()
        return buffer
    }
}
