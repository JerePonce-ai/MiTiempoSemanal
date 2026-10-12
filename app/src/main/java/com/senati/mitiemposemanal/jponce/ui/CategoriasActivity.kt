package com.senati.mitiemposemanal.jponce.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ActivityCategoriasBinding
import com.senati.mitiemposemanal.jponce.databinding.DialogCategoriaBinding
import com.senati.mitiemposemanal.jponce.model.Categoria

class CategoriasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriasBinding
    private lateinit var repository: Repository
    private lateinit var adapter: CategoriaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.rvCategorias.setPadding(
                binding.rvCategorias.paddingLeft,
                binding.rvCategorias.paddingTop,
                binding.rvCategorias.paddingRight,
                systemBars.bottom + 100
            )
            insets
        }

        setSupportActionBar(binding.toolbarCategorias)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarCategorias.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        repository = Repository(this)

        setupRecyclerView()

        binding.fabAgregarCategoria.setOnClickListener {
            mostrarDialogoAgregar()
        }

        binding.btnEmptyAgregarCategoria.setOnClickListener {
            mostrarDialogoAgregar()
        }

        cargarCategorias()
    }

    private fun setupRecyclerView() {
        adapter = CategoriaAdapter(
            taskCountProvider = { catId -> repository.obtenerCantidadTareasPorCategoria(catId) },
            onEditClick = { categoria -> mostrarDialogoEditar(categoria) },
            onDeleteClick = { categoria -> confirmarEliminar(categoria) }
        )
        binding.rvCategorias.layoutManager = LinearLayoutManager(this)
        binding.rvCategorias.adapter = adapter

        binding.rvCategorias.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.fabAgregarCategoria.isExtended) {
                    binding.fabAgregarCategoria.shrink()
                } else if (dy < 0 && !binding.fabAgregarCategoria.isExtended) {
                    binding.fabAgregarCategoria.extend()
                }
            }
        })
    }

    private fun cargarCategorias() {
        val lista = repository.obtenerCategorias()
        adapter.submitList(lista)
        if (lista.isEmpty()) {
            binding.rvCategorias.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvCategorias.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun mostrarDialogoAgregar() {
        val bindingDialog = DialogCategoriaBinding.inflate(layoutInflater)

        bindingDialog.etNombreCategoria.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bindingDialog.tilNombreCategoria.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_category_title)
            .setView(bindingDialog.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = bindingDialog.etNombreCategoria.text.toString().trim()
            if (nombre.isEmpty()) {
                bindingDialog.tilNombreCategoria.error = getString(R.string.error_empty_name)
                return@setOnClickListener
            }

            val resultado = repository.insertarCategoria(nombre)
            if (resultado.first) {
                bindingDialog.tilNombreCategoria.error = null
                dialog.dismiss()
                Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
                cargarCategorias()
            } else {
                bindingDialog.tilNombreCategoria.error = resultado.second
            }
        }
    }

    private fun mostrarDialogoEditar(categoria: Categoria) {
        val bindingDialog = DialogCategoriaBinding.inflate(layoutInflater)
        bindingDialog.etNombreCategoria.setText(categoria.nombre)
        bindingDialog.etNombreCategoria.selectAll()

        bindingDialog.etNombreCategoria.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bindingDialog.tilNombreCategoria.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_edit_category_title)
            .setView(bindingDialog.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevoNombre = bindingDialog.etNombreCategoria.text.toString().trim()
            if (nuevoNombre.isEmpty()) {
                bindingDialog.tilNombreCategoria.error = getString(R.string.error_empty_name)
                return@setOnClickListener
            }

            val resultado = repository.actualizarCategoria(categoria.id, nuevoNombre)
            if (resultado.first) {
                bindingDialog.tilNombreCategoria.error = null
                dialog.dismiss()
                Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
                cargarCategorias()
            } else {
                bindingDialog.tilNombreCategoria.error = resultado.second
            }
        }
    }

    private fun confirmarEliminar(categoria: Categoria) {
        val cantidadTareas = repository.obtenerCantidadTareasPorCategoria(categoria.id)
        if (cantidadTareas > 0) {
            // CORRECCIÓN OBLIGATORIA: Diálogo informativo si la categoría tiene tareas asociadas
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_category_has_tasks_title)
                .setMessage(getString(R.string.dialog_category_has_tasks_message, cantidadTareas))
                .setPositiveButton(R.string.btn_understood, null)
                .show()
        } else {
            // Confirmación normal si no tiene tareas
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_category_title)
                .setMessage(getString(R.string.dialog_confirm_delete_category, categoria.nombre))
                .setPositiveButton(R.string.btn_delete) { _, _ ->
                    val resultado = repository.eliminarCategoria(categoria.id)
                    Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
                    if (resultado.first) {
                        cargarCategorias()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }
}
