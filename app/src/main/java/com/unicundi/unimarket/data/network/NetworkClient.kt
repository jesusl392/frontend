package com.unicundi.unimarket.data.network

import com.unicundi.unimarket.data.model.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    // ─── AUTH ─────────────────────────────────────────────────
    // Paso 1 del registro: enviar código al correo
    @POST("verification/send-code")
    suspend fun enviarCodigo(@Body request: SendCodeRequest): Response<Void>

    // Paso 2 del registro: verificar código
    @POST("verification/verify-code")
    suspend fun verificarCodigo(@Body request: VerifyCodeRequest): Response<Void>

    // Paso 3 del registro: crear cuenta
    @POST("api/auth/register")
    suspend fun registrar(@Body request: RegisterRequest): Response<Usuario>

    // Login con correo + password
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ─── USUARIOS ─────────────────────────────────────────────
    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<Usuario>>

    @GET("api/usuarios/{id}")
    suspend fun getUsuarioById(@Path("id") id: Long): Response<Usuario>

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: Long, @Body usuario: UsuarioRequest): Response<Usuario>

    // ─── POSTS / PRODUCTOS ────────────────────────────────────
    // OJO: el backend usa /api/Post (con P mayúscula)
    @GET("api/Post")
    suspend fun getProductos(): Response<List<Producto>>

    @GET("api/Post/{id}")
    suspend fun getProductoById(@Path("id") id: Long): Response<Producto>

    @POST("api/Post")
    suspend fun publicarProducto(@Body request: ProductoRequest): Response<Producto>

    @PUT("api/Post/{id}")
    suspend fun actualizarProducto(@Path("id") id: Long, @Body request: ProductoRequest): Response<Producto>

    @DELETE("api/Post/{id}")
    suspend fun eliminarProducto(@Path("id") id: Long): Response<Void>

    // ─── MENSAJES ─────────────────────────────────────────────
    // OJO: el backend usa /api/Mensaje (con M mayúscula)
    @GET("api/Mensaje")
    suspend fun getMensajes(): Response<List<Mensaje>>

    @GET("api/Mensaje/{id}")
    suspend fun getMensajeById(@Path("id") id: Long): Response<Mensaje>

    @POST("api/Mensaje")
    suspend fun enviarMensaje(@Body request: MensajeRequest): Response<Mensaje>

    // ─── PERFIL: actualizar nombre y foto ────────────────────
    @PATCH("api/usuarios/{id}/nombre")
    suspend fun actualizarNombre(
        @Path("id") id: Long,
        @Query("nombre") nombre: String
    ): Response<Void>

    @Multipart
    @PATCH("api/usuarios/{id}/foto")
    suspend fun subirFotoPerfil(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part
    ): Response<okhttp3.ResponseBody>

    // ─── IMAGENES DE POSTS ────────────────────────────────────
    @Multipart
    @POST("api/postImagenes/upload")
    suspend fun subirImagenProducto(
        @Part file: MultipartBody.Part,
        @Part("postId") postId: RequestBody
    ): Response<PostImagen>

    // ─── SOLICITUDES DE COMPRA ────────────────────────────────
    @GET("api/Request")
    suspend fun getSolicitudes(): Response<List<SolicitudCompra>>

    @POST("api/Request")
    suspend fun crearSolicitud(@Body request: SolicitudCompraRequest): Response<SolicitudCompra>

    // ─── RATINGS ──────────────────────────────────────────────
    @GET("api/Ratings")
    suspend fun getRatings(): Response<List<Rating>>

    @POST("api/Ratings")
    suspend fun crearRating(@Body request: RatingRequest): Response<Rating>

    // ─── FAVORITOS ────────────────────────────────────────────
    @GET("api/favoritos/{userId}")
    suspend fun getFavoritos(@Path("userId") userId: Long): Response<List<Producto>>

    @POST("api/favoritos")
    suspend fun addFavorito(@Body request: FavoritoRequest): Response<Void>

    @DELETE("api/favoritos/{userId}/{postId}")
    suspend fun removeFavorito(@Path("userId") userId: Long, @Path("postId") postId: Long): Response<Void>
}

object RetrofitClient {
    // Emulador Android: 10.0.2.2 apunta al localhost de tu PC
    // Dispositivo físico: cambia por la IP local de tu PC (ipconfig)
    private const val BASE_URL = "https://marketplace-final-ncu7.onrender.com"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}