package com.unicundi.unimarket.ui.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.unicundi.unimarket.data.model.Mensaje
import com.unicundi.unimarket.data.model.Sesion
import com.unicundi.unimarket.data.network.StompClient
import com.unicundi.unimarket.R
import com.unicundi.unimarket.databinding.FragmentChatBinding
import com.unicundi.unimarket.ui.adapter.ChatAdapter
import com.unicundi.unimarket.ui.adapter.MensajeChat
import com.unicundi.unimarket.ui.viewmodel.MensajesViewModel
import com.unicundi.unimarket.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatAdapter
    private val viewModel: MensajesViewModel by viewModels()
    private val mensajesLocales = mutableListOf<MensajeChat>()
    private var receiverId: Long = 0L
    private var historialCargado = false

    // WebSocket
    private var stompClient: StompClient? = null
    private var ultimoIdHistorial = 0L   // evita duplicados si el WS llega antes que el historial
    private val gson = Gson()

    companion object {
        private const val PROD_TAG = "​__xpin_prod__​"
        private const val PROD_SEP = "​||t||​"
        private const val WS_URL = "wss://marketplace-final-ncu7.onrender.com/ws"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtNombreChat.text = arguments?.getString("nombreVendedor") ?: "Usuario"
        binding.txtProductoChat.text = "Sobre: ${arguments?.getString("tituloProducto") ?: ""}"

        receiverId = arguments?.getLong("receiverId") ?: 0L

        // Avatar del interlocutor
        val fotoVendedorUrl = arguments?.getString("fotoVendedor")
        if (!fotoVendedorUrl.isNullOrBlank()) {
            Glide.with(binding.imgAvatarChatHeader.context)
                .load(fotoVendedorUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.imgAvatarChatHeader)
        }

        // Si viene desde "Contactar vendedor", mostrar preview de inmediato
        val imagenProductoArg = arguments?.getString("imagenProducto")
        val tituloProductoArg = arguments?.getString("tituloProducto") ?: ""
        if (!imagenProductoArg.isNullOrBlank()) {
            mostrarPreviewProducto(imagenProductoArg, tituloProductoArg)
            if (receiverId > 0L) {
                val refMsg = "$PROD_TAG$imagenProductoArg$PROD_SEP$tituloProductoArg"
                viewModel.enviarMensaje(receiverId, refMsg)
            }
        }

        binding.btnVolverChat.setOnClickListener { findNavController().popBackStack() }

        // Tocar avatar o nombre → perfil público del interlocutor
        val irAPerfil = View.OnClickListener {
            if (receiverId > 0L) {
                val bundle = Bundle().apply { putLong("userId", receiverId) }
                findNavController().navigate(R.id.action_chat_to_perfilPublico, bundle)
            }
        }
        binding.imgAvatarChatHeader.setOnClickListener(irAPerfil)
        binding.txtNombreChat.setOnClickListener(irAPerfil)

        adapter = ChatAdapter()
        binding.recyclerChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerChat.adapter = adapter

        // ── Historial REST ──────────────────────────────────────────────────────
        viewModel.mensajesChat.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> {}
                is UiState.Success -> {
                    if (!historialCargado) {
                        historialCargado = true

                        // Guardar el ID máximo del historial para no duplicar mensajes WS
                        ultimoIdHistorial = estado.data.maxOfOrNull { it.id } ?: 0L

                        var urlProd: String? = null
                        var tituloProd: String? = null

                        val historial = estado.data.mapNotNull { msg ->
                            if (msg.message.startsWith(PROD_TAG)) {
                                val contenido = msg.message.removePrefix(PROD_TAG)
                                val sepIdx = contenido.indexOf(PROD_SEP)
                                if (sepIdx > 0) {
                                    urlProd = contenido.substring(0, sepIdx)
                                    tituloProd = contenido.substring(sepIdx + PROD_SEP.length)
                                }
                                null
                            } else {
                                MensajeChat(msg.message, extraerHora(msg.dateTime), msg.senderId == Sesion.usuarioId)
                            }
                        }

                        if (imagenProductoArg.isNullOrBlank() && !urlProd.isNullOrBlank()) {
                            mostrarPreviewProducto(urlProd!!, tituloProd ?: "")
                        }

                        val nuevosEnviados = mensajesLocales.drop(historial.size.coerceAtMost(mensajesLocales.size))
                        mensajesLocales.clear()
                        mensajesLocales.addAll(historial)
                        mensajesLocales.addAll(nuevosEnviados)
                        adapter.submitList(mensajesLocales.toList())
                        scrollAlFinal()

                        // Conectar WebSocket después de cargar el historial
                        if (Sesion.usuarioId > 0L && stompClient == null) {
                            conectarWebSocket()
                        }
                    }
                }
                is UiState.Error -> {}
                else -> {}
            }
        }

        // ── Errores al enviar ───────────────────────────────────────────────────
        viewModel.enviarState.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Error) {
                Toast.makeText(requireContext(), "Error al enviar: ${estado.message}", Toast.LENGTH_LONG).show()
            }
        }

        // ── Enviar mensaje ──────────────────────────────────────────────────────
        binding.btnEnviar.setOnClickListener {
            val texto = binding.etMensaje.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener

            if (Sesion.usuarioId == 0L) {
                Toast.makeText(requireContext(), "Debes iniciar sesión para enviar mensajes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (receiverId == 0L) {
                Toast.makeText(requireContext(), "Error: destinatario no definido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            mensajesLocales.add(MensajeChat(texto, hora, true))
            adapter.submitList(mensajesLocales.toList())
            binding.etMensaje.text?.clear()
            scrollAlFinal()

            // Envía por REST → el backend guarda y hace broadcast WS al destinatario
            viewModel.enviarMensaje(receiverId, texto)
        }

        if (receiverId > 0L) {
            viewModel.cargarMensajesChat(receiverId)
        }
    }

    // ── WebSocket ───────────────────────────────────────────────────────────────

    private fun conectarWebSocket() {
        stompClient = StompClient(wsUrl = WS_URL, userId = Sesion.usuarioId)
        stompClient?.conectar { jsonBody ->
            try {
                val msg = gson.fromJson(jsonBody, Mensaje::class.java)

                // Solo mostrar mensajes del interlocutor actual, no los propios
                // ni mensajes ya presentes en el historial
                if (msg.senderId != receiverId) return@conectar
                if (msg.id > 0 && msg.id <= ultimoIdHistorial) return@conectar

                // Referencia de producto → actualizar preview, no mostrar como burbuja
                if (msg.message.startsWith(PROD_TAG)) {
                    val contenido = msg.message.removePrefix(PROD_TAG)
                    val sepIdx = contenido.indexOf(PROD_SEP)
                    if (sepIdx > 0) {
                        val url = contenido.substring(0, sepIdx)
                        val titulo = contenido.substring(sepIdx + PROD_SEP.length)
                        activity?.runOnUiThread { mostrarPreviewProducto(url, titulo) }
                    }
                    return@conectar
                }

                val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                activity?.runOnUiThread {
                    mensajesLocales.add(MensajeChat(msg.message, hora, false))
                    adapter.submitList(mensajesLocales.toList())
                    scrollAlFinal()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatFragment", "Error parseando mensaje WS: ${e.message}")
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun mostrarPreviewProducto(url: String, titulo: String) {
        binding.cardProductoPreview.visibility = View.VISIBLE
        binding.txtProductoPreviewTitulo.text = titulo
        Glide.with(binding.imgProductoPreview.context)
            .load(url)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(binding.imgProductoPreview)
    }

    private fun extraerHora(dateTime: String?): String {
        if (dateTime.isNullOrBlank()) return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return try {
            val formatos = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            for (formato in formatos) {
                try {
                    val date = SimpleDateFormat(formato, Locale.getDefault()).parse(dateTime)
                    if (date != null) return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                } catch (_: Exception) {}
            }
            dateTime.take(5)
        } catch (e: Exception) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    private fun scrollAlFinal() {
        binding.recyclerChat.postDelayed({
            val ultimo = adapter.itemCount - 1
            if (ultimo >= 0) binding.recyclerChat.scrollToPosition(ultimo)
        }, 100)
    }

    override fun onDestroyView() {
        stompClient?.desconectar()
        stompClient = null
        super.onDestroyView()
        _binding = null
    }
}
