package com.unicundi.unimarket.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.unicundi.unimarket.R
import com.unicundi.unimarket.data.model.Sesion
import com.unicundi.unimarket.databinding.ActivityLoginBinding
import com.unicundi.unimarket.ui.viewmodel.AuthViewModel
import com.unicundi.unimarket.ui.viewmodel.UiState

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private var mostrarContrasena = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay sesión guardada, ir directo al home
        val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
        Sesion.cargarDesdePrefs(prefs)
        if (Sesion.estaLogueado) {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }

        configurarUI()
        configurarObservadores()
    }

    private fun configurarUI() {
        // Toggle contraseña
        binding.btnTogglePassword.setOnClickListener {
            mostrarContrasena = !mostrarContrasena
            binding.etContrasena.inputType = if (mostrarContrasena)
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.etContrasena.setSelection(binding.etContrasena.text?.length ?: 0)
        }

        // Iniciar sesión
        binding.btnIniciarSesion.setOnClickListener {
            val correo = binding.etCorreo.text.toString().trim()
            val password = binding.etContrasena.text.toString().trim()

            if (correo.isEmpty()) {
                binding.etCorreo.error = "Ingresa tu correo"
                return@setOnClickListener
            }
            if (!correo.contains("@")) {
                binding.etCorreo.error = "Correo no valido"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etContrasena.error = "Ingresa tu contrasena"
                return@setOnClickListener
            }
            viewModel.login(correo, password)
        }

        // Registrarse — abre dialogo para pedir solo el correo
        binding.btnRegistrarse.setOnClickListener {
            mostrarDialogoRegistro()
        }

        binding.btnOlvideContrasena.setOnClickListener {
            Toast.makeText(this, "Contacta a soporte institucional", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoRegistro() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_registro, null)

        val tilCorreo   = dialogView.findViewById<TextInputLayout>(R.id.tilCorreo)
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirmar = dialogView.findViewById<TextInputLayout>(R.id.tilConfirmar)
        val etCorreo    = dialogView.findViewById<TextInputEditText>(R.id.etCorreo)
        val etPassword  = dialogView.findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmar = dialogView.findViewById<TextInputEditText>(R.id.etConfirmar)

        val dialog = MaterialAlertDialogBuilder(this, R.style.DialogoRegistro)
            .setTitle("Crear cuenta")
            .setMessage("Ingresa tu correo y una contraseña.\nTe enviaremos un código de verificación.")
            .setView(dialogView)
            .setPositiveButton("Enviar código", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btnPositivo = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            btnPositivo.setTextColor(getColor(R.color.verde_principal))
            btnPositivo.setTypeface(null, android.graphics.Typeface.BOLD)

            btnPositivo.setOnClickListener {
                tilCorreo.error = null
                tilPassword.error = null
                tilConfirmar.error = null

                val correo    = etCorreo.text.toString().trim()
                val password  = etPassword.text.toString().trim()
                val confirmar = etConfirmar.text.toString().trim()

                if (correo.isEmpty()) {
                    tilCorreo.error = "Ingresa tu correo"; return@setOnClickListener
                }
                if (!correo.contains("@")) {
                    tilCorreo.error = "Correo no válido"; return@setOnClickListener
                }
                if (password.length < 8) {
                    tilPassword.error = "Mínimo 8 caracteres"; return@setOnClickListener
                }
                if (password != confirmar) {
                    tilConfirmar.error = "Las contraseñas no coinciden"; return@setOnClickListener
                }

                correoRegistro   = correo
                passwordRegistro = password
                viewModel.enviarCodigo(correo)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private var correoRegistro   = ""
    private var passwordRegistro = ""

    private fun configurarObservadores() {
        // Observar login
        viewModel.loginState.observe(this) { estado ->
            when (estado) {
                is UiState.Loading -> {
                    binding.progressLogin.visibility = View.VISIBLE
                    binding.btnIniciarSesion.isEnabled = false
                    binding.btnIniciarSesion.text = "Verificando..."
                }
                is UiState.Success -> {
                    binding.progressLogin.visibility = View.GONE
                    binding.btnIniciarSesion.isEnabled = true
                    binding.btnIniciarSesion.text = "Iniciar Sesion"
                    // Guardar sesión si el checkbox está marcado
                    if (binding.checkMantenerSesion.isChecked) {
                        Sesion.guardarEnPrefs(getSharedPreferences("sesion", MODE_PRIVATE))
                    }
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                is UiState.Error -> {
                    binding.progressLogin.visibility = View.GONE
                    binding.btnIniciarSesion.isEnabled = true
                    binding.btnIniciarSesion.text = "Iniciar Sesion"
                    Toast.makeText(this, estado.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressLogin.visibility = View.GONE
                    binding.btnIniciarSesion.isEnabled = true
                }
            }
        }

        // Observar envio de codigo (para registro)
        viewModel.envioCodigoState.observe(this) { estado ->
            when (estado) {
                is UiState.Loading -> {
                    binding.progressLogin.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressLogin.visibility = View.GONE
                    // Codigo enviado → ir a pantalla de verificacion
                    val intent = Intent(this, VerificacionActivity::class.java)
                    intent.putExtra("correo", correoRegistro)
                    intent.putExtra("password", passwordRegistro)
                    intent.putExtra("modo", "registro")
                    startActivity(intent)
                }
                is UiState.Error -> {
                    binding.progressLogin.visibility = View.GONE
                    Toast.makeText(this, "Error al enviar codigo: ${estado.message}", Toast.LENGTH_LONG).show()
                }
                else -> binding.progressLogin.visibility = View.GONE
            }
        }
    }
}