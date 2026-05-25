package com.unicundi.unimarket.ui.adapter

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unicundi.unimarket.data.model.Producto
import com.unicundi.unimarket.databinding.ItemMiProductoBinding
import com.unicundi.unimarket.databinding.ItemFavoritoBinding

// ─── MIS PRODUCTOS ADAPTER ────────────────────────────────────
class MiProductoAdapter(
    private val onEliminar: (Producto) -> Unit,
    private val onVendido: (Producto) -> Unit = {},
    private val onVerDetalle: (Producto) -> Unit = {}
) : ListAdapter<Producto, MiProductoAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(val binding: ItemMiProductoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMiProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = getItem(position)
        with(holder.binding) {
            txtTituloItem.text = p.titulo
            txtPrecioItem.text = "$${String.format("%,.0f", p.precio)}"
            txtFechaItem.text = "Publicado: ${p.fechaPublicacion ?: "Hoy"}"
            txtEstadoItem.text = p.estado

            val ctx = holder.itemView.context
            val esVendido = p.estado.equals("vendido", ignoreCase = true)
            txtEstadoItem.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (p.estado.equals("activo", ignoreCase = true))
                    androidx.core.content.ContextCompat.getColor(ctx, com.unicundi.unimarket.R.color.badge_activo)
                else
                    androidx.core.content.ContextCompat.getColor(ctx, com.unicundi.unimarket.R.color.badge_vendido)
            )
            (root as androidx.cardview.widget.CardView).setCardBackgroundColor(
                if (esVendido) android.graphics.Color.parseColor("#E8F5E9")
                else android.graphics.Color.WHITE
            )
            // Cargar imagen con Glide
            val url = p.imagenes?.firstOrNull()?.url
            if (!url.isNullOrBlank()) {
                Glide.with(imgMiProducto.context)
                    .load(url)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgMiProducto)
            } else {
                imgMiProducto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            btnVendidoProducto.setOnClickListener { onVendido(p) }
            btnEliminarProducto.setOnClickListener { onEliminar(p) }
            root.setOnClickListener { onVerDetalle(p) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(o: Producto, n: Producto) = o.id == n.id
        override fun areContentsTheSame(o: Producto, n: Producto) = o == n
    }
}

// ─── FAVORITO ADAPTER ─────────────────────────────────────────
class FavoritoAdapter(
    private val onQuitarFavorito: (Producto) -> Unit,
    private val onVerDetalle: (Producto) -> Unit
) : ListAdapter<Producto, FavoritoAdapter.ViewHolder>(Diff2()) {

    inner class ViewHolder(val binding: ItemFavoritoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemFavoritoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = getItem(position)
        with(holder.binding) {
            txtTituloFavorito.text = p.titulo
            txtPrecioFavorito.text = "$${String.format("%,.0f", p.precio)}"
            txtEstadoFavorito.text = p.estado
            txtUbicacionFavorito.text = p.usuarioNombre
            val url = p.imagenes?.firstOrNull()?.url
            if (!url.isNullOrBlank()) {
                Glide.with(imgFavorito.context)
                    .load(url)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgFavorito)
            } else {
                imgFavorito.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            btnQuitarFavorito.setOnClickListener { onQuitarFavorito(p) }
            root.setOnClickListener { onVerDetalle(p) }
        }
    }

    class Diff2 : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(o: Producto, n: Producto) = o.id == n.id
        override fun areContentsTheSame(o: Producto, n: Producto) = o == n
    }
}
