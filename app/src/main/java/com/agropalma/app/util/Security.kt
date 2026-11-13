/*
 * Security.kt - CIFRADO DE CONTRASENAS
 * ------------------------------------
 * Nunca se guarda la contrasena en texto plano. Antes de guardar o
 * comparar se aplica SHA-256 con una sal fija. Es una proteccion basica
 * para un prototipo local; en produccion se debe usar un servidor.
 */

// paquete donde vive esta utilidad de seguridad, ahí nomás
package com.agropalma.app.util

// import pa' crear el hash SHA-256, el cifrado ese que no se puede revertir
import java.security.MessageDigest

// objeto único que cifra las contraseñas pa' no guardarlas peladas como pendejo
object Security {
    // la sal fija que se le revuelve a la contraseña pa' que el hash no sea tan predecible
    private const val SALT = "agropalma-local-v1"

    // función que convierte la contraseña en hash seguro, pa' guardar o comparar
    fun hash(password: String): String {
        // consigue el motor de hash SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        // le pega la sal a la contraseña y la muele toda junta en bytes
        val bytes = digest.digest((SALT + password).toByteArray(Charsets.UTF_8))
        // pasa los bytes a texto hexadecimal, dos numeritos por byte pa' que se vea bonito
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
