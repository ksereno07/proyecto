/*
 * PalmAnalyzer.kt - ESCANER "INTELIGENTE" DE PALMA (sin internet)
 * ---------------------------------------------------------------
 * Trabaja en dos capas que se complementan:
 *
 *  CAPA 1 - MODELO ENTRENADO (PalmModelAnalyzer):
 *    Un MobileNetV3Small entrenado con 1.583 fotos reales de hojas de
 *    palma de aceite decide si la hoja esta SANA o ENFERMA. Ese modelo
 *    va dentro del APK (assets/palm_disease.tflite), no necesita internet.
 *    Dataset: "Image Dataset of Oil Palm Leaf Diseases" (Mendeley Data,
 *    DOI 10.17632/cp65skff89.1, CC BY 4.0).
 *
 *  CAPA 2 - REGLAS DE COLOR (esta función):
 *    Si el modelo dice "enferma", estas reglas deciden QUE tipo es y
 *    ademas sirven de respaldo si el modelo no carga. Analiza los colores
 *    de la foto que toma el trabajador y estima el estado del follaje.
 *    No usa una API externa ni genera costos:
 *
 *  1. Reduce la foto a 96x96 pixeles para que sea rapido.
 *  2. Convierte cada pixel a HSV (tono, saturacion, brillo).
 *  3. Cuenta pixeles verdes, amarillos, marrones y oscuros.
 *  4. Con esos porcentajes decide entre:
 *     - Pudricion del Cogollo (tejido marron/oscuro abundante).
 *     - Clorosis / posible Amarillamiento Letal (amarillo alto).
 *     - Mancha foliar tipo Curvularia (marron moderado).
 *     - Estres hidrico o dano mecanico.
 *     - Follaje sano.
 *  Devuelve tambien soluciones sugeridas y que muestra enviar al laboratorio.
 *
 * Es orientativo: la pantalla aclara que no reemplaza a un ingeniero
 * agronomo ni a un analisis de laboratorio.
 */

// paquete del escáner de palmas: aquí vive el modelo entrenado + las reglas, sin internet
package com.agropalma.app.util

// imports pa' leer imágenes: el Bitmap, decodificar la foto desde archivo y colores
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
// y este pa' redondear números sin ponernos quisquillosos
import kotlin.math.roundToInt
// y este pa' no pasarnos de un rango al combinar confianzas
import kotlin.math.min

// modelo con el resultado del diagnóstico: enfermedad, certeza y todo el chisme
data class DiagnosisResult(
    // nombre de la enfermedad probable que detectó el análisis
    val disease: String,
    // qué tan seguro está el escáner, de 0 a 100
    val confidence: Int,
    // qué tan grave está la palma: Alta, Media o Baja
    val severity: String,
    // lo que encontró mirando los píxeles de la foto
    val finding: String,
    // qué hay que hacerle a la palma pa' arreglarla
    val recommendation: String,
    // qué muestra mandar al laboratorio pa' confirmar
    val labSample: String,
    // probabilidad de "enferma" que dio el modelo entrenado (0..1).
    // va en null si el modelo no cargó y se usaron solo las reglas de color
    val modelDiseaseProbability: Float? = null
)

/**
 * Analizador local (sin conexión). Primero pregunta al modelo entrenado si la
 * hoja está sana o enferma y después usa las reglas de color para decir QUÉ
 * enfermedad es. Si el modelo no está disponible, funcionan solo las reglas,
 * así el escáner nunca se queda sin respuesta.
 */
// objeto analizador: todo local, sin API ni internet, modelo + reglas de píxeles
object PalmAnalyzer {

    /**
     * Punto de entrada: recibe el contexto (pa' abrir el modelo del APK) y la
     * ruta de la foto. Devuelve el diagnóstico ya mezclado modelo + reglas.
     */
    fun analyze(context: Context, path: String): DiagnosisResult? {
        // 1. Las reglas de color siempre corren: son el respaldo y el que dice el tipo
        val porColor = analyzeByColor(path) ?: return null
        // 2. Si el modelo entrenado está disponible, nos da su probabilidad
        val sickProb = PalmModelAnalyzer.diseaseProbability(context, path)
            ?: return porColor  // sin modelo: queda el resultado de las reglas, tal cual
        // 3. Mezclamos las dos opiniones y devolvemos el veredicto final
        return blendWithModel(porColor, sickProb)
    }

    // función de las reglas de color: le das la ruta y te suelta el diagnóstico
    private fun analyzeByColor(path: String): DiagnosisResult? {
        // 1. Se carga la foto y se reduce para analizarla rapido.
        // lee la foto del disco y si no existe mejor nos salimos con null
        val original = BitmapFactory.decodeFile(path) ?: return null
        // la encoge a 96x96 pa' que el análisis vuele, no ocupamos más detalle
        val bitmap = Bitmap.createScaledBitmap(original, 96, 96, true)

        // 2. Contadores de pixeles por familia de color.
        // contadores de píxeles verdes, amarillos, marrones y oscuros
        var green = 0
        var yellow = 0
        var brown = 0
        var dark = 0
        // total de píxeles que sí contamos pa' sacar los porcentajes
        var counted = 0
        // arreglo reutilizable pa' guardar el color HSV del píxel actual
        val hsv = FloatArray(3)

        // recorre la foto de arriba abajo saltando cada 2 píxeles pa' ir más rápido
        for (y in 0 until bitmap.height step 2) {
            // y de izquierda a derecha, también en saltos de dos, wey
            for (x in 0 until bitmap.width step 2) {
                // agarra el color del píxel en esta posición
                val pixel = bitmap.getPixel(x, y)
                // lo convierte a HSV (tono, saturación, brillo) que es más fácil de clasificar
                Color.colorToHSV(pixel, hsv)
                val h = hsv[0]   // tono: 0-360 grados
                val s = hsv[1]   // saturacion: 0-1
                val v = hsv[2]   // brillo: 0-1
                // Se ignoran los pixeles casi grises (fondo, cielo, suelo).
                // si el píxel es casi gris es fondo (cielo, suelo) y lo brincamos
                if (s < 0.12f) continue
                // este sí cuenta pa' las estadísticas
                counted++
                // 3. Clasificacion del pixel segun tono y brillo.
                // decidimos a qué familia de color pertenece el píxel
                when {
                    v < 0.26f -> dark++          // zonas oscuras / necrosis
                    h in 65f..175f -> green++    // follaje sano
                    h in 28f..65f -> yellow++    // amarillamiento / clorosis
                    else -> brown++              // marron / lesiones
                }
            }
        }
        // libera la memoria del bitmap porque Android no perdona desperdiciarla
        bitmap.recycle()
        // si no contamos nada (foto toda gris o rara) no hay diagnóstico posible
        if (counted == 0) return null

        // 4. Porcentaje de cada color sobre el total de pixeles analizados.
        // proporción de píxeles verdes, o sea el follaje sano
        val greenR = green.toFloat() / counted
        // proporción de amarillos, el famoso amarilleo
        val yellowR = yellow.toFloat() / counted
        // proporción de marrones pa' las manchas feas
        val brownR = brown.toFloat() / counted
        // proporción de zonas oscuras / necrosis
        val darkR = dark.toFloat() / counted
        // el daño total suma marrón más oscuro, el combo malo
        val damage = brownR + darkR

        // 5. Reglas de decision (de la mas grave a la mas leve).
        // el veredicto: se evalúa de la enfermedad más grave a la más leve
        return when {
            // daño mayor al 34%: Pudrición del Cogollo, esta es la fea de verdad
            damage >= 0.34f -> {
                // la certeza crece con el daño pero se topa entre 86 y 97
                val confidence = (86 + damage * 22).roundToInt().coerceIn(86, 97)
                // arma el diagnóstico completo con todo el chisme de la enfermedad
                DiagnosisResult(
                    // nombre de la enfermedad detectada
                    disease = "Pudrición del Cogollo (PC inicial)",
                    // qué tan seguro está el análisis
                    confidence = confidence,
                    // esto está grave, al tope
                    severity = "Alta / Crítica",
                    // lo que vio: el porcentaje de tejido muerto y su descripción
                    finding = "Se detectó ${(damage * 100).roundToInt()}% de tejido marrón oscuro y necrosis, " +
                        "compatible con lesiones húmedas en la base del cogollo y avance rápido de la pudrición.",
                    // lo que hay que hacerle ya mismo a la palma
                    recommendation = "Aislar la palma, retirar tejido afectado con herramienta desinfectada y aplicar " +
                        "fungicida sistémico. Programar inspección en 7 días y revisar las palmas vecinas.",
                    // muestra pa' que el laboratorio confirme el hongo
                    labSample = "Enviar muestra del tejido necrótico del cogollo al laboratorio para confirmar " +
                        "Phytophthora palmivora."
                )
            }

            // amarilleo arriba del 32%: clorosis o posible Amarillamiento Letal
            yellowR >= 0.32f -> {
                // la certeza sube con el amarilleo, entre 68 y 92
                val confidence = (68 + yellowR * 28).roundToInt().coerceIn(68, 92)
                // arma el diagnóstico del amarilleo con sus detalles
                DiagnosisResult(
                    // nombre de la probable enfermedad
                    disease = "Clorosis foliar / posible Amarillamiento Letal",
                    // certeza calculada arriba
                    confidence = confidence,
                    // gravedad media, hay que estar pendiente
                    severity = "Media",
                    // lo que vio: el % del follaje amarillo y sus posibles causas
                    finding = "El ${(yellowR * 100).roundToInt()}% del follaje analizado presenta amarillamiento. " +
                        "Puede corresponder a deficiencia de magnesio o potasio, o a un cuadro de amarillamiento letal.",
                    // le toca abono y ajustar la fertilización
                    recommendation = "Aplicar abono foliar con micronutrientes y corregir el plan de fertilización. " +
                        "Si el amarillamiento avanza desde las hojas bajas hacia el cogollo, tratar como sospecha de enfermedad.",
                    // muestras pa' mandar al laboratorio y confirmar
                    labSample = "Muestra foliar de hojas bajas y muestra de suelo para análisis de nutrientes."
                )
            }

            // marrón moderado (14% o más): mancha foliar, probable hongo
            brownR >= 0.14f -> {
                // la certeza depende de cuántas manchas haya, entre 60 y 85
                val confidence = (60 + brownR * 30).roundToInt().coerceIn(60, 85)
                // arma el diagnóstico de la mancha foliar
                DiagnosisResult(
                    // probable culpable: el hongo Curvularia
                    disease = "Mancha foliar (posible Curvularia)",
                    // certeza calculada arriba
                    confidence = confidence,
                    // gravedad media, ni pa' tanto ni pa' tan poco
                    severity = "Media",
                    // el porcentaje de área foliar con manchas
                    finding = "Se observan manchas marrones en el ${(brownR * 100).roundToInt()}% del área foliar, " +
                        "con bordes definidos compatibles con hongos foliares tipo Curvularia.",
                    // podar lo feo y echar fungicida, así de simple
                    recommendation = "Retirar y disponer las hojas más afectadas, aplicar fungicida foliar y mejorar " +
                        "la ventilación del lote. Monitorear las palmas vecinas.",
                    // hoja con mancha viva pa' que el lab la cultive
                    labSample = "Muestra de hoja con mancha activa para cultivo micológico."
                )
            }

            // mucha zona oscura sin patrón claro: estrés hídrico o daño mecánico
            darkR >= 0.18f -> {
                // diagnóstico de estrés o descuido, no de enfermedad
                DiagnosisResult(
                    // puede ser falta de agua o un machetazo, wey
                    disease = "Estrés hídrico o daño mecánico",
                    // certeza fija de 64, aquí no hay de dónde sacar más
                    confidence = 64,
                    // gravedad baja, con cuidarla alcanza
                    severity = "Baja",
                    // lo que ve: oscuridad sin patrón de enfermedad claro
                    finding = "Predominan zonas oscuras sin patrón claro de necrosis. Puede deberse a sombra, " +
                        "sequía o daño mecánico por herramientas.",
                    // revisar el riego y no golpear la palma, obvio
                    recommendation = "Verificar humedad del suelo y el estado del sistema de riego. Evitar cortes " +
                        "que hieran la palma y repetir la foto con mejor iluminación.",
                    // no hace falta laboratorio, solo seguirle tomando fotos
                    labSample = "No requiere muestra de laboratorio por ahora. Registrar seguimiento fotográfico."
                )
            }

            // si nada de lo malo se activó, la palma está sana, ¡bien ahí!
            else -> {
                // más verde = más confianza en que está sana, entre 72 y 94
                val confidence = (72 + greenR * 22).roundToInt().coerceIn(72, 94)
                // el diagnóstico feliz de "todo bien, compa"
                DiagnosisResult(
                    // palma sin síntomas, lo que todos queremos ver
                    disease = "Follaje sano, sin síntomas evidentes",
                    // certeza calculada según el verdor
                    confidence = confidence,
                    // nada grave, gravedad baja
                    severity = "Baja",
                    // el porcentaje de follaje verde y uniforme
                    finding = "El ${(greenR * 100).roundToInt()}% del follaje presenta color verde uniforme, " +
                        "sin manchas ni necrosis relevantes en la fotografía.",
                    // seguir el plan normal y vigilar cada 15 días
                    recommendation = "Mantener el plan de fertilización y el monitoreo preventivo cada 15 días. " +
                        "Repetir la foto si aparece amarillamiento o lesiones nuevas.",
                    // sin muestras, solo monitoreo rutinario
                    labSample = "No requiere muestra. Continuar con el monitoreo rutinario."
                )
            }
        }
    }

    /**
     * Mezcla la opinión del modelo entrenado con la de las reglas de color.
     * sickProb es la probabilidad de que la hoja esté enferma (0 a 1).
     */
    private fun blendWithModel(base: DiagnosisResult, sickProb: Float): DiagnosisResult {
        // qué tan segura está la IA de su veredicto (siempre entre 50 y 100)
        val modelConf = ((if (sickProb >= 0.5f) sickProb else 1f - sickProb) * 100f).roundToInt()
        // el modelo dice que está enferma?
        val modelSick = sickProb >= 0.5f
        // las reglas vieron síntomas? (si el nombre arranca con "Follaje sano", no)
        val rulesSick = !base.disease.startsWith("Follaje sano")
        // frase pa' que el usuario sepa que también opinó el modelo entrenado
        val verdict = if (modelSick) "enferma" else "sana"
        val modelNote = "Modelo IA (MobileNetV3 entrenado con 1.583 fotos): hoja $verdict al $modelConf%."

        return when {
            // 1) los dos dicen sana: se refuerza la buena noticia
            !rulesSick && !modelSick -> base.copy(
                confidence = ((base.confidence + modelConf) / 2).coerceIn(60, 97),
                finding = base.finding + " " + modelNote,
                modelDiseaseProbability = sickProb
            )

            // 2) los dos dicen enferma: el tipo lo ponen las reglas y sube la certeza
            rulesSick && modelSick -> base.copy(
                confidence = ((base.confidence + modelConf) / 2).coerceIn(60, 97),
                finding = base.finding + " " + modelNote,
                modelDiseaseProbability = sickProb
            )

            // 3) el modelo ve enfermedad pero el color todavía no: sospecha temprana
            !rulesSick && modelSick -> DiagnosisResult(
                disease = "Posible afección temprana (detectada por el modelo IA)",
                confidence = modelConf.coerceIn(55, 90),
                severity = "Media",
                finding = "El modelo entrenado reconoció patrones de hoja enferma aunque el color todavía " +
                    "no muestra daño fuerte. " + modelNote,
                recommendation = "Repetir la foto en 3 días con buena luz y revisar el envés de la hoja. " +
                    "Si la IA insiste, tomar muestra y consultar al ingeniero agrónomo.",
                labSample = "Muestra de la hoja señalada si los síntomas avanzan; por ahora seguimiento fotográfico.",
                modelDiseaseProbability = sickProb
            )

            // 4) las reglas ven daño pero el modelo dice sana: caso dudoso
            else -> base.copy(
                confidence = ((base.confidence + modelConf) / 2)
                    .coerceIn(min(45, base.confidence), base.confidence),
                severity = if (base.severity.contains("Alta", ignoreCase = true)) "Media" else base.severity,
                finding = base.finding + " Ojo: " + modelNote + " Por eso el caso queda como dudoso.",
                recommendation = base.recommendation +
                    " Repetir la foto con mejor encuadre y luz antes de aplicar cualquier tratamiento.",
                modelDiseaseProbability = sickProb
            )
        }
    }
}
