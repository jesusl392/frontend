package com.unicundi.unimarket.ui.adapter

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unicundi.unimarket.databinding.ItemMensajeEnviadoBinding
import com.unicundi.unimarket.databinding.ItemMensajeRecibidoBinding

data class MensajeChat(
    val texto: String,
    val hora: String,
    val esPropio: Boolean  // true = enviado por mí, false = recibido
)

class ChatAdapter : ListAdapter<MensajeChat, RecyclerView.ViewHolder>(MensajeDiff()) {

    companion object {
        private const val TIPO_ENVIADO = 1
        private const val TIPO_RECIBIDO = 2
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position).esPropio) TIPO_ENVIADO else TIPO_RECIBIDO

    // ViewHolder para mensajes enviados (burbuja derecha verde)
    inner class EnviadoViewHolder(val binding: ItemMensajeEnviadoBinding) :
        RecyclerView.ViewHolder(binding.root)

    // ViewHolder para mensajes recibidos (burbuja izquierda blanca)
    inner class RecibidoViewHolder(val binding: ItemMensajeRecibidoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENVIADO) {
            EnviadoViewHolder(
                ItemMensajeEnviadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            RecibidoViewHolder(
                ItemMensajeRecibidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = getItem(position)
        when (holder) {
            is EnviadoViewHolder -> {
                holder.binding.txtMensajeEnviado.text = mensaje.texto
                holder.binding.txtHoraEnviado.text = mensaje.hora
            }
            is RecibidoViewHolder -> {
                holder.binding.txtMensajeRecibido.text = mensaje.texto
                holder.binding.txtHoraRecibido.text = mensaje.hora
            }
        }
    }

    class MensajeDiff : DiffUtil.ItemCallback<MensajeChat>() {
        override fun areItemsTheSame(o: MensajeChat, n: MensajeChat) =
            o.texto == n.texto && o.hora == n.hora
        override fun areContentsTheSame(o: MensajeChat, n: MensajeChat) = o == n
    }
}
