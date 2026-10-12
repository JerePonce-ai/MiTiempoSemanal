package com.senati.mitiemposemanal.jponce.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.jponce.databinding.ItemTareaBinding
import com.senati.mitiemposemanal.jponce.model.Tarea

class TareaAdapter(
    private val onEditClick: (Tarea) -> Unit,
    private val onDeleteClick: (Tarea) -> Unit
) : ListAdapter<Tarea, TareaAdapter.TareaViewHolder>(DiffCallback) {

    inner class TareaViewHolder(private val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(tarea: Tarea) {
            val context = binding.root.context
            // Formato exacto requerido: "${tarea.nombre} · ${tarea.categoriaNombre}"
            binding.tvNombreTarea.text = "${tarea.nombre} · ${tarea.categoriaNombre}"

            val style = CategoryStyleHelper.getStyle(context, tarea.categoriaNombre)
            binding.ivTaskAvatar.setImageResource(style.iconRes)
            binding.ivTaskAvatar.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
            binding.ivTaskAvatar.imageTintList = ColorStateList.valueOf(style.iconColor)

            binding.btnEditarTarea.setOnClickListener { onEditClick(tarea) }
            binding.btnEliminarTarea.setOnClickListener { onDeleteClick(tarea) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea): Boolean {
            return oldItem == newItem
        }
    }
}
