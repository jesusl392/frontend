package com.unicundi.unimarket.ui.fragment

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.unicundi.unimarket.R
import com.unicundi.unimarket.data.model.Sesion
import com.unicundi.unimarket.databinding.*
import com.unicundi.unimarket.ui.activity.MainActivity
import com.unicundi.unimarket.ui.adapter.*
import com.unicundi.unimarket.ui.viewmodel.*

// ─── HOME FRAGMENT ────────────────────────────────────────────
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapterProductos: ProductoAdapter
    private var panelVisible = false
    private var categoriaSeleccionada = "Todos"
    private var estadoSeleccionado = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nombre = Sesion.nombre.ifEmpty { "Estudiante" }
        binding.txtTituloHome.text = "UniMarket - Bienvenido, $nombre!"

        configurarRecycler()
        configurarCategorias()
        configurarBusqueda()
        configurarFiltros()
        configurarObservadores()
        viewModel.cargarProductos()
    }

    private fun configurarRecycler() {
        adapterProductos = ProductoAdapter { producto ->
            val bundle = Bundle().apply { putLong("productoId", producto.id) }
            findNavController().navigate(R.id.action_home_to_detalle, bundle)
        }
        binding.recyclerProductos.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerProductos.adapter = adapterProductos
    }

    private fun configurarCategorias() {
        val categorias = listOf("Todos", "Libros", "Electronica", "Ropa", "Deporte", "Otros")
        val adapterCat = CategoriaAdapter(categorias, categoriaSeleccionada) { cat ->
            categoriaSeleccionada = cat
            viewModel.filtrarPorCategoria(cat)
        }
        binding.recyclerCategorias.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCategorias.adapter = adapterCat
    }

    private fun configurarBusqueda() {
        binding.etBuscar.addTextChangedListener { texto ->
            val query = texto.toString().trim()
            if (query.length >= 2) viewModel.buscarProductos(query)
            else if (query.isEmpty()) viewModel.cargarProductos()
        }
    }

    private fun configurarFiltros() {
        val panel = binding.includeFiltros

        binding.btnFiltros.setOnClickListener {
            panelVisible = !panelVisible
            if (panelVisible) {
                panel.panelFiltros.visibility = View.VISIBLE
                panel.panelFiltros.animate().translationY(0f).alpha(1f).setDuration(250).start()
            } else {
                panel.panelFiltros.animate().alpha(0f).setDuration(200).withEndAction {
                    panel.panelFiltros.visibility = View.GONE
                }.start()
            }
        }

        binding.btnMenu.setOnClickListener {
            (activity as? MainActivity)?.abrirDrawer()
        }

        val chipsCategoria = mapOf(
            panel.chipTodos to "Todos", panel.chipLibros to "Libros",
            panel.chipElectronica to "Electronica", panel.chipRopa to "Ropa",
            panel.chipDeporte to "Deporte", panel.chipOtros to "Otros"
        )
        chipsCategoria.forEach { (chip, valor) ->
            chip.setOnClickListener {
                categoriaSeleccionada = valor
                actualizarChips(chipsCategoria.keys.toList(), chip)
            }
        }

        val chipsEstado = mapOf(
            panel.chipNuevo to "activo", panel.chipComoNuevo to "activo", panel.chipUsado to "vendido"
        )
        chipsEstado.forEach { (chip, _) ->
            chip.setOnClickListener {
                actualizarChipsEstado(chipsEstado.keys.toList(), chip)
            }
        }

        panel.btnAplicarFiltros.setOnClickListener {
            val precioMin = panel.etPrecioMin.text.toString().trim().toDoubleOrNull()
            val precioMax = panel.etPrecioMax.text.toString().trim().toDoubleOrNull()

            if (precioMin != null && precioMax != null && precioMin > precioMax) {
                panel.etPrecioMin.error = "El minimo no puede ser mayor al maximo"
                return@setOnClickListener
            }

            viewModel.filtrar(categoriaSeleccionada, precioMin, precioMax)
            panelVisible = false
            panel.panelFiltros.visibility = View.GONE
            Toast.makeText(requireContext(), "Filtros aplicados", Toast.LENGTH_SHORT).show()
        }

        panel.btnLimpiarFiltros.setOnClickListener {
            categoriaSeleccionada = "Todos"
            panel.etPrecioMin.text?.clear()
            panel.etPrecioMax.text?.clear()
            actualizarChips(chipsCategoria.keys.toList(), panel.chipTodos)
            actualizarChipsEstado(chipsEstado.keys.toList(), null)
            viewModel.cargarProductos()
        }
    }

    private fun actualizarChips(chips: List<TextView>, seleccionado: TextView) {
        chips.forEach { chip ->
            val activo = chip == seleccionado
            chip.background = ContextCompat.getDrawable(requireContext(),
                if (activo) R.drawable.bg_categoria_seleccionada else R.drawable.bg_categoria_normal)
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                if (activo) R.color.blanco else R.color.texto_secundario))
        }
    }

    private fun actualizarChipsEstado(chips: List<TextView>, seleccionado: TextView?) {
        chips.forEach { chip ->
            val activo = chip == seleccionado
            chip.background = ContextCompat.getDrawable(requireContext(),
                if (activo) R.drawable.bg_categoria_seleccionada else R.drawable.bg_categoria_normal)
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                if (activo) R.color.blanco else R.color.texto_secundario))
        }
    }

    private fun configurarObservadores() {
        viewModel.productos.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> binding.progressHome.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressHome.visibility = View.GONE
                    adapterProductos.submitList(estado.data)
                }
                is UiState.Error -> {
                    binding.progressHome.visibility = View.GONE
                    Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                }
                else -> binding.progressHome.visibility = View.GONE
            }
        }

        viewModel.destacados.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Success && estado.data.isNotEmpty()) {
                val p = estado.data.first()
                binding.txtDestacadoTitulo.text = p.titulo
                binding.txtDestacadoPrecio.text = "$${String.format("%.0f", p.precio)}"
                val urlDestacado = p.imagenes?.firstOrNull()?.url
                if (!urlDestacado.isNullOrBlank()) {
                    com.bumptech.glide.Glide.with(this)
                        .load(urlDestacado)
                        .centerCrop()
                        .into(binding.imgDestacado)
                }
                binding.cardDestacado.setOnClickListener {
                    val bundle = Bundle().apply { putLong("productoId", p.id) }
                    findNavController().navigate(R.id.action_home_to_detalle, bundle)
                }
            }
        }

        binding.btnEmpezarAhora.setOnClickListener {
            findNavController().navigate(R.id.publicarFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (panelVisible) {
                        panelVisible = false
                        binding.includeFiltros.panelFiltros.visibility = View.GONE
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── MENSAJES FRAGMENT ────────────────────────────────────────
class MensajesFragment : Fragment() {

    private var _binding: FragmentMensajesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MensajesViewModel by viewModels()
    private lateinit var adapter: ConversacionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMensajesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Limpiar el punto del ícono de mensajes al abrir la pestaña
        (activity as? com.unicundi.unimarket.ui.activity.MainActivity)?.limpiarBadgeMensajes()

        adapter = ConversacionAdapter { item ->
            val esEnviado = item.mensaje.senderId == Sesion.usuarioId
            val nombreInterlocutor = if (esEnviado) item.mensaje.receiverNombre else item.mensaje.senderNombre
            val idInterlocutor = if (esEnviado) item.mensaje.receiverId else item.mensaje.senderId
            val bundle = Bundle().apply {
                putString("nombreVendedor", nombreInterlocutor)
                putString("tituloProducto", "")
                putLong("receiverId", idInterlocutor)
                putString("fotoVendedor", item.interlocutorFoto)
            }
            findNavController().navigate(R.id.action_mensajes_to_chat, bundle)
        }
        binding.recyclerMensajes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMensajes.adapter = adapter

        viewModel.conversaciones.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> binding.progressMensajes.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressMensajes.visibility = View.GONE
                    if (estado.data.isEmpty()) {
                        binding.layoutVacio.visibility = View.VISIBLE
                        binding.recyclerMensajes.visibility = View.GONE
                    } else {
                        binding.layoutVacio.visibility = View.GONE
                        binding.recyclerMensajes.visibility = View.VISIBLE
                        adapter.submitList(estado.data)
                    }
                }
                is UiState.Error -> {
                    binding.progressMensajes.visibility = View.GONE
                    binding.layoutVacio.visibility = View.VISIBLE
                }
                else -> {}
            }
        }

        viewModel.cargarConversaciones()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarConversaciones()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── PERFIL FRAGMENT ──────────────────────────────────────────
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()
    private lateinit var adapterPublicaciones: MisPublicacionesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapterPublicaciones = MisPublicacionesAdapter { producto ->
            val bundle = Bundle().apply { putLong("productoId", producto.id) }
            findNavController().navigate(R.id.action_perfil_to_detalle, bundle)
        }
        binding.recyclerMisPublicaciones.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerMisPublicaciones.adapter = adapterPublicaciones

        binding.btnVolverInicio.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        binding.layoutAvatar.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_editar)
        }

        binding.btnEditarPerfil.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_editar)
        }

        viewModel.perfil.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Success) {
                val u = estado.data
                binding.txtNombre.text = u.nombre
                binding.txtFacultad.text = u.correo
                binding.txtVentas.text = "0"
                binding.txtCalificacion.text = "—"
                binding.txtFavoritos.text = "0"
                // Cargar foto de perfil si existe
                val fotoPerfil = u.fotoPerfil
                if (!fotoPerfil.isNullOrBlank()) {
                    Glide.with(binding.imgAvatarPerfil.context)
                        .load(fotoPerfil)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.imgAvatarPerfil)
                }
            }
        }

        viewModel.misPublicaciones.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Success) adapterPublicaciones.submitList(estado.data)
        }

        viewModel.cargarPerfil()
        viewModel.cargarMisPublicaciones()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarPerfil()
        viewModel.cargarMisPublicaciones()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── PUBLICAR FRAGMENT ────────────────────────────────────────
class PublicarFragment : Fragment() {

    private var _binding: FragmentPublicarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductoViewModel by viewModels()
    private var imagenSeleccionada: android.net.Uri? = null

    private val seleccionarImagen = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagenSeleccionada = uri
            binding.imgProductoSeleccionada.setImageURI(uri)
            binding.imgProductoSeleccionada.visibility = View.VISIBLE
            binding.layoutSubirFoto.visibility = View.GONE
            binding.btnCambiarFoto.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPublicarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }
        binding.layoutSubirFoto.setOnClickListener { seleccionarImagen.launch("image/*") }
        binding.btnCambiarFoto.setOnClickListener { seleccionarImagen.launch("image/*") }
        binding.btnPublicar.setOnClickListener { publicar() }
        binding.spinnerCategoria.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (position != 0) binding.txtErrorCategoria.visibility = View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        viewModel.publicarState.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> {
                    binding.btnPublicar.isEnabled = false
                    binding.btnPublicar.text = "Publicando..."
                }
                is UiState.Success -> {
                    // Si hay imagen seleccionada, subirla asociada al producto recién creado
                    val uri = imagenSeleccionada
                    if (uri != null) {
                        viewModel.subirImagenProducto(requireContext(), uri, estado.data.id)
                    }
                    Toast.makeText(requireContext(), "Producto publicado exitosamente", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    binding.btnPublicar.isEnabled = true
                    binding.btnPublicar.text = "Publicar Producto"
                    Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                }
                else -> { binding.btnPublicar.isEnabled = true }
            }
        }
    }

    private fun publicar() {
        val titulo = binding.etTitulo.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val precioStr = binding.etPrecio.text.toString().trim()

        if (titulo.isEmpty()) { binding.etTitulo.error = "Obligatorio"; return }
        if (descripcion.isEmpty()) { binding.etDescripcion.error = "Obligatorio"; return }
        if (precioStr.isEmpty()) { binding.etPrecio.error = "Obligatorio"; return }

        val precio = precioStr.toDoubleOrNull()
        if (precio == null) { binding.etPrecio.error = "Precio invalido"; return }

        if (binding.spinnerCategoria.selectedItemPosition == 0) {
            binding.txtErrorCategoria.visibility = View.VISIBLE
            return
        }
        binding.txtErrorCategoria.visibility = View.GONE

        val categoria = binding.spinnerCategoria.selectedItem.toString()
        val ubicacion = binding.etUbicacion.text.toString().trim().ifEmpty { null }
        viewModel.publicarProducto(titulo, descripcion, precio, categoria, "activo", ubicacion)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── DETALLE PRODUCTO FRAGMENT ────────────────────────────────
class DetalleProductoFragment : Fragment() {

    private var _binding: FragmentDetalleProductoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductoViewModel by viewModels()
    private var esFavorito = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleProductoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productoId = arguments?.getLong("productoId") ?: 0L

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }

        // Verificar si ya es favorito al abrir
        esFavorito = com.unicundi.unimarket.data.model.FavoritosManager.esFavorito(requireContext(), productoId)
        actualizarIconoFavorito()

        binding.btnFavorito.setOnClickListener {
            val productoActual = (viewModel.productoDetalle.value as? UiState.Success)?.data ?: return@setOnClickListener
            esFavorito = !esFavorito
            if (esFavorito) {
                com.unicundi.unimarket.data.model.FavoritosManager.agregar(requireContext(), productoActual)
                viewModel.agregarFavoritoServidor(productoActual.id)
            } else {
                com.unicundi.unimarket.data.model.FavoritosManager.quitar(requireContext(), productoActual.id)
                viewModel.quitarFavoritoServidor(productoActual.id)
            }
            actualizarIconoFavorito()
            val msg = if (esFavorito) "Agregado a favoritos" else "Quitado de favoritos"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.productoDetalle.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Loading -> binding.progressDetalle.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressDetalle.visibility = View.GONE
                    val p = estado.data
                    binding.txtTitulo.text = p.titulo
                    binding.txtPrecio.text = "$${String.format("%.0f", p.precio)}"
                    binding.txtDescripcion.text = p.descripcion
                    binding.txtCategoria.text = p.categoria
                    binding.txtEstado.text = p.estado
                    binding.txtUbicacion.text = p.ubicacion?.ifBlank { null } ?: "Universidad de Cundinamarca"
                    binding.txtVendedorNombre.text = p.usuarioNombre
                    binding.txtVendedorFacultad.text = "Estudiante UniCundi"
                    binding.txtVendedorCalificacion.text = "—"

                    // Cargar foto del vendedor
                    if (!p.usuarioFoto.isNullOrBlank()) {
                        Glide.with(this)
                            .load(p.usuarioFoto)
                            .circleCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(binding.imgAvatarVendedor)
                    }

                    // Cargar primera imagen del producto
                    val urlImagen = p.imagenes?.firstOrNull()?.url
                    if (!urlImagen.isNullOrBlank()) {
                        Glide.with(this)
                            .load(urlImagen)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(binding.imgDetalleProducto)
                    }

                    binding.btnContactarVendedor.setOnClickListener {
                        val bundle = Bundle().apply {
                            putString("nombreVendedor", p.usuarioNombre)
                            putString("tituloProducto", p.titulo)
                            putLong("receiverId", p.usuarioId)
                            putString("fotoVendedor", p.usuarioFoto)
                            putString("imagenProducto", p.imagenes?.firstOrNull()?.url)
                        }
                        findNavController().navigate(R.id.action_detalle_to_chat, bundle)
                    }
                }
                is UiState.Error -> {
                    binding.progressDetalle.visibility = View.GONE
                    Toast.makeText(requireContext(), estado.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.cargarDetalle(productoId)
    }

    private fun actualizarIconoFavorito() {
        val icono = if (esFavorito) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        binding.btnFavorito.setImageResource(icono)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
