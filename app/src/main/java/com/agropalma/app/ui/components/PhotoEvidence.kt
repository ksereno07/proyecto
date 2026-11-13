/*
 * PhotoEvidence.kt - CAPTURA DE FOTOS
 * -----------------------------------
 * Componente que permite tomar una foto con la camara o elegirla de la
 * galeria. Muestra la vista previa y devuelve la ruta del archivo.
 * Se usa como evidencia obligatoria en los reportes de trabajo y tambien
 * en materiales y en el escaner de palma.
 */

// el package donde vive este archivo, junto a sus amigotes de components
package com.agropalma.app.ui.components

// imports de Android: el permiso de cámara y cómo chequearlo
import android.Manifest
import android.content.pm.PackageManager
// un chorro de imports de Compose, Material 3 y pa' lanzar cámara/galería
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// el chequeador de permisos, pa' saber si ya podemos abrir la cámara
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
// los colores palma de nuestro tema, los de siempre
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmOutline
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSurfaceVariant
import com.agropalma.app.util.PhotoStorage
import java.io.File

// composable de la UI, obvio
@Composable
// el widget pa' tomar o elegir la foto de evidencia, con su vista previa
fun PhotoEvidence(
    // la ruta de la foto actual (null si aún no hay) y el callback al cambiarla
    photoPath: String?,
    onPhotoPath: (String?) -> Unit,
    // el texto de arriba del recuadro y si la foto es obligatoria o no
    label: String = "Foto de evidencia",
    required: Boolean = false,
    modifier: Modifier = Modifier
) {
    // el context de Android, pa' guardar archivos y crear uris
    val context = LocalContext.current
    // Guarda la ruta del archivo que se abrira con la camara; cuando
    // Android confirma que la foto se tomo, esa ruta se envia al padre.
    var pendingCameraPath by remember { mutableStateOf<String?>(null) }

    // Selector de galeria del sistema (no requiere permisos de almacenamiento).
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoPath(PhotoStorage.saveFromUri(context, uri))
        }
    }

    // lanzador de la cámara del sistema, guarda la foto en la ruta pendiente
    val cameraLauncher = rememberLauncherForActivityResult(
        // el contrato de "toma una foto" de Android
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        // si sí se tomó la foto, devolvemos la ruta al padre
        if (saved) {
            onPhotoPath(pendingCameraPath)
        }
    }

    // lanzador que pide SOLO el permiso de cámara en el momento justo;
    // lo usamos de respaldo por si el usuario dijo "no gracias" en el
    // portón de permisos del inicio: sin él, abrir la cámara truena
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        // el contrato de pedir UN permiso, el clásico
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        // si al final sí lo concedió, abrimos la cámara de una vez
        if (concedido) {
            // creamos el archivo nuevo, guardamos su ruta y abrimos la cámara
            val file = PhotoStorage.newCameraFile(context)
            pendingCameraPath = file.absolutePath
            cameraLauncher.launch(PhotoStorage.uriFor(context, file))
        }
    }

    // columna principal: etiqueta, recuadro de la foto y botones
    Column(modifier = modifier.fillMaxWidth()) {
        // fila del título con los textos centrados verticalmente
        Row(verticalAlignment = Alignment.CenterVertically) {
            // el label, con asterisco si la foto es obligatoria
            Text(
                if (required) "$label *" else label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            // texto extra "Obligatoria" cuando la foto es requerida
            if (required) {
                Text("  Obligatoria", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
            }
        }
        // espaciador entre el título y el recuadro
        Spacer(Modifier.height(8.dp))
        // el recuadro grande donde se ve la foto (o el placeholder)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // altura fija de 190dp pa' que la foto se vea bien
                .height(190.dp),
            shape = RoundedCornerShape(18.dp),
            color = PalmSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, PalmOutline.copy(alpha = 0.5f))
        ) {
            // si ya hay foto guardada en disco, la mostramos
            if (photoPath != null && File(photoPath).exists()) {
                // AsyncImage de Coil carga la imagen sin bloquear la UI wey
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    // recorte pa' que la foto llene el recuadro sin deformarse
                    contentScale = ContentScale.Crop
                )
            } else {
                // si no hay foto, cajita centrada con icono y texto tristes
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // el icono de cámara en grisote
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = PalmMuted,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        // el mensajito de que aún no hay nada
                        Text("Aún no hay foto", style = MaterialTheme.typography.bodySmall, color = PalmMuted)
                    }
                }
            }
        }
        // espaciador antes de los botones de abajo
        Spacer(Modifier.height(10.dp))
        // fila con los dos botones, galería y cámara, repartidos a la mitad
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // botón outline pa' elegir una foto de la galería
            OutlinedButton(
                onClick = {
                    // lanzamos el selector de imágenes del sistema
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                // iconito de galería + su texto
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Galería")
            }
            // botón verde relleno pa' abrir la cámara
            androidx.compose.material3.Button(
                onClick = {
                    // OJO: desde que declaramos el permiso CAMERA en el
                    // manifest, hay que TENERLO concedido antes de abrir
                    // la cámara o Android nos tira SecurityException
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        // ya tenemos permiso: creamos el archivo nuevo,
                        // guardamos su ruta y abrimos la cámara directo
                        val file = PhotoStorage.newCameraFile(context)
                        pendingCameraPath = file.absolutePath
                        cameraLauncher.launch(PhotoStorage.uriFor(context, file))
                    } else {
                        // no lo tenemos (lo negó en el inicio): se lo pedimos
                        // aquí mismo y si acepta, la cámara abre solita
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                // iconito de cámara + su texto
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Tomar foto")
            }
        }
    }
}
