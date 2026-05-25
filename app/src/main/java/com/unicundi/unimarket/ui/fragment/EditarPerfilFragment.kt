package com.unicundi.unimarket.ui.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.unicundi.unimarket.data.model.Sesion
import com.unicundi.unimarket.databinding.FragmentEditarPerfilBinding
import com.unicundi.unimarket.ui.viewmodel.PerfilViewModel
import com.unicundi.unimarket.ui.viewmodel.UiState

class EditarPerfilFragment : Fragment() {

    private var _binding: FragmentEditarPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()
    private var fotoUri: Uri? = null

    private val seleccionarFoto = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri
            Glide.with(binding.imgFotoPerfil.context)
                .load(uri)
                .circleCrop()
                .into(binding.imgFotoPerfil)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE)

        // Pre-llenar campos
        binding.etNombre.setText(Sesion.nombre)
        binding.txtCorreoFijo.text = Sesion.correo
        binding.etFacultad.setText(prefs.getString("facultad", ""))
        binding.etSemestre.setText(prefs.getString("semestre", ""))

        // Cargar foto de perfil actual si existe
        val fotoActual = prefs.getString("fotoPerfil", null)
        if (!fotoActual.isNullOrBlank()) {
            Glide.with(binding.imgFotoPerfil.context)
                .load(fotoActual)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.imgFotoPerfil)
        }

        binding.layoutFoto.setOnClickListener { seleccionarFoto.launch("image/*") }
        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }
        binding.btnGuardar.setOnClickListener { guardarCambios(prefs) }

        viewModel.updatePerfilState.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is UiState.Success -> {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar"
                    Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar"
                    Toast.makeText(requireContext(), estado.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun guardarCambios(prefs: android.content.SharedPreferences) {
        val nombre = binding.etNombre.text.toString().trim()
        val facultad = binding.etFacultad.text.toString().trim()
        val semestre = binding.etSemestre.text.toString().trim()
        val contrasenaNueva = binding.etContrasenaNueva.text.toString()
        val contrasenaConfirmar = binding.etContrasenaConfirmar.text.toString()

        if (nombre.isEmpty()) {
            binding.etNombre.error = "El nombre es obligatorio"
            binding.etNombre.requestFocus()
            return
        }
        if (contrasenaNueva.isNotEmpty()) {
            if (binding.etContrasenaActual.text.toString().isEmpty()) {
                binding.etContrasenaActual.error = "Ingresa tu contrasena actual"
                return
            }
            if (contrasenaNueva.length < 6) {
                binding.etContrasenaNueva.error = "Minimo 6 caracteres"; return
            }
            if (contrasenaNueva != contrasenaConfirmar) {
                binding.etContrasenaConfirmar.error = "Las contrasenas no coinciden"; return
            }
        }

        binding.btnGuardar.isEnabled = false
        binding.btnGuardar.text = "Guardando..."

        // Guardar facultad y semestre localmente (el backend no tiene estos campos)
        prefs.edit()
            .putString("facultad", facultad)
            .putString("semestre", semestre)
            .apply()

        // Subir foto si se seleccionó una nueva (la URL de Cloudinary se guarda en prefs async)
        val uri = fotoUri
        if (uri != null) {
            viewModel.subirFotoPerfil(requireContext(), uri)
        }

        // Actualizar nombre via API (y Sesion internamente)
        viewModel.actualizarNombre(nombre)

        // También actualizar prefs de sesión para persistir
        prefs.edit().putString("nombre", nombre).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
