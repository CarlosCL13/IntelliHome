package com.intelliworks.intellihome.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.util.Base64
import java.util.UUID


/**
 * Gestiona tokens y claves protegidas por biometría
 */
object BiometricCryptoHelper {
    private const val KEY_ALIAS = "biometric_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"

    // Genera un token público aleatorio (UUID)
    fun generarTokenPublico(): String = UUID.randomUUID().toString()

    // Genera y almacena una clave protegida por biometría en el Keystore si no existe previamente
    fun generarClaveBiometrica() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(false) // Cambiar a false para no requerir autenticación en cada uso
                .setInvalidatedByBiometricEnrollment(true)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    // Recupera la clave almacenada en el Keystore bajo el alias biométrico
    private fun obtenerClaveBiometrica(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    // Inicializa y retorna un Cifrado configurado para cifrado usando la clave biométrica
    fun encriptarClave(): Cipher {
        val cifrado = Cipher.getInstance(TRANSFORMATION)
        cifrado.init(Cipher.ENCRYPT_MODE, obtenerClaveBiometrica())
        return cifrado
    }

    // Inicializa y retorna un Cifrado configurado para descifrado usando la clave biométrica y el IV proporcionado
    fun desencriptarClave(iv: ByteArray): Cipher {
        val cifrado = Cipher.getInstance(TRANSFORMATION)
        cifrado.init(Cipher.DECRYPT_MODE, obtenerClaveBiometrica(), IvParameterSpec(iv))
        return cifrado
    }

    // Encripta el token recibido usando el Cipher proporcionado
    // Retorna un par: el token encriptado en Base64 y el IV en Base64
    fun encriptarToken(token: String, cifrado: Cipher): Pair<String, String> {
        val encriptado = cifrado.doFinal(token.toByteArray(Charsets.UTF_8))
        val iv = cifrado.iv
        return Base64.encodeToString(encriptado, Base64.DEFAULT) to Base64.encodeToString(iv, Base64.DEFAULT)
    }

    // Descifra el token encriptado usando el IV y el Cipher proporcionado
    // Retorna el token original como String
    fun desencriptarToken(encryptedToken: String, iv: String, cifrado: Cipher): String {
        val bytesEncriptados = Base64.decode(encryptedToken, Base64.DEFAULT)
        val ivBytes = Base64.decode(iv, Base64.DEFAULT)
        val descifrarCifrado = desencriptarClave(ivBytes)
        val descifrado = descifrarCifrado.doFinal(bytesEncriptados)
        return String(descifrado, Charsets.UTF_8)
    }

}
