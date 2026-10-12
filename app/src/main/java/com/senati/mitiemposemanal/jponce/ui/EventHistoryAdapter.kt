@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ItemEventoHeaderBinding
import com.senati.mitiemposemanal.jponce.databinding.ItemEventoHistoryBinding
import com.senati.mitiemposemanal.jponce.model.Evento
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class HistoryListItem {
    data class Header(val dateTitle: String) : HistoryListItem()
    data class EventItem(val evento: Evento) : HistoryListItem()
}

class EventHistoryAdapter : ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_EVENT = 1

        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.forLanguageTag("es-ES"))

        fun processEventList(eventos: List<Evento>, context: Context): List<HistoryListItem> {
            if (eventos.isEmpty()) return emptyList()

            val result = mutableListOf<HistoryListItem>()
            val hoy = LocalDate.now()
            val ayer = hoy.minusDays(1)

            val grouped = eventos.groupBy { it.inicio.toLocalDate() }

            for ((date, eventGroup) in grouped) {
                val headerTitle = when (date) {
                    hoy -> context.getString(R.string.header_today)
                    ayer -> context.getString(R.string.header_yesterday)
                    else -> date.format(dateFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }

                result.add(HistoryListItem.Header(headerTitle))
                for (evt in eventGroup) {
                    result.add(HistoryListItem.EventItem(evt))
                }
            }

            return result
        }

        object DiffCallback : DiffUtil.ItemCallback<HistoryListItem>() {
            override fun areItemsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
                return when {
                    oldItem is HistoryListItem.Header && newItem is HistoryListItem.Header ->
                        oldItem.dateTitle == newItem.dateTitle
                    oldItem is HistoryListItem.EventItem && newItem is HistoryListItem.EventItem ->
                        oldItem.evento.id == newItem.evento.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryListItem.Header -> TYPE_HEADER
            is HistoryListItem.EventItem -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemEventoHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemEventoHistoryBinding.inflate(inflater, parent, false)
            EventViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is HistoryListItem.Header) {
            holder.bind(item)
        } else if (holder is EventViewHolder && item is HistoryListItem.EventItem) {
            holder.bind(item.evento)
        }
    }

    class HeaderViewHolder(private val binding: ItemEventoHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: HistoryListItem.Header) {
            binding.tvHeaderDate.text = header.dateTitle
        }
    }

    class EventViewHolder(private val binding: ItemEventoHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(evento: Evento) {
            val context = binding.root.context
            binding.tvEventTaskName.text = evento.tareaNombre
            binding.tvEventCategoryName.text = evento.categoriaNombre

            val inicioStr = evento.inicio.format(timeFormatter)
            val finStr = evento.fin?.format(timeFormatter) ?: "--:--"
            binding.tvEventTimeRange.text = "$inicioStr - $finStr"

            val duracionSeg = evento.duracion?.seconds ?: 0L
            binding.chipDuration.text = Repository.formatearDuracionLegible(duracionSeg)

            val style = CategoryStyleHelper.getStyle(context, evento.categoriaNombre)
            binding.ivEventCategoryAvatar.setImageResource(style.iconRes)
            binding.ivEventCategoryAvatar.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
            binding.ivEventCategoryAvatar.imageTintList = ColorStateList.valueOf(style.iconColor)

            // Franja de color de la categoría en el borde izquierdo
            binding.viewCategoryStripe.setBackgroundColor(style.iconColor)
        }
    }
}
