package com.unicundi.unimarket.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.unicundi.unimarket.databinding.ActivityVerificacionBinding
import com.unicundi.unimarket.ui.viewmodel.AuthViewModel
import com.unicundi.unimarket.ui.viewmodel.UiState

class VerificacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificacionBinding
    private val viewModel: AuthViewModel by viewModels()
    private var correo:   String = ""
    private var password: String = ""
    private var modo:     String = "registro" // "registro" o "login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        correo   = intent.getStringExtra("correo")   ?: ""
        password = intent.getStringExtra("password") ?: ""
        modo     = intent.getStringExtra("modo")     ?: "registro"

        val correoMascarado = enmascararCorreo(correo)
        binding.txtCorreoEnviado.text =
            "Ingresa el codigo de 6 digitos enviado a $correoMascarado"

        configurarCajas()
        iniciarCuentaRegresiva()
        binding.btnVolver.setOnClickListener { finish() }
        binding.btnReenviar.setOnClickListener {
            viewModel.enviarCodigo(correo)
            iniciarCuentaRegresiva()
            Toast.makeText(this, "Codigo reenviado", Toast.LENGTH_SHORT).show()
        }

        configurarObservadores()
    }

    private fun configurarCajas() {
        val cajas = listOf(
            binding.etCodigo1, binding.etCodigo2, binding.etCodigo3,
            binding.etCodigo4, binding.etCodigo5, binding.etCodigo6
        )
        cajas.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < cajas.size - 1) cajas[index + 1].requestFocus()
                    if (cajas.all { it.text?.length == 1 }) verificar(cajas)
                }
            })
            editText.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && editText.text.isEmpty() && index > 0) {
                    cajas[index - 1].requestFocus(); cajas[index - 1].text?.clear()
                }
                false
            }
        }
    }

    private fun verificar(cajas: List<EditText>) {
        val codigo = cajas.joinToString("") { it.text.toString() }
        binding.progressVerificacion.visibility = View.VISIBLE
        // Verificar el código contra el backend
        viewModel.verificarCodigo(correo, codigo)
    }

    private fun configurarObservadores() {
        viewModel.envioCodigoState.observe(this) { estado ->
            when (estado) {
                is UiState.Loading -> binding.progressVerificacion.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressVerificacion.visibility = View.GONE
                    if (modo == "registro") {
                        // Código válido → pedir datos para completar el registro
                        mostrarDialogRegistro()
                    } else {
                        // En login no hay 2FA, esto no debería ocurrir
                        irAlHome()
                    }
                }
                is UiState.Error -> {
                    binding.progressVerificacion.visibility = View.GONE
                    Toast.makeText(this, estado.message, Toast.LENGTH_LONG).show()
                    limpiarCajas()
                }
                else -> binding.progressVerificacion.visibility = View.GONE
            }
        }

        viewModel.registroState.observe(this) { estado ->
            when (estado) {
                is UiState.Loading -> binding.progressVerificacion.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressVerificacion.visibility = View.GONE
                    Toast.makeText(this, "Cuenta creada. Inicia sesion.", Toast.LENGTH_LONG).show()
                    finish() // Volver al login
                }
                is UiState.Error -> {
                    binding.progressVerificacion.visibility = View.GONE
                    Toast.makeText(this, estado.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun mostrarDialogRegistro() {
        val codigoActual = listOf(
            binding.etCodigo1, binding.etCodigo2, binding.etCodigo3,
            binding.etCodigo4, binding.etCodigo5, binding.etCodigo6
        ).joinToString("") { it.text.toString() }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etNombre = android.widget.EditText(this).apply { hint = "Nombre completo" }
        layout.addView(etNombre)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Un ultimo paso")
            .setMessage("Ingresa tu nombre para completar el registro.")
            .setView(layout)
            .setPositiveButton("Registrarme", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = etNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    etNombre.error = "Ingresa tu nombre"
                    return@setOnClickListener
                }
                viewModel.registrar(nombre, correo, password, codigoActual)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun irAlHome() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun limpiarCajas() {
        listOf(binding.etCodigo1, binding.etCodigo2, binding.etCodigo3,
               binding.etCodigo4, binding.etCodigo5, binding.etCodigo6)
            .forEach { it.text?.clear() }
        binding.etCodigo1.requestFocus()
    }

    private fun iniciarCuentaRegresiva() {
        binding.btnReenviar.isEnabled = false
        object : CountDownTimer(60000, 1000) {
            override fun onTick(ms: Long) { binding.txtCuentaRegresiva.text = "Reenviar en ${ms/1000}s" }
            override fun onFinish() { binding.txtCuentaRegresiva.text = ""; binding.btnReenviar.isEnabled = true }
        }.start()
    }

    private fun enmascararCorreo(correo: String): String {
        val partes = correo.split("@")
        if (partes.size != 2) return correo
        val visible = if (partes[0].length > 3) partes[0].take(3) else partes[0].take(1)
        return "$visible***@${partes[1]}"
    }
}
