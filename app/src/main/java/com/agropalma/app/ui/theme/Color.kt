/*
 * Color.kt - PALETA DE COLORES (CLARA Y OSCURA)
 * ---------------------------------------------
 * Aqui viven los colores de la app. Antes eran valores fijos; ahora cada
 * nombre (PalmPrimary, PalmCream, etc.) es un "getter" que lee la paleta
 * ACTIVA segun el modo claro u oscuro. Asi TODAS las pantallas se adaptan
 * solas sin tocar una sola linea de ellas, wey.
 *
 * Como funciona:
 *  - PalmPalette: data class con los 14 colores del look AgroPalma.
 *  - PalmLightPalette / PalmDarkPalette: las dos paletas crudas.
 *  - LocalPalmPalette: el CompositionLocal que el tema prende segun el modo.
 *  - PalmPrimary, PalmCream...: getters @Composable que leen la paleta
 *    activa. En modo claro valen lo mismo que siempre (cero cambio visual).
 */

// el package del tema, aquí viven los colores de la app
package com.agropalma.app.ui.theme

// el tipo Color de Compose, pa' declarar los valores hexadecimalitos
import androidx.compose.ui.graphics.Color
// Composable pa' los getters que se adaptan al modo oscuro
import androidx.compose.runtime.Composable
// el CompositionLocal estático: el tema le inyecta la paleta activa
import androidx.compose.runtime.staticCompositionLocalOf

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: LA PALETA COMO OBJETO
//  Propósito: agrupar los 14 colores del look en una sola pieza, pa'
//  poder cambiar TODA la personalidad de la app de un golpe (modo oscuro).
// ═══════════════════════════════════════════════════════════════════
// la forma de la paleta: cada campo es uno de los colores de siempre
data class PalmPalette(
    // el verde insignia: barras, botones y acentos
    val primary: Color,
    // el verde más oscuro: gradientes y texto fuerte
    val primaryDark: Color,
    // el verde medio: acciones secundarias y el "todo salió chido"
    val secondary: Color,
    // el mostaza: acentos, pendientes y el escáner
    val tertiary: Color,
    // el fondo general de las pantallas (crema de día, casi negro de noche)
    val cream: Color,
    // las tarjetas y campos (blanco cálido de día, verde oscuro de noche)
    val surface: Color,
    // fondos secundarios y pastillas
    val surfaceVariant: Color,
    // el texto principal sobre fondo/superficie
    val onSurface: Color,
    // el texto secundario, apagadito
    val muted: Color,
    // bordes y líneas divisorias
    val outline: Color,
    // el rojo de error, pa' cuando el user la caga
    val error: Color,
    // verde de éxito, o sea "ya quedó"
    val success: Color,
    // mostaza oscuro pa' avisos tipo "espérate tantito"
    val warning: Color,
    // azulito pa' info, el único que no es verde
    val info: Color
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: PALETA CLARA (la de toda la vida)
//  Es EXACTAMENTE la que ya usaba la app, ni un tono cambió.
// ═══════════════════════════════════════════════════════════════════
val PalmLightPalette = PalmPalette(
    // el verde palma oscuro, el color insignia de la app wey
    primary = Color(0xFF1B4332),
    // el mismo verde pero más oscuro, pa' contenedores y texto fuerte
    primaryDark = Color(0xFF123024),
    // el verde medio, pa' acciones secundarias
    secondary = Color(0xFF2D6A4F),
    // el mostaza, ese amarillito pa' acentos y pendientes
    tertiary = Color(0xFFE09F3E),
    // el crema, el fondo general de las pantallas
    cream = Color(0xFFF7F4EA),
    // el blanco cálido de las tarjetas, NO blanco puro pendejo
    surface = Color(0xFFFFFDF7),
    // el beige clarito pa' fondos secundarios y pastillas
    surfaceVariant = Color(0xFFEDE7D8),
    // el casi negro verdoso pa' textos sobre fondo claro
    onSurface = Color(0xFF1E2A23),
    // el gris verdoso apagado, pa' textos secundarios
    muted = Color(0xFF5F6F64),
    // el beige grisáceo pa' bordes y líneas divisorias
    outline = Color(0xFFB9B19F),
    // el rojo error, pa' cuando algo sale mal
    error = Color(0xFFB3261E),
    // verde de éxito, el mismo verde medio de siempre
    success = Color(0xFF2D6A4F),
    // mostaza oscuro pa' avisos
    warning = Color(0xFFB7791F),
    // azulito pa' info, ahí nomás
    info = Color(0xFF2F6690)
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: PALETA OSCURA (modo noche)
//  Los verdes se aclaran un cachito pa' que se lean bien sobre fondos
//  oscuros, y los fondos se vuelven verdes casi negros, bien pro.
//  Los textos blancos sobre verde SIGUEN funcionando, así ninguna
//  pantalla hubo que tocarse pa' el modo oscuro. Chulada.
// ═══════════════════════════════════════════════════════════════════
val PalmDarkPalette = PalmPalette(
    // verde medio-claro: se lee perfecto como texto sobre tarjetas oscuras
    primary = Color(0xFF57A87E),
    // verde profundo pa' los arranques de degradado, casi negro verdoso
    primaryDark = Color(0xFF0E2418),
    // verde medio pa' lo secundario del degradado
    secondary = Color(0xFF3E8E63),
    // el mostaza se queda, sobre oscuro luce igual de chido
    tertiary = Color(0xFFE09F3E),
    // fondo nocturno: negro con un toque de verde, nada de gris flatiao
    cream = Color(0xFF0F1512),
    // tarjetas de noche: verde oscuro, NO negro puro (cansa la vista)
    surface = Color(0xFF19221B),
    // pastillas y fondos secundarios de noche, un pelín más claros
    surfaceVariant = Color(0xFF233026),
    // texto principal de noche, blanquito verdoso suave
    onSurface = Color(0xFFE6EDE5),
    // texto secundario de noche, gris verdoso claro
    muted = Color(0xFF9BAFA1),
    // bordes de noche, verde grisáceo discreto
    outline = Color(0xFF3C4A3F),
    // rojo más clarito, el oscuro no se lee sobre fondo negro
    error = Color(0xFFE57368),
    // éxito de noche: el mismo verde claro de primary
    success = Color(0xFF57A87E),
    // aviso de noche, mostaza suavizado
    warning = Color(0xFFD9A441),
    // info de noche, azul despeinado
    info = Color(0xFF7FA8C9)
)

// el "tubo" por donde viaja la paleta activa: el tema le mete la clara o
// la oscura y TODA la app la lee desde aquí sin enterarse del cambio
val LocalPalmPalette = staticCompositionLocalOf { PalmLightPalette }

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: LOS NOMBRES DE SIEMPRE (AHORA ADAPTATIVOS)
//  Son los mismos nombres que usaban todas las pantallas, pero ahora
//  valen lo que diga la paleta ACTIVA. En modo claro es como si nada
//  hubiera cambiado; en oscuro, toda la app se viste de noche solita.
// ═══════════════════════════════════════════════════════════════════

// el verde palma insignia: barras, botones, acentos y textos verdes
val PalmPrimary: Color
    // getter composable: lee la paleta activa en el momento de pintar
    @Composable get() = LocalPalmPalette.current.primary

// el verde más oscuro, pa' degradados y texto fuerte
val PalmPrimaryDark: Color
    @Composable get() = LocalPalmPalette.current.primaryDark

// el verde medio, pa' acciones secundarias y lo que ya salió chido
val PalmSecondary: Color
    @Composable get() = LocalPalmPalette.current.secondary

// el mostaza, pa' acentos, pendientes y el escáner
val PalmTertiary: Color
    @Composable get() = LocalPalmPalette.current.tertiary

// el fondo general de las pantallas (crema de día, noche de... noche)
val PalmCream: Color
    @Composable get() = LocalPalmPalette.current.cream

// las tarjetas y campos (blanco cálido de día, verde oscuro de noche)
val PalmSurface: Color
    @Composable get() = LocalPalmPalette.current.surface

// fondos secundarios y pastillas
val PalmSurfaceVariant: Color
    @Composable get() = LocalPalmPalette.current.surfaceVariant

// el texto principal sobre fondo/superficie
val PalmOnSurface: Color
    @Composable get() = LocalPalmPalette.current.onSurface

// el texto secundario, apagadito pero legible en ambos modos
val PalmMuted: Color
    @Composable get() = LocalPalmPalette.current.muted

// bordes y líneas divisorias
val PalmOutline: Color
    @Composable get() = LocalPalmPalette.current.outline

// el rojo error, pa' cuando el user la caga en algo
val PalmError: Color
    @Composable get() = LocalPalmPalette.current.error

// verde de éxito, o sea "ya quedó"
val PalmSuccess: Color
    @Composable get() = LocalPalmPalette.current.success

// mostaza oscuro pa' avisos tipo "espérate tantito"
val PalmWarning: Color
    @Composable get() = LocalPalmPalette.current.warning

// azulito pa' info, el único que no es verde, ahí nomás
val PalmInfo: Color
    @Composable get() = LocalPalmPalette.current.info
