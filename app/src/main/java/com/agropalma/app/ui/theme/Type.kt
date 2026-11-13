/*
 * Type.kt - TIPOGRAFIA
 * --------------------
 * Define los tamanos y pesos de letra (titulos grandes, texto normal,
 * etiquetas) para que toda la app tenga el mismo estilo legible y con
 * botones grandes pensados para uso en campo.
 */

// el package del tema, aquí se definen las letras de toda la app
package com.agropalma.app.ui.theme

// imports pa' la tipografía de Material 3 y los estilos de texto
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// la tipografía completa de la app, letras legibles pa' uso en campo
val AgroTypography = Typography(
    // el más grandote: pa' números gigantes de las estadísticas
    displaySmall = TextStyle(
        // letra normalita del sistema, en negritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        // tamaño y alto de línea pa' que no se amontone el texto
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    // títulos bien grandes, casi pa' banner
    headlineLarge = TextStyle(
        // misma letra del sistema, en negritas también
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    // headline mediano, pa' títulos de sección
    headlineMedium = TextStyle(
        // letra del sistema en negritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    // headline chico, el de los encabezados menores
    headlineSmall = TextStyle(
        // letra del sistema en seminegritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    // título grande, el de la barra de arriba
    titleLarge = TextStyle(
        // letra del sistema en seminegritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // título mediano, pa' tarjetas y encabezados de bloque
    titleMedium = TextStyle(
        // letra del sistema en seminegritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // texto grande, pa' párrafos que se leen bonito
    bodyLarge = TextStyle(
        // letra normalita del sistema, sin negritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    // texto mediano, el del cuerpo normal de las pantallas
    bodyMedium = TextStyle(
        // letra normalita del sistema
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // texto chiquito, pa' notas y detalles que nomás complementan
    bodySmall = TextStyle(
        // letra normalita del sistema
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    // etiqueta grande, pa' botones y campos bien legibles
    labelLarge = TextStyle(
        // letra del sistema en seminegritas
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // etiqueta mediana, pa' pastillas y los iconitos del menú de abajo
    labelMedium = TextStyle(
        // letra del sistema en medium, ni flaca ni gorda
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
