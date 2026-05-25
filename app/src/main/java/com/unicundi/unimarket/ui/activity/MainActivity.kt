package com.unicundi.unimarket.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.work.*
import com.bumptech.glide.Glide
import com.unicundi.unimarket.R
import com.unicundi.unimarket.data.network.RetrofitClient
import com.unicundi.unimarket.databinding.ActivityMainBinding
import com.unicundi.unimarket.util.NotificacionHelper
import com.unicundi.unimarket.worker.MensajeCheckWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restaurar sesión
        if (!com.unicundi.unimarket.data.model.Sesion.estaLogueado) {
            com.unicundi.unimarket.data.model.Sesion.cargarDesdePrefs(
                getSharedPreferences("sesion", MODE_PRIVATE)
            )
            if (!com.unicundi.unimarket.data.model.Sesion.estaLogueado) {
                startActivity(
                    android.content.Intent(this, LoginActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
                return
            }
        }

        // Notificaciones
        NotificacionHelper.crearCanal(this)
        pedirPermisoNotificaciones()
        registrarWorkManagerPolling()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                    true
                }
                R.id.mensajesFragment -> {
                    limpiarBadgeMensajes()
                    navController.navigate(R.id.mensajesFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, false)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                    true
                }
                else -> false
            }
        }

        binding.fabPublicar.setOnClickListener {
            navController.navigate(R.id.publicarFragment)
        }

        actualizarDrawerHeader()
        sincronizarFotoPerfil()

        fun cerrarDrawerYNavegar(destino: Int) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigate(destino)
        }

        binding.root.findViewById<LinearLayout>(R.id.drawerItemPerfil)
            .setOnClickListener { cerrarDrawerYNavegar(R.id.perfilFragment) }
        binding.root.findViewById<LinearLayout>(R.id.drawerItemPublicaciones)
            .setOnClickListener { cerrarDrawerYNavegar(R.id.misPublicacionesFragment) }
        binding.root.findViewById<LinearLayout>(R.id.drawerItemFavoritos)
            .setOnClickListener { cerrarDrawerYNavegar(R.id.favoritosFragment) }
        binding.root.findViewById<LinearLayout>(R.id.drawerItemAyuda)
            .setOnClickListener { cerrarDrawerYNavegar(R.id.ayudaFragment) }

        val panelCreditos = binding.root.findViewById<LinearLayout>(R.id.panelCreditos)
        val imgArrow = binding.root.findViewById<ImageView>(R.id.imgCreditosArrow)
        var creditosExpandido = false

        binding.root.findViewById<LinearLayout>(R.id.layoutCreditos).setOnClickListener {
            creditosExpandido = !creditosExpandido
            panelCreditos.visibility = if (creditosExpandido) View.VISIBLE else View.GONE
            imgArrow.animate().rotation(if (creditosExpandido) 180f else 0f).setDuration(200).start()
        }

        binding.root.findViewById<LinearLayout>(R.id.layoutCerrarSesion).setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
            com.unicundi.unimarket.data.model.Sesion.limpiar()
            com.unicundi.unimarket.data.model.Sesion.limpiarPrefs(prefs)
            prefs.edit().remove("ultima_msg_id").remove("msgs_no_leidos").apply()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarDrawerHeader()
        iniciarPollingMensajes()
    }

    override fun onPause() {
        super.onPause()
        pollingJob?.cancel()
    }

    // ─── Polling foreground ───────────────────────────────────────

    private fun iniciarPollingMensajes() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                verificarMensajesNuevos()
                delay(30_000L)
            }
        }
    }

    private suspend fun verificarMensajesNuevos() {
        val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
        val myId = com.unicundi.unimarket.data.model.Sesion.usuarioId
        if (myId == 0L) return

        val ultimaId = prefs.getLong("ultima_msg_id", -1L)

        try {
            val r = RetrofitClient.apiService.getMensajes()
            if (!r.isSuccessful) return

            val mensajes = r.body() ?: emptyList()
            val recibidos = mensajes.filter { it.receiverId == myId }

            // Primera vez: solo marcar el máximo actual, sin notificar
            if (ultimaId == -1L) {
                val maxId = recibidos.maxOfOrNull { it.id } ?: 0L
                prefs.edit().putLong("ultima_msg_id", maxId).apply()
                return
            }

            val nuevos = recibidos.filter {
                it.id > ultimaId && !it.message.contains("__xpin_prod__")
            }

            if (nuevos.isNotEmpty()) {
                val maxId = nuevos.maxOf { it.id }
                prefs.edit()
                    .putLong("ultima_msg_id", maxId)
                    .putInt("msgs_no_leidos", prefs.getInt("msgs_no_leidos", 0) + nuevos.size)
                    .apply()

                withContext(Dispatchers.Main) {
                    // Punto en el ícono de mensajes
                    val badge = binding.bottomNavigation.getOrCreateBadge(R.id.mensajesFragment)
                    badge.isVisible = true

                    // Notificación por cada remitente nuevo
                    nuevos.map { it.senderId to it.senderNombre }.distinctBy { it.first }
                        .forEach { (senderId, nombre) ->
                            NotificacionHelper.mostrarNotificacion(this@MainActivity, nombre, senderId)
                        }
                }
            }
        } catch (_: Exception) { /* silencioso */ }
    }

    // ─── Limpiar badge (llamado al abrir la pestaña de mensajes) ─

    fun limpiarBadgeMensajes() {
        binding.bottomNavigation.removeBadge(R.id.mensajesFragment)
        getSharedPreferences("sesion", MODE_PRIVATE)
            .edit().putInt("msgs_no_leidos", 0).apply()
        // Actualizar ultima_msg_id al máximo actual para no re-notificar
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val myId = com.unicundi.unimarket.data.model.Sesion.usuarioId
                if (myId == 0L) return@launch
                val r = RetrofitClient.apiService.getMensajes()
                if (r.isSuccessful) {
                    val maxId = (r.body() ?: emptyList())
                        .filter { it.receiverId == myId }
                        .maxOfOrNull { it.id } ?: return@launch
                    getSharedPreferences("sesion", MODE_PRIVATE)
                        .edit().putLong("ultima_msg_id", maxId).apply()
                }
            } catch (_: Exception) {}
        }
    }

    // ─── WorkManager (polling cuando la app está cerrada) ────────

    private fun registrarWorkManagerPolling() {
        val work = PeriodicWorkRequestBuilder<MensajeCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "mensaje_check",
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }

    // ─── Permiso de notificaciones (Android 13+) ─────────────────

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    // ─── Drawer ──────────────────────────────────────────────────

    private fun actualizarDrawerHeader() {
        val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
        val root = binding.root

        root.findViewById<android.widget.TextView>(R.id.txtDrawerNombre)?.text =
            com.unicundi.unimarket.data.model.Sesion.nombre.ifEmpty { "Estudiante UniCundi" }
        root.findViewById<android.widget.TextView>(R.id.txtDrawerCorreo)?.text =
            com.unicundi.unimarket.data.model.Sesion.correo.ifEmpty { "usuario@ucundinamarca.edu.co" }
        root.findViewById<android.widget.TextView>(R.id.txtDrawerFacultad)?.text =
            prefs.getString("facultad", "").orEmpty().ifEmpty { "Universidad de Cundinamarca" }

        val imgAvatarDrawer = root.findViewById<ImageView>(R.id.imgAvatarDrawer)
        val fotoUrl = prefs.getString("fotoPerfil", null)
        if (!fotoUrl.isNullOrBlank() && imgAvatarDrawer != null) {
            Glide.with(this)
                .load(fotoUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imgAvatarDrawer)
        }
    }

    private fun sincronizarFotoPerfil() {
        val userId = com.unicundi.unimarket.data.model.Sesion.usuarioId
        if (userId == 0L) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val r = RetrofitClient.apiService.getUsuarioById(userId)
                if (r.isSuccessful) {
                    val fotoUrl = r.body()?.fotoPerfil
                    if (!fotoUrl.isNullOrBlank()) {
                        getSharedPreferences("sesion", MODE_PRIVATE)
                            .edit().putString("fotoPerfil", fotoUrl).apply()
                        withContext(Dispatchers.Main) { actualizarDrawerHeader() }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun abrirDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
