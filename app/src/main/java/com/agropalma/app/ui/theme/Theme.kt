/*
 * Theme.kt - TEMA DE LA APLICACION (CLARO Y OSCURO)
 * --------------------------------------------------
 * Combina colores, tipografia y formas redondeadas en un solo tema de
 * Material 3. Todas las pantallas se envuelven con AgroPalmaTheme.
 *
 * Ahora soporta MODO OSCURO: le pasas darkTheme=true y aqui pasan dos
 * cosas al mismo tiempo:
 *  1. El colorScheme de Material 3 se cambia por el esquema nocturno.
 *  2. Se le inyecta la PalmDarkPalette al LocalPalmPalette, así TODOS
 *     los colores PalmX (PalmCream, PalmPrimary...) que usan las
 *     pantallas cambian solos a sus versiones de noche.
 * El modo lo decide el usuario en Ajustes y se guarda en SharedPreferences.
 */

// el package del tema, donde se arma el look completo de la app
package com.agropalma.app.ui.theme

// imports pa' las formas redondeadas y el MaterialTheme de Material 3
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
// CompositionLocalProvider: el que "inyecta" la paleta activa a toda la app
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// la paleta completa de colores del tema CLARO, todo el chiste del look
private val AgroColors = lightColorScheme(
    // Verde palma: color principal de barras y botones.
    primary = PalmLightPalette.primary,
    // el texto e iconos que van ENCIMA del verde van en blanco cálido
    onPrimary = PalmLightPalette.surface,
    // contenedor verde clarito pa' pastillas y fondos suaves
    primaryContainer = Color(0xFFD8EADF),
    // y el texto sobre ese contenedor, en verde bien oscuro
    onPrimaryContainer = PalmLightPalette.primaryDark,
    // Verde medio para acciones secundarias.
    secondary = PalmLightPalette.secondary,
    // texto blanco cálido sobre el verde medio
    onSecondary = PalmLightPalette.surface,
    // contenedor beige pa' las superficies secundarias
    secondaryContainer = PalmLightPalette.surfaceVariant,
    // y su texto en el casi negro verdoso
    onSecondaryContainer = PalmLightPalette.onSurface,
    // Mostaza: acentos, codigo de invitacion y escaner.
    tertiary = PalmLightPalette.tertiary,
    // texto oscuro sobre el mostaza pa' que se lea bien
    onTertiary = PalmLightPalette.primaryDark,
    // contenedor mostaza clarito
    tertiaryContainer = Color(0xFFF7E7C8),
    // y su texto en café bien oscuro
    onTertiaryContainer = Color(0xFF5A3B00),
    // Fondo crema y superficies blancas cálidas.
    background = PalmLightPalette.cream,
    // texto sobre el fondo, el casi negro verdoso
    onBackground = PalmLightPalette.onSurface,
    // superficie de tarjetas en blanco cálido
    surface = PalmLightPalette.surface,
    onSurface = PalmLightPalette.onSurface,
    // variante de superficie beige pa' zonas secundarias
    surfaceVariant = PalmLightPalette.surfaceVariant,
    // texto apagado sobre esa variante
    onSurfaceVariant = PalmLightPalette.muted,
    // bordes con el beige grisáceo de siempre
    outline = PalmLightPalette.outline,
    // el rojo de error y su texto en blanco cálido
    error = PalmLightPalette.error,
    onError = PalmLightPalette.surface
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: ESQUEMA NOCTURNO DE MATERIAL 3
//  Propósito: los componentes M3 (botones, diálogos, chips...) también
//  se pintan de noche. Va agarrado de la mano de PalmDarkPalette pa' que
//  todo quede en el mismo tono, nada de grises genéricos.
// ═══════════════════════════════════════════════════════════════════
private val AgroNightColors = darkColorScheme(
    // el verde nocturno: más clarito pa' que se lea sobre fondos oscuros
    primary = PalmDarkPalette.primary,
    // y el texto sobre ese verde va oscuro, pa' contraste al revés
    onPrimary = Color(0xFF0E2418),
    // contenedor verde profundo pa' pastillas activas de noche
    primaryContainer = Color(0xFF2E5A42),
    // y su texto en verde clarito
    onPrimaryContainer = Color(0xFFBFE3CD),
    // el verde medio nocturno
    secondary = PalmDarkPalette.secondary,
    // texto oscuro sobre el verde medio
    onSecondary = Color(0xFF0E2418),
    // contenedor de superficies secundarias de noche
    secondaryContainer = PalmDarkPalette.surfaceVariant,
    // y su texto clarito
    onSecondaryContainer = PalmDarkPalette.onSurface,
    // el mostaza de noche (funciona igual de bien sobre oscuro)
    tertiary = PalmDarkPalette.tertiary,
    // texto oscuro sobre el mostaza, como de día
    onTertiary = Color(0xFF3A2B0D),
    // contenedor mostaza oscuro pa' las pastillas pendientes
    tertiaryContainer = Color(0xFF554422),
    // y su texto en mostaza clarito
    onTertiaryContainer = Color(0xFFF2DCB4),
    // fondo nocturno: negro con alma de verde
    background = PalmDarkPalette.cream,
    // texto claro sobre ese fondo
    onBackground = PalmDarkPalette.onSurface,
    // tarjetas de noche
    surface = PalmDarkPalette.surface,
    onSurface = PalmDarkPalette.onSurface,
    // variante de superficie nocturna
    surfaceVariant = PalmDarkPalette.surfaceVariant,
    // texto apagado nocturno
    onSurfaceVariant = PalmDarkPalette.muted,
    // bordes de noche
    outline = PalmDarkPalette.outline,
    // el rojo de error clarito de noche
    error = PalmDarkPalette.error,
    onError = Color(0xFF2A0E0B)
)

// las formas de la app, TODO lleva esquinas suaves aquí, nada de picudos
private val AgroShapes = Shapes(
    // las más chiquitas pa' chips y botoncitos pequeños
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    // medianas pa' campos y tarjetas
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    // las más grandes pa' modales y banners
    extraLarge = RoundedCornerShape(32.dp)
)

// composable de la UI wey
@Composable
// el tema maestro: toda pantalla se envuelve aquí pa' heredar el look.
// darkTheme: false = modo claro de siempre, true = modo noche
fun AgroPalmaTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    // el MaterialTheme con los colores, letras y formas de AgroPalma
    MaterialTheme(
        // según el modo: paleta clara o la nocturna, así de fácil
        colorScheme = if (darkTheme) AgroNightColors else AgroColors,
        typography = AgroTypography,
        // las esquinas suaves
        shapes = AgroShapes
    ) {
        // y aquí la magia: le inyectamos a TODA la app la paleta Palm
        // activa (clara u oscura). Todos los PalmX que leen las pantallas
        // cambian solos, sin tocar una línea de ellas
        CompositionLocalProvider(
            LocalPalmPalette provides if (darkTheme) PalmDarkPalette else PalmLightPalette
        ) {
            // el contenido de la pantalla envuelto en la paleta nueva
            content()
        }
    }
}
