package com.senati.mitiemposemanal.jponce.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.databinding.ItemEventSearchBinding
import com.senati.mitiemposemanal.jponce.model.Tarea

sealed class SearchResultItem {
    data class ExistingTask(val tarea: Tarea) : SearchResultItem()
    data class CreateNewTask(val queryText: String) : SearchResultItem()
}

class EventSearchAdapter(
    private val onItemClick: (SearchResultItem) -> Unit
) : RecyclerView.Adapter<EventSearchAdapter.SearchViewHolder>() {

    private var items: List<SearchResultItem> = emptyList()

    fun submitList(newList: List<SearchResultItem>) {
        items = newList
        notifyDataSetChanged()
    }

    inner class SearchViewHolder(private val binding: ItemEventSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResultItem) {
            val context = binding.root.context
            when (item) {
                is SearchResultItem.ExistingTask -> {
                    val tarea = item.tarea
                    binding.tvSearchText.text = "${tarea.nombre} · ${tarea.categoriaNombre}"

                    val style = CategoryStyleHelper.getStyle(context, tarea.categoriaNombre)
                    binding.ivSearchCategoryAvatar.setImageResource(style.iconRes)
                    binding.ivSearchCategoryAvatar.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
                    binding.ivSearchCategoryAvatar.imageTintList = ColorStateList.valueOf(style.iconColor)

                    binding.root.setOnClickListener { onItemClick(item) }
                }
                is SearchResultItem.CreateNewTask -> {
                    val text = context.getString(R.string.search_create_new_task_item, item.queryText)
                    binding.tvSearchText.text = text

                    binding.ivSearchCategoryAvatar.setImageResource(R.drawable.ic_add)
                    val primaryColor = ContextCompat.getColor(context, R.color.md_theme_light_primary)
                    val primaryContainer = ContextCompat.getColor(context, R.color.md_theme_light_primaryContainer)
                    binding.ivSearchCategoryAvatar.backgroundTintList = ColorStateList.valueOf(primaryContainer)
                    binding.ivSearchCategoryAvatar.imageTintList = ColorStateList.valueOf(primaryColor)

                    binding.root.setOnClickListener { onItemClick(item) }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemEventSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
