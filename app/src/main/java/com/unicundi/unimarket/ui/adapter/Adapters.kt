package com.unicundi.unimarket.ui.adapter

import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unicundi.unimarket.R
import com.unicundi.unimarket.data.model.*
import com.unicundi.unimarket.databinding.*

// ─── PRODUCTO ADAPTER (grid 2 columnas) ───────────────────────
class ProductoAdapter(
    private val onClick: (Producto) -> Unit
) : ListAdapter<Producto, ProductoAdapter.ViewHolder>(ProductoDiff()) {

    inner class ViewHolder(val binding: ItemProductoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = getItem(position)
        with(holder.binding) {
            txtTituloProducto.text = p.titulo
            txtPrecioProducto.text = "$${String.format("%,.0f", p.precio)}"
            txtUbicacionProducto.text = p.usuarioNombre
            root.setOnClickListener { onClick(p) }

            // Cargar imagen con Glide
            val urlImagen = p.imagenes?.firstOrNull()?.url
            if (!urlImagen.isNullOrBlank()) {
                Glide.with(imgProducto.context)
                    .load(urlImagen)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgProducto)
            } else {
                imgProducto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    class ProductoDiff : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(o: Producto, n: Producto) = o.id == n.id
        override fun areContentsTheSame(o: Producto, n: Producto) = o == n
    }
}

// ─── CATEGORÍA ADAPTER (horizontal chips) ─────────────────────
class CategoriaAdapter(
    private val categorias: List<String>,
    private var seleccionada: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCategoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = categorias.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cat = categorias[position]
        holder.binding.txtCategoria.text = cat
        val isSelected = cat == seleccionada
        val ctx = holder.itemView.context
        holder.binding.txtCategoria.apply {
            background = if (isSelected)
                ContextCompat.getDrawable(ctx, R.drawable.bg_categoria_seleccionada)
            else
                ContextCompat.getDrawable(ctx, R.drawable.bg_categoria_normal)
            setTextColor(
                if (isSelected) ContextCompat.getColor(ctx, R.color.blanco)
                else ContextCompat.getColor(ctx, R.color.texto_secundario)
            )
        }
        holder.itemView.setOnClickListener {
            seleccionada = cat
            notifyDataSetChanged()
            onClick(cat)
        }
    }
}

// ─── CONVERSACION ADAPTER ─────────────────────────────────────
class ConversacionAdapter(
    private val onClick: (com.unicundi.unimarket.data.model.ConversacionItem) -> Unit
) : ListAdapter<com.unicundi.unimarket.data.model.ConversacionItem, ConversacionAdapter.ViewHolder>(ConversacionDiff()) {

    inner class ViewHolder(val binding: ItemConversacionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemConversacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val m = item.mensaje
        with(holder.binding) {
            val esEnviado = m.senderId == com.unicundi.unimarket.data.model.Sesion.usuarioId
            txtNombreChat.text = if (esEnviado) m.receiverNombre else m.senderNombre
            txtUltimoMensaje.text = m.message
            txtHoraChat.text = m.dateTime?.take(16) ?: ""
            txtProductoChat.text = ""
            if (m.leido == false) {
                txtNombreChat.setTypeface(null, android.graphics.Typeface.BOLD)
                indicadorNoLeido.visibility = View.VISIBLE
            } else {
                indicadorNoLeido.visibility = View.GONE
            }
            // Cargar foto del interlocutor
            if (!item.interlocutorFoto.isNullOrBlank()) {
                Glide.with(root.context)
                    .load(item.interlocutorFoto)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgAvatarConversacion)
            } else {
                imgAvatarConversacion.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    class ConversacionDiff : DiffUtil.ItemCallback<com.unicundi.unimarket.data.model.ConversacionItem>() {
        override fun areItemsTheSame(o: com.unicundi.unimarket.data.model.ConversacionItem, n: com.unicundi.unimarket.data.model.ConversacionItem) = o.mensaje.id == n.mensaje.id
        override fun areContentsTheSame(o: com.unicundi.unimarket.data.model.ConversacionItem, n: com.unicundi.unimarket.data.model.ConversacionItem) = o == n
    }
}

// ─── MIS PUBLICACIONES ADAPTER (horizontal en perfil) ─────────
class MisPublicacionesAdapter(
    private val onVerDetalle: (Producto) -> Unit = {}
) : ListAdapter<Producto, MisPublicacionesAdapter.ViewHolder>(ProductoDiff2()) {

    inner class ViewHolder(val binding: ItemMiPublicacionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMiPublicacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = getItem(position)
        with(holder.binding) {
            txtEstadoPublicacion.text = p.estado
            val ctx = holder.itemView.context
            txtEstadoPublicacion.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    if (p.estado.equals("activo", ignoreCase = true))
                        ContextCompat.getColor(ctx, R.color.badge_activo)
                    else
                        ContextCompat.getColor(ctx, R.color.badge_vendido)
                )
            // Cargar imagen con Glide
            val url = p.imagenes?.firstOrNull()?.url
            if (!url.isNullOrBlank()) {
                Glide.with(imgMiPublicacion.context)
                    .load(url)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgMiPublicacion)
            } else {
                imgMiPublicacion.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            root.setOnClickListener { onVerDetalle(p) }
        }
    }

    class ProductoDiff2 : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(o: Producto, n: Producto) = o.id == n.id
        override fun areContentsTheSame(o: Producto, n: Producto) = o == n
    }
}
