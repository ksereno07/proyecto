/*
 * Common.kt - COMPONENTES VISUALES REUTILIZABLES
 * ----------------------------------------------
 * Pequenas piezas de interfaz que se usan en muchas pantallas:
 *  - AgroTopBar: barra superior verde con titulo y boton de volver.
 *  - AgroBottomBar: menu inferior con iconos (Inicio, Reportes, etc.).
 *  - AgroCard: tarjeta blanca redondeada con borde suave.
 *  - AgroField: campo de texto con icono y estilo de la app.
 *  - StatTile, StatusPill, SectionTitle, EmptyState, LabelValue, HDivider.
 * Tenerlas aqui evita repetir el mismo diseno en cada pantalla.
 */

// el package: aquí vive este archivo dentro de la app, wey
package com.agropalma.app.ui.components

// un chorro de imports de Compose y Material 3 pa' armar toda la UI
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// los colores de la paleta palma que usamos en todos lados
import com.agropalma.app.ui.theme.PalmMuted
import com.agropalma.app.ui.theme.PalmOutline
import com.agropalma.app.ui.theme.PalmPrimary
import com.agropalma.app.ui.theme.PalmSurface
import com.agropalma.app.ui.theme.PalmSurfaceVariant

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: AgroTopBar — Barra superior verde
//  Propósito: título de la pantalla + botón "Volver" + acciones extra.
// ═══════════════════════════════════════════════════════════════════
// marcamos que usamos APIs experimentales del Material 3, no la cagues sin esto
@OptIn(ExperimentalMaterial3Api::class)
// esto es un componente de la UI, o sea un composable
@Composable
// la barra verde de arriba que sale en TODAS las pantallas wey
fun AgroTopBar(
    // el título grande y su subtitulo opcional, por si hay que dar contexto
    title: String,
    subtitle: String? = null,
    // qué hacer al picar la flechita de volver; si es null no aparece
    onBack: (() -> Unit)? = null,
    // acciones extra que van a la derecha, ahí nomás vacío por defecto
    actions: @Composable () -> Unit = {}
) {
    // la barra como tal, la de Material 3
    TopAppBar(
        // el título va en una columna por si trae subtitulo debajo
        title = {
            Column {
                // el texto del título, bien grande pa' que se vea de lejos
                Text(title, style = MaterialTheme.typography.titleLarge)
                // y solo si hay subtitulo lo pintamos abajo del título
                if (subtitle != null) {
                    // subtitulo en blanquito semitransparente pa' que no estorbe
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        },
        // la flechita de volver que va a la izquierda
        navigationIcon = {
            // nomás sale el botón si nos pasaron la acción de volver
            if (onBack != null) {
                // botón picable que dispara el volver
                IconButton(onClick = onBack) {
                    // la flechita que apunta pa' atrás, obvio
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        // las acciones extra que nos mandaron, ahí se muestran
        actions = { actions() },
        // aquí viene el look verde de la app, el detalle importante
        colors = TopAppBarDefaults.topAppBarColors(
            // fondo verde palma oscuro, el color insignia wey
            containerColor = PalmPrimary,
            // y todo lo demás (título e iconos) en blanco pa' contraste
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

// modelito con los datos de cada botón del menú: su texto y su iconito
data class BottomItem(val label: String, val icon: ImageVector)

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: AgroBottomBar — Menú inferior con iconos
//  Propósito: navegación rápida entre Inicio / Reportes / Costos / Perfil.
// ═══════════════════════════════════════════════════════════════════
// composable de la UI wey
@Composable
// el menú de abajo: Inicio, Reportes, Costos, Perfil, todo el chiste de navegar
fun AgroBottomBar(items: List<BottomItem>, selected: Int, onSelect: (Int) -> Unit) {
    // la barra de navegación con fondo casi blanco y una sombrita sutil
    NavigationBar(containerColor = PalmSurface, tonalElevation = 4.dp) {
        // recorremos cada botón del menú junto con su posición
        items.forEachIndexed { index, item ->
            // cada botoncito de la barra
            NavigationBarItem(
                // marcado como seleccionado si su posición coincide con la actual
                selected = selected == index,
                // al picarlo avisamos al padre qué tab se eligió
                onClick = { onSelect(index) },
                // el iconito del botón, 24dp nomás pa' que no ocupe mucho
                icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp)) },
                // el texto que va debajo del icono
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                // los colores del botón: verde si está activo, grisote si no
                colors = NavigationBarItemDefaults.colors(
                    // icono y texto seleccionados en verde palma
                    selectedIconColor = PalmPrimary,
                    selectedTextColor = PalmPrimary,
                    // la pastillita de atrás del icono activo
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    // y los no seleccionados en gris apagado
                    unselectedIconColor = PalmMuted,
                    unselectedTextColor = PalmMuted
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: SectionTitle — Encabezado de bloque
//  Propósito: título grande + subtítulo opcional dentro de una pantalla.
// ═══════════════════════════════════════════════════════════════════
// composable de la UI, como todos por aquí
@Composable
// el título de cada bloque dentro de la pantalla, con su subtitulo opcional
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    // columna que ocupa todo el ancho disponible
    Column(modifier = modifier.fillMaxWidth()) {
        // el título en grande con el color del fondo
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        // y el subtitulo abajo, en grisote, si es que existe
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = PalmMuted
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: AgroCard — Tarjeta blanca redondeada
//  Propósito: contenedor estándar para todos los bloques de contenido.
// ═══════════════════════════════════════════════════════════════════
// composable de la UI wey
@Composable
// la tarjeta blanca redondeada que se usa en TODOS lados pa' agrupar contenido
fun AgroCard(
    modifier: Modifier = Modifier,
    // lo que va adentro de la tarjeta: aquí nomás pasas el contenido y listo
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    // la tarjeta de Material 3 como tal
    Card(
        // ocupa todo el ancho disponible
        modifier = modifier.fillMaxWidth(),
        // esquinas bien redonditas, 20dp, pa' que se vea suave
        shape = RoundedCornerShape(20.dp),
        // fondo blanco cálido de la app
        colors = CardDefaults.cardColors(containerColor = PalmSurface),
        // bordesito suave casi invisible
        border = BorderStroke(1.dp, PalmOutline.copy(alpha = 0.35f)),
        // sin sombra, planita; así se diseñó y no la cagues
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // el contenido va en columna con padding pa' que respire
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: StatTile — Tarjeta de totales del panel
//  Propósito: número grande + etiqueta (ej. "$ 0 / Por pagar").
// ═══════════════════════════════════════════════════════════════════
// composable de UI, como todos por acá
@Composable
// la tarjetita de totales del panel: número grande arriba + su etiqueta
fun StatTile(
    // iconito, etiqueta y el valor que se muestra grandote
    icon: ImageVector,
    label: String,
    value: String,
    // colores personalizables: el fondo y el color del contenido
    containerColor: Color = PalmSurfaceVariant,
    contentColor: Color = PalmPrimary,
    modifier: Modifier = Modifier
) {
    // tarjeta con esquinas redonditas
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        // el fondo elegido y cero sombra, planita
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // columna con padding pa' ordenar icono, valor y etiqueta
        Column(Modifier.padding(14.dp)) {
            // el icono arriba, con el color del contenido
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
            // espaciador pa' separar el icono del número
            Spacer(Modifier.height(10.dp))
            // el valor bien grande y en negritas pa' que impacte
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            // la etiqueta abajo del número, un poquito transparente
            Text(label, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: StatusPill — Etiqueta de estado
//  Propósito: pastilla de color según el estado (Pendiente / Pagado /
//  Entregado) de un reporte o material.
// ═══════════════════════════════════════════════════════════════════
// composable de la UI wey
@Composable
// la pastilla redondita de estado: verde si ya está pagado, mostaza si va pendiente
fun StatusPill(status: String) {
    // aquí escogemos los colores (fondo y texto) según el estado que llegue
    val (container, content) = when (status) {
        // pagado o entregado: pastilla verde clarito con texto verde palma
        "PAID", "DELIVERED" -> MaterialTheme.colorScheme.primaryContainer to PalmPrimary
        // aprobado o comprado: otro tono de verde, ahí nomás
        "APPROVED", "BOUGHT" -> Color(0xFFDCEBDF) to Color(0xFF24523A)
        // pendiente: mostaza clarito, o sea "espérate tantito"
        "PENDING" -> MaterialTheme.colorScheme.tertiaryContainer to Color(0xFF6B4A12)
        // cualquier otro estado raro: gris apagado
        else -> PalmSurfaceVariant to PalmMuted
    }
    // la superficie en forma de píldora (esquinas de 999dp = redonda redonda)
    Surface(shape = RoundedCornerShape(999.dp), color = container) {
        Text(
            // traducimos el estado a texto bonito en español
            com.agropalma.app.util.Fmt.statusLabel(status),
            // con su padding pa' que respire dentro de la pastilla
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: AgroField — Campo de texto del formulario
//  Propósito: entrada con icono, etiqueta flotante y estilo de la app.
// ═══════════════════════════════════════════════════════════════════
// composable de la UI, qué más
@Composable
// el campo de texto de los formularios, con su iconito al lado, bien bonito
fun AgroField(
    // el texto actual y el callback pa' cuando el user teclea algo
    value: String,
    onValueChange: (String) -> Unit,
    // la etiqueta flotante que sale arriba del campo
    label: String,
    modifier: Modifier = Modifier,
    // el textito de ejemplo cuando el campo está vacío
    placeholder: String? = null,
    // icono al inicio y contenido al final del campo, ambos opcionales
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    // tipo de teclado: numérico pa' números, texto normal pa' lo demás
    keyboardType: KeyboardType = KeyboardType.Text,
    // pa' cosas como contraseñas (puntitos en vez de letras); aquí normal
    visualTransformation: VisualTransformation = VisualTransformation.None,
    // una sola línea por defecto, o más si pides minLines mayor
    singleLine: Boolean = true,
    minLines: Int = 1,
    // texto de ayuda o error que sale abajo del campo
    supportingText: String? = null
) {
    // el campo outlined de Material 3 con TODO el estilo de la app
    OutlinedTextField(
        // el texto y su callback, directo del estado del padre
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        // la etiqueta flotante y el placeholder de ejemplo
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        // el iconito del inicio, en gris apagado pa' que no gane atención
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = PalmMuted) }
        },
        trailingIcon = trailingIcon,
        // teclado y transformación según lo que pidieron
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        // líneas y forma: una línea por defecto, esquinas suaves
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        // texto de apoyo abajo del campo, si lo mandaron
        supportingText = supportingText?.let { { Text(it) } },
        // los colores del campo: verde al enfocar, línea suave si no
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PalmPrimary,
            unfocusedBorderColor = PalmOutline,
            focusedLabelColor = PalmPrimary,
            cursorColor = PalmPrimary,
            // el fondo blanco cálido, siempre
            focusedContainerColor = PalmSurface,
            unfocusedContainerColor = PalmSurface
        )
    )
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: EmptyState — Estado vacío
//  Propósito: mensaje amable con icono cuando no hay datos todavía
//  (ej. "Comparte el código para que tu primer trabajador se una").
// ═══════════════════════════════════════════════════════════════════
// composable de la UI como los demás
@Composable
// lo que se muestra cuando NO hay nada: icono, mensajito y botón opcional
fun EmptyState(
    // el iconito, el título y el mensajito pa' consolar al user
    icon: ImageVector,
    title: String,
    message: String,
    // botón de acción opcional: su texto y qué hace al picarlo
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // columna con todo centradito y espacio entre elementos
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        // centrados al centro, como debe ser
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // la cajita redondeada de atrás del icono
        Box(
            modifier = Modifier
                .size(64.dp)
                // fondo verde clarito con esquinas redonditas
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // el icono en verde palma, mediano
            Icon(icon, contentDescription = null, tint = PalmPrimary, modifier = Modifier.size(30.dp))
        }
        // el título del estado vacío, centrado como todo aquí
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        // el mensajito explicativo, en grisote y centrado también
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = PalmMuted
        )
        // si mandaron botón (texto + acción) lo pintamos aquí abajo
        if (actionLabel != null && onAction != null) {
            // botón verde estándar con esquinas suaves
            androidx.compose.material3.Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: LabelValue — Fila "etiqueta → valor"
//  Propósito: pares de datos como "Celular → 3001234567" en el perfil.
// ═══════════════════════════════════════════════════════════════════
// composable de la UI wey
@Composable
// la fila "etiqueta → valor" tipo "Celular: 3001234567" del perfil
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    // fila que empuja la etiqueta a la izquierda y el valor a la derecha
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // la etiqueta en gris apagado
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PalmMuted)
        // el valor en seminegritas pa' que se note más
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: HDivider — Línea divisoria horizontal
// ═══════════════════════════════════════════════════════════════════
// composable de la UI, pa' separar cositas
@Composable
// la línea divisoria horizontal sutil, separa bloques sin dramatismo
fun HDivider(modifier: Modifier = Modifier) {
    // un spacer flaco de 1dp pintado con el color del borde bien tenue
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PalmOutline.copy(alpha = 0.3f))
    )
}

// ═══════════════════════════════════════════════════════════════════
//  COMPONENTE: IconBadge — Icono dentro de un círculo
//  Propósito: icono destacado (ej. el logo de la parcela activa).
// ═══════════════════════════════════════════════════════════════════
// composable de la UI, el último de la lista wey
@Composable
// el icono adentro de su cajita redondeada, tipo el logo de la parcela
fun IconBadge(icon: ImageVector, tint: Color = PalmPrimary, background: Color = PalmSurfaceVariant) {
    // caja de 42dp con fondo redondeadito
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(background, RoundedCornerShape(13.dp)),
        // con el icono clavado al centro
        contentAlignment = Alignment.Center
    ) {
        // el icono pintado con su color, 22dp pa' que quepa bonito
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}
