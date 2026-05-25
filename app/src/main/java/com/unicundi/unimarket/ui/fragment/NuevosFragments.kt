package com.unicundi.unimarket.ui.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicundi.unimarket.R
import com.unicundi.unimarket.databinding.*
import com.unicundi.unimarket.ui.adapter.MiProductoAdapter
import com.unicundi.unimarket.ui.adapter.FavoritoAdapter
import com.unicundi.unimarket.ui.viewmodel.*

// ─── MIS PUBLICACIONES FRAGMENT ──────────────────────────────
class MisPublicacionesFragment : Fragment() {

    private var _binding: FragmentMisPublicacionesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()
    private lateinit var adapter: MiProductoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisPublicacionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }

        adapter = MiProductoAdapter(
            onEliminar = { producto ->
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar publicacion")
                    .setMessage("¿Estas seguro de que quieres eliminar \"${producto.titulo}\"?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.eliminarProducto(producto.id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onVendido = { producto ->
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Marcar como vendido")
                    .setMessage("¿Has vendido el producto \"${producto.titulo}\"?")
                    .setPositiveButton("Si, vendido") { _, _ ->
                        viewModel.marcarVendido(producto)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onVerDetalle = { producto ->
                val bundle = android.os.Bundle().apply { putLong("productoId", producto.id) }
                findNavController().navigate(R.id.action_misPublicaciones_to_detalle, bundle)
            }
        )

        viewModel.eliminarState.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Success -> Toast.makeText(requireContext(), "Publicacion eliminada", Toast.LENGTH_SHORT).show()
                is UiState.Error -> Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }

        viewModel.vendidoState.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Success -> Toast.makeText(requireContext(), "Producto marcado como vendido", Toast.LENGTH_SHORT).show()
                is UiState.Error -> Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
        binding.recyclerMisPublicaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMisPublicaciones.adapter = adapter

        viewModel.misPublicaciones.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> {
                    binding.progressMisPublicaciones.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressMisPublicaciones.visibility = View.GONE
                    val lista = estado.data
                    adapter.submitList(lista)

                    // Estadísticas
                    val activas = lista.count { it.estado.equals("activo", ignoreCase = true) }
                    val vendidas = lista.count { it.estado.equals("vendido", ignoreCase = true) }
                    binding.txtTotalPublicaciones.text = lista.size.toString()
                    binding.txtPublicacionesActivas.text = activas.toString()
                    binding.txtPublicacionesVendidas.text = vendidas.toString()

                    if (lista.isEmpty()) {
                        binding.layoutVacioPublicaciones.visibility = View.VISIBLE
                        binding.recyclerMisPublicaciones.visibility = View.GONE
                    } else {
                        binding.layoutVacioPublicaciones.visibility = View.GONE
                        binding.recyclerMisPublicaciones.visibility = View.VISIBLE
                    }
                }
                is UiState.Error -> {
                    binding.progressMisPublicaciones.visibility = View.GONE
                    binding.layoutVacioPublicaciones.visibility = View.VISIBLE
                }
                else -> {}
            }
        }

        viewModel.cargarMisPublicaciones()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── FAVORITOS FRAGMENT ───────────────────────────────────────
class FavoritosFragment : Fragment() {

    private var _binding: FragmentFavoritosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()
    private lateinit var adapter: FavoritoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }

        adapter = FavoritoAdapter(
            onQuitarFavorito = { producto ->
                viewModel.quitarFavorito(producto.id)
                Toast.makeText(requireContext(), "Quitado de favoritos", Toast.LENGTH_SHORT).show()
            },
            onVerDetalle = { producto ->
                val bundle = android.os.Bundle().apply { putLong("productoId", producto.id) }
                findNavController().navigate(R.id.action_favoritos_to_detalle, bundle)
            }
        )
        binding.recyclerFavoritos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFavoritos.adapter = adapter

        viewModel.favoritos.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> binding.progressFavoritos.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressFavoritos.visibility = View.GONE
                    val lista = estado.data
                    adapter.submitList(lista)
                    binding.txtTotalFavoritos.text = "${lista.size} articulos"
                    if (lista.isEmpty()) {
                        binding.layoutVacioFavoritos.visibility = View.VISIBLE
                        binding.recyclerFavoritos.visibility = View.GONE
                    } else {
                        binding.layoutVacioFavoritos.visibility = View.GONE
                        binding.recyclerFavoritos.visibility = View.VISIBLE
                    }
                }
                is UiState.Error -> {
                    binding.progressFavoritos.visibility = View.GONE
                    binding.layoutVacioFavoritos.visibility = View.VISIBLE
                }
                else -> {}
            }
        }

        viewModel.cargarFavoritos()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── AYUDA FRAGMENT ───────────────────────────────────────────
class AyudaFragment : Fragment() {

    private var _binding: FragmentAyudaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAyudaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }

        // FAQs desplegables
        configurarFaq(binding.faqHeader1, binding.faqRespuesta1, binding.faqArrow1)
        configurarFaq(binding.faqHeader2, binding.faqRespuesta2, binding.faqArrow2)
        configurarFaq(binding.faqHeader3, binding.faqRespuesta3, binding.faqArrow3)
        configurarFaq(binding.faqHeader4, binding.faqRespuesta4, binding.faqArrow4)

        // Formulario de soporte
        binding.btnEnviarMensaje.setOnClickListener {
            val asunto = binding.etAsunto.text.toString().trim()
            val mensaje = binding.etMensajeSoporte.text.toString().trim()
            if (asunto.isEmpty()) {
                binding.etAsunto.error = "Ingresa el asunto"
                return@setOnClickListener
            }
            if (mensaje.isEmpty()) {
                binding.etMensajeSoporte.error = "Ingresa tu mensaje"
                return@setOnClickListener
            }
            binding.etAsunto.text?.clear()
            binding.etMensajeSoporte.text?.clear()
            Toast.makeText(requireContext(),
                "Mensaje enviado. Te responderemos pronto a tu correo institucional.",
                Toast.LENGTH_LONG).show()
        }

        // Redes sociales (simulación)
        binding.btnFacebook.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Facebook...", Toast.LENGTH_SHORT).show()
        }
        binding.btnInstagram.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Instagram...", Toast.LENGTH_SHORT).show()
        }
        binding.btnYoutube.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RDdQw4w9WgXcQ&start_radio=1"))
            startActivity(intent)
        }
    }

    private fun configurarFaq(header: View, respuesta: View, flecha: android.widget.ImageView) {
        var abierto = false
        header.setOnClickListener {
            abierto = !abierto
            if (abierto) {
                respuesta.visibility = View.VISIBLE
                flecha.animate().rotation(180f).setDuration(200).start()
            } else {
                respuesta.visibility = View.GONE
                flecha.animate().rotation(0f).setDuration(200).start()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
