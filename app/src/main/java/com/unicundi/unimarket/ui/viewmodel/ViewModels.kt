package com.unicundi.unimarket.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.unicundi.unimarket.data.model.*
import com.unicundi.unimarket.data.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    object Idle    : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ─── AUTH VIEWMODEL ───────────────────────────────────────────
class AuthViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _envioCodigoState = MutableLiveData<UiState<String>>(UiState.Idle)
    val envioCodigoState: LiveData<UiState<String>> = _envioCodigoState

    private val _loginState = MutableLiveData<UiState<LoginResponse>>(UiState.Idle)
    val loginState: LiveData<UiState<LoginResponse>> = _loginState

    private val _registroState = MutableLiveData<UiState<Usuario>>(UiState.Idle)
    val registroState: LiveData<UiState<Usuario>> = _registroState

    // Paso 1: Enviar código al correo
    fun enviarCodigo(correo: String) {
        _envioCodigoState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.enviarCodigo(SendCodeRequest(correo))
                android.util.Log.d("UniMarket", "enviarCodigo response: ${r.code()} ${r.message()} body: ${r.body()} error: ${r.errorBody()?.string()}")
                if (r.isSuccessful) {
                    _envioCodigoState.value = UiState.Success("Codigo enviado")
                } else {
                    // 4xx puede significar que el correo ya existe o formato incorrecto
                    val errorBody = r.errorBody()?.string() ?: ""
                    android.util.Log.e("UniMarket", "Error body: $errorBody")
                    _envioCodigoState.value = UiState.Error("Error ${r.code()}: $errorBody")
                }
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("UniMarket", "Timeout: ${e.message}")
                _envioCodigoState.value = UiState.Error("El servidor tarda en responder. Intenta de nuevo en 30 segundos.")
            } catch (e: Exception) {
                android.util.Log.e("UniMarket", "Exception: ${e.javaClass.simpleName}: ${e.message}")
                _envioCodigoState.value = UiState.Error("Error de conexion: ${e.javaClass.simpleName}")
            }
        }
    }

    // Paso 2: Verificar código
    fun verificarCodigo(correo: String, codigo: String) {
        _envioCodigoState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.verificarCodigo(VerifyCodeRequest(correo, codigo))
                if (r.isSuccessful) {
                    _envioCodigoState.value = UiState.Success("Codigo valido")
                } else {
                    _envioCodigoState.value = UiState.Error("Codigo incorrecto o expirado")
                }

            } catch (e: Exception) {
                _envioCodigoState.value = UiState.Error("Sin conexion: ${e.message}")
            }
        }
    }

    // Login con correo + password
    fun login(correo: String, password: String) {
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.login(LoginRequest(correo, password))
                if (r.isSuccessful && r.body() != null) {
                    Sesion.guardar(r.body()!!)
                    _loginState.value = UiState.Success(r.body()!!)
                } else {
                    _loginState.value = UiState.Error("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                _loginState.value = UiState.Error("Sin conexion: ${e.message}")
            }
        }
    }

    // Registro completo con código ya verificado
    fun registrar(nombre: String, correo: String, password: String, codigo: String) {
        _registroState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.registrar(RegisterRequest(nombre, correo, password, codigo))
                if (r.isSuccessful) {
                    _registroState.value = UiState.Success(r.body()!!)
                } else {
                    _registroState.value = UiState.Error("Error en registro: ${r.code()}")
                }
            } catch (e: Exception) {
                _registroState.value = UiState.Error("Sin conexion: ${e.message}")
            }
        }
    }
}

// ─── PRODUCTO VIEWMODEL ───────────────────────────────────────
class ProductoViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private var listaCompleta: List<Producto> = emptyList()

    private val _productos = MutableLiveData<UiState<List<Producto>>>()
    val productos: LiveData<UiState<List<Producto>>> = _productos

    // Para el home: destacados simulados localmente (los 2 primeros de la lista)
    private val _destacados = MutableLiveData<UiState<List<Producto>>>()
    val destacados: LiveData<UiState<List<Producto>>> = _destacados

    private val _productoDetalle = MutableLiveData<UiState<Producto>>()
    val productoDetalle: LiveData<UiState<Producto>> = _productoDetalle

    private val _publicarState = MutableLiveData<UiState<Producto>>(UiState.Idle)
    val publicarState: LiveData<UiState<Producto>> = _publicarState

    fun cargarProductos() {
        _productos.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getProductos()
                if (r.isSuccessful) {
                    val lista = r.body() ?: emptyList()
                    val visibles = lista.filter { !it.estado.equals("vendido", ignoreCase = true) }
                    listaCompleta = visibles
                    _productos.value = UiState.Success(visibles)
                    // Los primeros 2 activos como "destacados"
                    _destacados.value = UiState.Success(
                        visibles.filter { it.estado.equals("activo", ignoreCase = true) }.take(2)
                    )
                } else {
                    _productos.value = UiState.Error("Error ${r.code()}")
                }
            } catch (e: Exception) {
                _productos.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    fun buscarProductos(query: String) {
        if (listaCompleta.isEmpty()) { cargarProductos(); return }
        val resultado = listaCompleta.filter {
            it.titulo.contains(query, ignoreCase = true) ||
                    it.descripcion.contains(query, ignoreCase = true) ||
                    it.categoria.contains(query, ignoreCase = true)
        }
        _productos.value = UiState.Success(resultado)
    }

    fun filtrarPorCategoria(categoria: String) {
        if (listaCompleta.isEmpty()) { cargarProductos(); return }
        if (categoria == "Todos") {
            _productos.value = UiState.Success(listaCompleta)
            return
        }
        val resultado = listaCompleta.filter {
            it.categoria.equals(categoria, ignoreCase = true)
        }
        _productos.value = UiState.Success(resultado)
    }

    fun filtrar(categoria: String, precioMin: Double?, precioMax: Double?) {
        if (listaCompleta.isEmpty()) { cargarProductos(); return }
        var resultado = listaCompleta
        if (categoria != "Todos") {
            resultado = resultado.filter { it.categoria.equals(categoria, ignoreCase = true) }
        }
        precioMin?.let { min -> resultado = resultado.filter { it.precio >= min } }
        precioMax?.let { max -> resultado = resultado.filter { it.precio <= max } }
        _productos.value = UiState.Success(resultado)
    }

    fun cargarDetalle(id: Long) {
        _productoDetalle.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getProductoById(id)
                _productoDetalle.value = if (r.isSuccessful) UiState.Success(r.body()!!)
                else UiState.Error("Error ${r.code()}")
            } catch (e: Exception) {
                _productoDetalle.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    fun publicarProducto(titulo: String, descripcion: String, precio: Double,
                         categoria: String, estado: String, ubicacion: String? = null) {
        if (Sesion.usuarioId == 0L) {
            _publicarState.value = UiState.Error("Debes iniciar sesion")
            return
        }
        _publicarState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.publicarProducto(
                    ProductoRequest(titulo, descripcion, precio, categoria, estado, ubicacion, Sesion.usuarioId)
                )
                _publicarState.value = if (r.isSuccessful) UiState.Success(r.body()!!)
                else UiState.Error("Error al publicar: ${r.code()}")
            } catch (e: Exception) {
                _publicarState.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    fun eliminarProducto(id: Long) {
        viewModelScope.launch {
            try { api.eliminarProducto(id); cargarProductos() }
            catch (e: Exception) { /* silencioso */ }
        }
    }

    fun agregarFavoritoServidor(postId: Long) {
        if (Sesion.usuarioId == 0L) return
        viewModelScope.launch {
            try { api.addFavorito(FavoritoRequest(Sesion.usuarioId, postId)) }
            catch (e: Exception) { /* silencioso */ }
        }
    }

    fun quitarFavoritoServidor(postId: Long) {
        if (Sesion.usuarioId == 0L) return
        viewModelScope.launch {
            try { api.removeFavorito(Sesion.usuarioId, postId) }
            catch (e: Exception) { /* silencioso */ }
        }
    }

    fun subirImagenProducto(context: Context, imageUri: Uri, postId: Long) {
        // Se usa un scope propio (no viewModelScope) para que el upload NO se cancele
        // cuando el Fragment navega de regreso y el ViewModel se destruye.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "imagen.jpg", requestBody)
                val postIdBody = postId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val response = api.subirImagenProducto(part, postIdBody)
                android.util.Log.d("UniMarket", "Upload imagen: ${response.code()} url=${response.body()?.url}")
            } catch (e: Exception) {
                android.util.Log.e("UniMarket", "Error subiendo imagen: ${e.message}")
            }
        }
    }
}

// ─── PERFIL VIEWMODEL ─────────────────────────────────────────
class PerfilViewModel(application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.apiService

    private val _perfil = MutableLiveData<UiState<Usuario>>()
    val perfil: LiveData<UiState<Usuario>> = _perfil

    // Mis publicaciones: filtra la lista completa por usuarioId
    private val _misPublicaciones = MutableLiveData<UiState<List<Producto>>>()
    val misPublicaciones: LiveData<UiState<List<Producto>>> = _misPublicaciones

    fun cargarPerfil() {
        if (Sesion.usuarioId == 0L) {
            // Usar datos de sesión directamente si no hay ID
            _perfil.value = UiState.Success(
                Usuario(nombre = Sesion.nombre, correo = Sesion.correo, rol = Sesion.rol)
            )
            return
        }
        _perfil.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getUsuarioById(Sesion.usuarioId)
                _perfil.value = if (r.isSuccessful) UiState.Success(r.body()!!)
                else UiState.Error("Error ${r.code()}")
            } catch (e: Exception) {
                _perfil.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    fun cargarMisPublicaciones() {
        _misPublicaciones.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getProductos()
                if (r.isSuccessful) {
                    val misPosts = (r.body() ?: emptyList())
                        .filter { it.usuarioId == Sesion.usuarioId }
                    _misPublicaciones.value = UiState.Success(misPosts)
                } else {
                    _misPublicaciones.value = UiState.Error("Error ${r.code()}")
                }
            } catch (e: Exception) {
                _misPublicaciones.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    private val _eliminarState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val eliminarState: LiveData<UiState<Unit>> = _eliminarState

    fun eliminarProducto(id: Long) {
        _eliminarState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.eliminarProducto(id)
                if (r.isSuccessful) {
                    _eliminarState.value = UiState.Success(Unit)
                    cargarMisPublicaciones()
                } else {
                    _eliminarState.value = UiState.Error("Error al eliminar: ${r.code()}")
                }
            } catch (e: Exception) {
                _eliminarState.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    private val _vendidoState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val vendidoState: LiveData<UiState<Unit>> = _vendidoState

    fun marcarVendido(producto: com.unicundi.unimarket.data.model.Producto) {
        _vendidoState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val request = com.unicundi.unimarket.data.model.ProductoRequest(
                    titulo      = producto.titulo,
                    descripcion = producto.descripcion,
                    precio      = producto.precio,
                    categoria   = producto.categoria,
                    estado      = "vendido",
                    ubicacion   = producto.ubicacion,
                    usuarioId   = producto.usuarioId
                )
                val r = api.actualizarProducto(producto.id, request)
                if (r.isSuccessful) {
                    _vendidoState.value = UiState.Success(Unit)
                    cargarMisPublicaciones()
                } else {
                    _vendidoState.value = UiState.Error("Error al actualizar: ${r.code()}")
                }
            } catch (e: Exception) {
                _vendidoState.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    private val _favoritos = MutableLiveData<UiState<List<Producto>>>(UiState.Success(emptyList()))
    val favoritos: LiveData<UiState<List<Producto>>> = _favoritos

    fun cargarFavoritos() {
        if (Sesion.usuarioId == 0L) {
            _favoritos.value = UiState.Success(FavoritosManager.obtener(getApplication()))
            return
        }
        _favoritos.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getFavoritos(Sesion.usuarioId)
                _favoritos.value = if (r.isSuccessful) UiState.Success(r.body() ?: emptyList())
                else UiState.Success(FavoritosManager.obtener(getApplication()))
            } catch (e: Exception) {
                _favoritos.value = UiState.Success(FavoritosManager.obtener(getApplication()))
            }
        }
    }

    fun quitarFavorito(productoId: Long) {
        FavoritosManager.quitar(getApplication(), productoId)
        if (Sesion.usuarioId == 0L) { cargarFavoritos(); return }
        viewModelScope.launch {
            try { api.removeFavorito(Sesion.usuarioId, productoId) }
            catch (e: Exception) { /* silencioso */ }
            cargarFavoritos()
        }
    }

    private val _updatePerfilState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val updatePerfilState: LiveData<UiState<Unit>> = _updatePerfilState

    fun actualizarNombre(nombre: String) {
        if (Sesion.usuarioId == 0L) return
        viewModelScope.launch {
            try {
                val r = api.actualizarNombre(Sesion.usuarioId, nombre)
                if (r.isSuccessful) {
                    Sesion.nombre = nombre
                    _updatePerfilState.value = UiState.Success(Unit)
                } else {
                    _updatePerfilState.value = UiState.Error("Error ${r.code()}")
                }
            } catch (e: Exception) {
                _updatePerfilState.value = UiState.Error("Sin conexion: ${e.message}")
            }
        }
    }

    fun subirFotoPerfil(context: android.content.Context, imageUri: Uri) {
        if (Sesion.usuarioId == 0L) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val part = okhttp3.MultipartBody.Part.createFormData(
                    "file", "foto_perfil.jpg",
                    bytes.toRequestBody("image/*".toMediaTypeOrNull())
                )
                val r = api.subirFotoPerfil(Sesion.usuarioId, part)
                if (r.isSuccessful) {
                    // Guardar la URL real de Cloudinary en SharedPreferences
                    val cloudinaryUrl = r.body()?.string()?.trim() ?: ""
                    if (cloudinaryUrl.isNotBlank()) {
                        context.getSharedPreferences("sesion", android.content.Context.MODE_PRIVATE)
                            .edit().putString("fotoPerfil", cloudinaryUrl).apply()
                        android.util.Log.d("UniMarket", "Foto perfil guardada: $cloudinaryUrl")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UniMarket", "Error subiendo foto perfil: ${e.message}")
            }
        }
    }
}

// ─── MENSAJES VIEWMODEL ───────────────────────────────────────
class MensajesViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _mensajes = MutableLiveData<UiState<List<Mensaje>>>()
    val mensajes: LiveData<UiState<List<Mensaje>>> = _mensajes

    // Para la lista de conversaciones: agrupa mensajes por interlocutor con foto del interlocutor
    private val _conversaciones = MutableLiveData<UiState<List<ConversacionItem>>>()
    val conversaciones: LiveData<UiState<List<ConversacionItem>>> = _conversaciones

    // Para el historial dentro de un chat específico
    private val _mensajesChat = MutableLiveData<UiState<List<Mensaje>>>()
    val mensajesChat: LiveData<UiState<List<Mensaje>>> = _mensajesChat

    fun cargarConversaciones() {
        if (Sesion.usuarioId == 0L) {
            _conversaciones.value = UiState.Success(emptyList())
            return
        }
        _conversaciones.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getMensajes()
                if (r.isSuccessful) {
                    val misMensajes = (r.body() ?: emptyList()).filter {
                        it.senderId == Sesion.usuarioId || it.receiverId == Sesion.usuarioId
                    }
                    // Último mensaje por interlocutor + foto del interlocutor
                    val items = misMensajes
                        .groupBy { msg ->
                            if (msg.senderId == Sesion.usuarioId) msg.receiverId else msg.senderId
                        }
                        .map { (interlocutorId, msgs) ->
                            val foto = try {
                                val userResp = api.getUsuarioById(interlocutorId)
                                if (userResp.isSuccessful) userResp.body()?.fotoPerfil else null
                            } catch (e: Exception) { null }
                            val ultimoVisible = msgs.lastOrNull { !it.message.contains("__xpin_prod__") }
                                ?: msgs.last()
                            ConversacionItem(ultimoVisible, foto)
                        }
                    _conversaciones.value = UiState.Success(items)
                } else {
                    _conversaciones.value = UiState.Error("Error ${r.code()}")
                }
            } catch (e: Exception) {
                _conversaciones.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    fun cargarMensajesChat(otroUsuarioId: Long) {
        if (Sesion.usuarioId == 0L) {
            _mensajesChat.value = UiState.Success(emptyList())
            return
        }
        _mensajesChat.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getMensajes()
                if (r.isSuccessful) {
                    val conversacion = (r.body() ?: emptyList())
                        .filter { msg ->
                            (msg.senderId == Sesion.usuarioId && msg.receiverId == otroUsuarioId) ||
                            (msg.senderId == otroUsuarioId && msg.receiverId == Sesion.usuarioId)
                        }
                        .sortedBy { it.id }
                    _mensajesChat.value = UiState.Success(conversacion)
                } else {
                    _mensajesChat.value = UiState.Error("Error ${r.code()}")
                }
            } catch (e: Exception) {
                _mensajesChat.value = UiState.Error("Sin conexion con el servidor")
            }
        }
    }

    private val _enviarState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val enviarState: LiveData<UiState<Unit>> = _enviarState

    fun enviarMensaje(receiverId: Long, contenido: String) {
        if (Sesion.usuarioId == 0L) {
            _enviarState.value = UiState.Error("No hay sesión activa")
            return
        }
        viewModelScope.launch {
            try {
                val r = api.enviarMensaje(MensajeRequest(contenido, Sesion.usuarioId, receiverId))
                if (r.isSuccessful) {
                    _enviarState.value = UiState.Success(Unit)
                } else {
                    val errorBody = r.errorBody()?.string() ?: ""
                    android.util.Log.e("UniMarket", "enviarMensaje error ${r.code()}: $errorBody")
                    _enviarState.value = UiState.Error("Error ${r.code()}: $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("UniMarket", "enviarMensaje exception: ${e.message}")
                _enviarState.value = UiState.Error("Sin conexión: ${e.message}")
            }
        }
    }
}

// ─── SOLICITUDES DE COMPRA VIEWMODEL ─────────────────────────
class SolicitudViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _solicitudes = MutableLiveData<UiState<List<SolicitudCompra>>>()
    val solicitudes: LiveData<UiState<List<SolicitudCompra>>> = _solicitudes

    private val _crearState = MutableLiveData<UiState<SolicitudCompra>>(UiState.Idle)
    val crearState: LiveData<UiState<SolicitudCompra>> = _crearState

    fun cargarSolicitudes() {
        _solicitudes.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.getSolicitudes()
                _solicitudes.value = if (r.isSuccessful) UiState.Success(r.body() ?: emptyList())
                else UiState.Error("Error ${r.code()}")
            } catch (e: Exception) {
                _solicitudes.value = UiState.Error("Sin conexion")
            }
        }
    }

    fun crearSolicitud(postId: Long, sellerId: Long) {
        if (Sesion.usuarioId == 0L) return
        _crearState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = api.crearSolicitud(
                    SolicitudCompraRequest(postId, Sesion.usuarioId, sellerId)
                )
                _crearState.value = if (r.isSuccessful) UiState.Success(r.body()!!)
                else UiState.Error("Error ${r.code()}")
            } catch (e: Exception) {
                _crearState.value = UiState.Error("Sin conexion")
            }
        }
    }
}