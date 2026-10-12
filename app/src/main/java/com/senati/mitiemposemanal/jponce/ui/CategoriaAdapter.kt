package com.senati.mitiemposemanal.jponce.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.databinding.ItemCategoriaBinding
import com.senati.mitiemposemanal.jponce.model.Categoria

class CategoriaAdapter(
    private val taskCountProvider: (Int) -> Int,
    private val onEditClick: (Categoria) -> Unit,
    private val onDeleteClick: (Categoria) -> Unit
) : ListAdapter<Categoria, CategoriaAdapter.CategoriaViewHolder>(DiffCallback) {

    inner class CategoriaViewHolder(private val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(categoria: Categoria) {
            val context = binding.root.context
            binding.tvNombreCategoria.text = categoria.nombre

            val count = taskCountProvider(categoria.id)
            val countText = context.resources.getQuantityString(R.plurals.cantidad_tareas, count, count)
            binding.tvCantidadTareas.text = countText

            val style = CategoryStyleHelper.getStyle(context, categoria.nombre)
            binding.ivCategoryAvatar.setImageResource(style.iconRes)
            binding.ivCategoryAvatar.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
            binding.ivCategoryAvatar.imageTintList = ColorStateList.valueOf(style.iconColor)

            binding.btnEditarCategoria.setOnClickListener { onEditClick(categoria) }
            binding.btnEliminarCategoria.setOnClickListener { onDeleteClick(categoria) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Categoria>() {
        override fun areItemsTheSame(oldItem: Categoria, newItem: Categoria): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Categoria, newItem: Categoria): Boolean {
            return oldItem == newItem
        }
    }
}
