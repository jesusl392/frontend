package com.unicundi.unimarket.data.model

import com.google.gson.annotations.SerializedName

// ─── AUTH ────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String
)

// El backend devuelve id, nombre, correo, rol, estado — sin JWT
data class LoginResponse(
    @SerializedName("id")     val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("rol")    val rol: String,
    @SerializedName("estado") val estado: Boolean
)

data class RegisterRequest(
    @SerializedName("nombre")  val nombre: String,
    @SerializedName("correo")  val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("codigo")  val codigo: String
)

// Enviar código de verificación al correo
data class SendCodeRequest(
    @SerializedName("correo") val correo: String
)

// Verificar código (también usado para confirmar registro)
data class VerifyCodeRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("codigo") val codigo: String
)

// ─── USUARIO ─────────────────────────────────────────────────

data class Usuario(
    @SerializedName("id")          val id: Long = 0,
    @SerializedName("nombre")      var nombre: String = "",
    @SerializedName("correo")      val correo: String = "",
    @SerializedName("rol")         val rol: String = "",
    @SerializedName("fotoPerfil")  val fotoPerfil: String? = null,
    @SerializedName("estado")      val estado: Boolean = true
)

data class UsuarioRequest(
    @SerializedName("nombre")   val nombre: String,
    @SerializedName("correo")   val correo: String,
    @SerializedName("password") val password: String
)

// ─── PRODUCTO (Posts en el backend) ──────────────────────────

data class Producto(
    @SerializedName("id")               val id: Long = 0,
    @SerializedName("titulo")           val titulo: String = "",
    @SerializedName("descripcion")      val descripcion: String = "",  // backend DTO usa descripcion correcto
    @SerializedName("precio")           val precio: Double = 0.0,
    @SerializedName("categoria")        val categoria: String = "",
    @SerializedName("estado")           val estado: String = "",       // activo, vendido, eliminado
    @SerializedName("fechaPublicacion") val fechaPublicacion: String? = null,
    @SerializedName("usuarioId")        val usuarioId: Long = 0,
    @SerializedName("usuarioNombre")    val usuarioNombre: String = "",
    @SerializedName("usuarioFoto")      val usuarioFoto: String? = null,
    @SerializedName("ubicacion")        val ubicacion: String? = null,
    @SerializedName("imagenes")         val imagenes: List<PostImagen>? = null
)

data class ProductoRequest(
    @SerializedName("titulo")      val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("precio")      val precio: Double,
    @SerializedName("categoria")   val categoria: String,
    @SerializedName("estado")      val estado: String,
    @SerializedName("ubicacion")   val ubicacion: String?,
    @SerializedName("usuarioId")   val usuarioId: Long
)

data class PostImagen(
    @SerializedName("id")    val id: Long = 0,
    @SerializedName("url")   val url: String = "",
    @SerializedName("orden") val orden: Int = 0
)

// ─── MENSAJE ─────────────────────────────────────────────────

data class Mensaje(
    @SerializedName("id")            val id: Long = 0,
    @SerializedName("message")       val message: String = "",
    @SerializedName("leido")         val leido: Boolean = false,
    @SerializedName("dateTime")      val dateTime: String? = null,
    @SerializedName("senderId")      val senderId: Long = 0,
    @SerializedName("senderNombre")  val senderNombre: String = "",
    @SerializedName("receiverId")    val receiverId: Long = 0,
    @SerializedName("receiverNombre") val receiverNombre: String = ""
)

data class MensajeRequest(
    @SerializedName("message")    val message: String,
    @SerializedName("senderId")   val senderId: Long,
    @SerializedName("receiverId") val receiverId: Long
)

data class ConversacionItem(
    val mensaje: Mensaje,
    val interlocutorFoto: String?
)

// ─── PURCHASE REQUEST ────────────────────────────────────────

data class SolicitudCompra(
    @SerializedName("id")           val id: Long = 0,
    @SerializedName("estado")       val estado: String = "",
    @SerializedName("fecha")        val fecha: String? = null,
    @SerializedName("postId")       val postId: Long = 0,
    @SerializedName("postTitulo")   val postTitulo: String = "",
    @SerializedName("buyerId")      val buyerId: Long = 0,
    @SerializedName("buyerNombre")  val buyerNombre: String = "",
    @SerializedName("sellerId")     val sellerId: Long = 0,
    @SerializedName("sellerNombre") val sellerNombre: String = ""
)

data class SolicitudCompraRequest(
    @SerializedName("postId")   val postId: Long,
    @SerializedName("buyerId")  val buyerId: Long,
    @SerializedName("sellerId") val sellerId: Long,
    @SerializedName("estado")   val estado: String = "pendiente"
)

// ─── RATING ──────────────────────────────────────────────────

data class Rating(
    @SerializedName("id")          val id: Long = 0,
    @SerializedName("puntuacion")  val puntuacion: Int = 0,
    @SerializedName("comentario")  val comentario: String = "",
    @SerializedName("tipo")        val tipo: String = "",
    @SerializedName("date")        val date: String? = null,
    @SerializedName("fromId")      val fromId: Long = 0,
    @SerializedName("fromNombre")  val fromNombre: String = "",
    @SerializedName("toId")        val toId: Long = 0,
    @SerializedName("toNombre")    val toNombre: String = ""
)

data class RatingRequest(
    @SerializedName("puntuacion")  val puntuacion: Int,
    @SerializedName("comentario")  val comentario: String,
    @SerializedName("tipo")        val tipo: String,
    @SerializedName("fromId")      val fromId: Long,
    @SerializedName("toId")        val toId: Long
)

data class FavoritoRequest(
    @SerializedName("usuarioId") val usuarioId: Long,
    @SerializedName("postId")    val postId: Long
)

// ─── FAVORITOS ────────────────────────────────────────────────
object FavoritosManager {
    private const val PREFS_NAME = "favoritos"
    private const val KEY_LISTA  = "lista"

    fun obtener(context: android.content.Context): List<Producto> {
        val json = prefs(context).getString(KEY_LISTA, "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<Producto>>() {}.type
        return com.google.gson.Gson().fromJson(json, type)
    }

    fun agregar(context: android.content.Context, producto: Producto) {
        val lista = obtener(context).toMutableList()
        if (lista.none { it.id == producto.id }) {
            lista.add(producto)
            guardar(context, lista)
        }
    }

    fun quitar(context: android.content.Context, productoId: Long) {
        val lista = obtener(context).toMutableList()
        lista.removeAll { it.id == productoId }
        guardar(context, lista)
    }

    fun esFavorito(context: android.content.Context, productoId: Long): Boolean =
        obtener(context).any { it.id == productoId }

    private fun guardar(context: android.content.Context, lista: List<Producto>) {
        val json = com.google.gson.Gson().toJson(lista)
        prefs(context).edit().putString(KEY_LISTA, json).apply()
    }

    private fun prefs(context: android.content.Context) =
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
}

// ─── SESIÓN LOCAL ─────────────────────────────────────────────
object Sesion {
    var usuarioId: Long = 0
    var nombre: String = ""
    var correo: String = ""
    var rol: String = ""

    fun guardar(response: LoginResponse) {
        usuarioId = response.id
        nombre    = response.nombre
        correo    = response.correo
        rol       = response.rol
    }

    fun guardarEnPrefs(prefs: android.content.SharedPreferences) {
        prefs.edit()
            .putLong("usuarioId", usuarioId)
            .putString("nombre", nombre)
            .putString("correo", correo)
            .putString("rol", rol)
            .apply()
    }

    fun cargarDesdePrefs(prefs: android.content.SharedPreferences) {
        usuarioId = prefs.getLong("usuarioId", 0)
        nombre    = prefs.getString("nombre", "") ?: ""
        correo    = prefs.getString("correo", "") ?: ""
        rol       = prefs.getString("rol", "") ?: ""
    }

    fun limpiar() {
        usuarioId = 0
        nombre    = ""
        correo    = ""
        rol       = ""
    }

    fun limpiarPrefs(prefs: android.content.SharedPreferences) {
        prefs.edit().clear().apply()
    }

    val estaLogueado get() = usuarioId > 0
}
