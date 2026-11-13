/*
 * ScannerScreen.kt - ESCANER DE PALMA ENFERMA
 * -------------------------------------------
 * El trabajador toma o sube una foto de la hoja/racimo/cogollo. La app
 * la analiza localmente con PalmAnalyzer (modelo entrenado + reglas) y muestra:
 *  - Posible enfermedad y porcentaje de certeza.
 *  - Hallazgo visual y soluciones sugeridas.
 *  - Que muestra enviar al laboratorio.
 * El diagnostico queda guardado y el dueno lo ve en su panel.
 */

// paquete de la pantalla del escáner de palmas enfermas
package com.agropalma.app.ui.screens.worker

// imports de Compose: layout, botones, iconos, chips y barras de progreso
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// imports de AgroPalma: el VM, componentes, tema y el resultado del análisis
import androidx.navigation.NavController
import com.agropalma.app.AppViewModel
import com.agropalma.app.ui.components.AgroCard
import com.agropalma.app.ui.components.AgroTopBar
import com.agropalma.app.ui.components.PhotoEvidence
import com.agropalma.app.ui.components.SectionTitle
import com.agropalma.app.ui.theme.PalmCream
import com.agropalma.app.ui.theme.PalmError
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSuccess
import com.agropalma.app.ui.theme.PalmTertiary
import com.agropalma.app.ui.theme.PalmWarning
import com.agropalma.app.util.DiagnosisResult
// pa' redondear el porcentaje del modelo entrenado
import kotlin.math.roundToInt

// OptIn porque el FlowRow de los chips sigue siendo beta
@OptIn(ExperimentalLayoutApi::class)
// pantalla del escáner: le tomas foto a la palma y la IA te dice qué bronca tiene
@Composable
fun ScannerScreen(vm: AppViewModel, nav: NavController, parcelId: Long) {
    // la ruta de la foto de la palma
    var photoPath by remember { mutableStateOf<String?>(null) }
    // aquí cae el resultado del análisis con la enfermedad y todo
    var result by remember { mutableStateOf<DiagnosisResult?>(null) }
    // bandera pa' saber si la IA está pensando
    var analyzing by remember { mutableStateOf(false) }
    // pa' guardar el error si algo truina
    var error by remember { mutableStateOf<String?>(null) }
    // qué parte de la palma vas a analizar
    var mode by remember { mutableStateOf("Hojas / Fronda") }

    // cargamos los datos de la parcela si no estaba activa
    LaunchedEffect(parcelId) {
        if (vm.activeParcel?.id != parcelId) vm.loadParcelData(parcelId)
    }

    // andamiaje con fondo crema y su barra de arriba
    Scaffold(
        containerColor = PalmCream,
        topBar = {
            AgroTopBar(
                "Escáner Agronómico",
                vm.activeParcel?.name ?: "Diagnóstico de la palma",
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        // columna scrolleable con todo el contenido
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // banner verde del visor IA, puro estilo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PalmPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Science, contentDescription = null, tint = PalmTertiary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Visor IA AgroPalma", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        // le avisamos que el análisis es local y que usa el modelo entrenado
                        Text(
                            "Modelo entrenado + análisis local, sin consumir datos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    // pastilla de "Local" porque el modelo va dentro del APK
                    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
                        Text(
                            "Local",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // tarjeta de instrucciones pa' que la foto no salga chueca
            AgroCard {
                Text("Encuadre la fronda", style = MaterialTheme.typography.titleMedium)
                // tips de cómo tomar la foto bien
                Text(
                    "Acérquese a 30 cm de la hoja afectada, con buena luz y sin sombra. " +
                        "Entre más clara la foto, mejor el diagnóstico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PalmMuted
                )
                Spacer(Modifier.height(10.dp))
                // chips pa' elegir qué parte de la palma vas a analizar
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // los tres modos: hojas, racimo o cogollo
                    listOf("Hojas / Fronda", "Racimo / Fruto", "Cogollo Central").forEach { label ->
                        FilterChip(
                            // el chip marcado es el modo actual
                            selected = mode == label,
                            onClick = { mode = label },
                            label = { Text(label) },
                            // colores del chip seleccionado
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PalmPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // aquí se toma o sube la foto de la palma, obligatoria pa' analizar
            PhotoEvidence(
                photoPath = photoPath,
                // al cambiar la foto borramos resultado y error pa' empezar limpio
                onPhotoPath = { photoPath = it; result = null; error = null },
                label = "Foto de la palma ($mode)",
                required = true
            )

            // caja roja del error si algo salió mal
            if (error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        error!!,
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // botón de analizar, solo aparece si aún no hay resultado
            if (result == null) {
                Button(
                    onClick = {
                        // sin foto no hay análisis wey
                        if (photoPath == null) {
                            error = "Toma o sube una foto de la palma para analizarla."
                        } else {
                            // marcamos que la IA está pensando y limpiamos errores
                            analyzing = true
                            error = null
                            // el VM analiza la foto localmente y guarda el diagnóstico
                            vm.analyzeAndSaveDiagnosis(parcelId, photoPath!!) { diagnosis ->
                                analyzing = false
                                // si no pudo leer la imagen, se lo avisamos
                                if (diagnosis == null) {
                                    error = "No se pudo leer la imagen. Intenta con otra foto más nítida."
                                } else {
                                    // si sí salió, guardamos el resultado pa' mostrarlo
                                    result = diagnosis
                                }
                            }
                        }
                    },
                    // deshabilitado mientras analiza o el VM está ocupado
                    enabled = !analyzing && !vm.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    // mientras piensa, mostramos un loader chiquito
                    if (analyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.size(10.dp))
                        Text("Analizando imagen…")
                    } else {
                        // si no, el botón normal con su iconito de ciencia
                        Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Analizar con IA", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // cuando ya hay resultado, mostramos la tarjeta del diagnóstico
            if (result != null) {
                DiagnosisCard(result!!)
                // botón pa' escanear otra palma, borra todo y empiezas de cero
                OutlinedButton(
                    onClick = {
                        result = null
                        photoPath = null
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Escanear otra palma")
                }
            }

            // disclaimer de que la IA orienta pero no reemplaza al ingeniero
            if (result == null) {
                AgroCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = PalmMuted)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "El resultado es orientativo: apoya la decisión en campo, pero no reemplaza " +
                                "el concepto de un ingeniero agrónomo ni un análisis de laboratorio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalmMuted
                        )
                    }
                }
            }
        }
    }
}

// composable que dibuja la tarjeta grande con el diagnóstico completo
@Composable
private fun DiagnosisCard(result: DiagnosisResult) {
    // checamos si la gravedad es alta pa' pintar todo rojo
    val isCritical = result.severity.contains("Alta", ignoreCase = true)
    // el color según la gravedad: rojo, amarillo o verde
    val accent = when {
        isCritical -> PalmError
        result.severity.contains("Media", ignoreCase = true) -> PalmWarning
        else -> PalmSuccess
    }

    // columna con todas las tarjetas del resultado
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // tarjeta principal: nombre de la enfermedad y certeza de la IA
        AgroCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent)
                Spacer(Modifier.size(8.dp))
                // aviso de que el diagnóstico ya quedó guardado
                Text("Diagnóstico guardado", style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.weight(1f))
                // pastilla con la gravedad (Alta, Media o Baja)
                Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        result.severity,
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // el nombre de la enfermedad en grande
            Text(result.disease, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Certeza", style = MaterialTheme.typography.bodySmall, color = PalmMuted, modifier = Modifier.weight(1f))
                // el porcentaje de certeza que calculó la IA
                Text("${result.confidence}%", style = MaterialTheme.typography.titleMedium, color = accent)
            }
            Spacer(Modifier.height(6.dp))
            // barrita de progreso con la certeza del análisis
            LinearProgressIndicator(
                progress = { result.confidence / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            // si el modelo entrenado dio su opinión, la mostramos aparte
            if (result.modelDiseaseProbability != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Science, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Modelo IA: hoja enferma al ${(result.modelDiseaseProbability * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = PalmMuted
                    )
                }
            }
        }

        // tarjeta con lo que la IA encontró en la foto
        AgroCard {
            SectionTitle("Hallazgo visual")
            Spacer(Modifier.height(6.dp))
            Text(result.finding, style = MaterialTheme.typography.bodyMedium)
        }

        // tarjeta de posibles soluciones pa' curar la palma
        AgroCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = PalmPrimary)
                Spacer(Modifier.size(8.dp))
                Text("Posibles soluciones", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(result.recommendation, style = MaterialTheme.typography.bodyMedium)
        }

        // tarjeta con la muestra que hay que mandar al laboratorio
        AgroCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Science, contentDescription = null, tint = PalmTertiary)
                Spacer(Modifier.size(8.dp))
                Text("Qué enviar a analizar", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(result.labSample, style = MaterialTheme.typography.bodyMedium)
        }

        // aviso final: mejor confírmalo con el ingeniero antes de gastar en tratamientos
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Diagnóstico orientativo generado en el teléfono con un modelo de IA entrenado con " +
                    "el dataset público \"Image Dataset of Oil Palm Leaf Diseases\" (Mendeley Data, " +
                    "DOI 10.17632/cp65skff89.1, CC BY 4.0). Confírmelo con un ingeniero agrónomo " +
                    "o con el laboratorio antes de aplicar tratamientos costosos.",
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5A3B00)
            )
        }
    }
}
