package com.senati.mitiemposemanal.jponce.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ActivityTareasBinding
import com.senati.mitiemposemanal.jponce.databinding.DialogTareaBinding
import com.senati.mitiemposemanal.jponce.model.Tarea

class TareasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTareasBinding
    private lateinit var repository: Repository
    private lateinit var adapter: TareaAdapter

    private var todasLasTareas: List<Tarea> = emptyList()
    private var categoriaFiltroId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.rvTareas.setPadding(
                binding.rvTareas.paddingLeft,
                binding.rvTareas.paddingTop,
                binding.rvTareas.paddingRight,
                systemBars.bottom + 100
            )
            insets
        }

        binding.toolbarTareas.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        repository = Repository(this)

        setupRecyclerView()

        binding.fabAgregarTarea.setOnClickListener {
            binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            mostrarDialogoAgregar()
        }

        binding.btnEmptyAgregarTarea.setOnClickListener {
            binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            mostrarDialogoAgregar()
        }

        cargarTareas()
    }

    private fun setupRecyclerView() {
        adapter = TareaAdapter(
            onEditClick = { tarea -> mostrarDialogoEditar(tarea) },
            onDeleteClick = { tarea -> confirmarEliminar(tarea) }
        )
        binding.rvTareas.layoutManager = LinearLayoutManager(this)
        binding.rvTareas.adapter = adapter

        binding.rvTareas.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.fabAgregarTarea.isExtended) {
                    binding.fabAgregarTarea.shrink()
                } else if (dy < 0 && !binding.fabAgregarTarea.isExtended) {
                    binding.fabAgregarTarea.extend()
                }
            }
        })
    }

    private fun setupFilterChips() {
        binding.chipGroupCategorias.removeAllViews()
        val categorias = repository.obtenerCategorias()

        // Chip "Todas"
        val chipTodas = Chip(this).apply {
            text = getString(R.string.filter_all_tasks)
            isCheckable = true
            isChecked = (categoriaFiltroId == null)
            setOnClickListener {
                categoriaFiltroId = null
                aplicarFiltro()
            }
        }
        binding.chipGroupCategorias.addView(chipTodas)

        for (cat in categorias) {
            val chip = Chip(this).apply {
                text = cat.nombre
                isCheckable = true
                isChecked = (categoriaFiltroId == cat.id)
                setOnClickListener {
                    categoriaFiltroId = cat.id
                    aplicarFiltro()
                }
            }
            binding.chipGroupCategorias.addView(chip)
        }
    }

    private fun cargarTareas() {
        todasLasTareas = repository.obtenerTareas()
        setupFilterChips()
        aplicarFiltro()
    }

    private fun aplicarFiltro() {
        val listaFiltrada = if (categoriaFiltroId == null) {
            todasLasTareas
        } else {
            todasLasTareas.filter { it.categoriaId == categoriaFiltroId }
        }

        adapter.submitList(listaFiltrada)

        if (listaFiltrada.isEmpty()) {
            binding.rvTareas.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE

            if (categoriaFiltroId != null) {
                binding.tvEmptyTitle.text = getString(R.string.empty_category_tasks_title)
                binding.tvEmptySubtitle.text = getString(R.string.empty_category_tasks_subtitle)
            } else {
                binding.tvEmptyTitle.text = getString(R.string.empty_tasks_title)
                binding.tvEmptySubtitle.text = getString(R.string.empty_tasks_subtitle)
            }
        } else {
            binding.rvTareas.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun mostrarDialogoAgregar() {
        val categorias = repository.obtenerCategorias()
        if (categorias.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.error_create_category_first), Snackbar.LENGTH_LONG).show()
            return
        }

        val bindingDialog = DialogTareaBinding.inflate(layoutInflater)
        val nombresCategorias = categorias.map { it.nombre }
        val adapterCategorias = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresCategorias)
        bindingDialog.actvCategoria.setAdapter(adapterCategorias)

        var categoriaSeleccionadaId: Int? = null
        bindingDialog.actvCategoria.setOnItemClickListener { _, _, position, _ ->
            categoriaSeleccionadaId = categorias[position].id
            bindingDialog.tilCategoria.error = null
        }

        bindingDialog.etNombreTarea.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bindingDialog.tilNombreTarea.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_task_title)
            .setView(bindingDialog.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = bindingDialog.etNombreTarea.text.toString().trim()
            var hayError = false

            if (nombre.isEmpty()) {
                bindingDialog.tilNombreTarea.error = getString(R.string.error_empty_name)
                hayError = true
            }

            if (categoriaSeleccionadaId == null) {
                bindingDialog.tilCategoria.error = getString(R.string.error_no_category_selected)
                hayError = true
            }

            if (hayError) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                return@setOnClickListener
            }

            val resultado = repository.insertarTarea(nombre, categoriaSeleccionadaId!!)
            if (resultado.first) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                bindingDialog.tilNombreTarea.error = null
                bindingDialog.tilCategoria.error = null
                dialog.dismiss()
                Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
                cargarTareas()
            } else {
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                bindingDialog.tilNombreTarea.error = resultado.second
            }
        }
    }

    private fun mostrarDialogoEditar(tarea: Tarea) {
        val categorias = repository.obtenerCategorias()
        if (categorias.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.error_create_category_first), Snackbar.LENGTH_LONG).show()
            return
        }

        val bindingDialog = DialogTareaBinding.inflate(layoutInflater)
        bindingDialog.etNombreTarea.setText(tarea.nombre)

        val nombresCategorias = categorias.map { it.nombre }
        val adapterCategorias = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresCategorias)
        bindingDialog.actvCategoria.setAdapter(adapterCategorias)

        bindingDialog.actvCategoria.setText(tarea.categoriaNombre, false)
        var categoriaSeleccionadaId: Int = tarea.categoriaId

        bindingDialog.actvCategoria.setOnItemClickListener { _, _, position, _ ->
            categoriaSeleccionadaId = categorias[position].id
            bindingDialog.tilCategoria.error = null
        }

        bindingDialog.etNombreTarea.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bindingDialog.tilNombreTarea.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_edit_task_title)
            .setView(bindingDialog.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevoNombre = bindingDialog.etNombreTarea.text.toString().trim()
            if (nuevoNombre.isEmpty()) {
                bindingDialog.tilNombreTarea.error = getString(R.string.error_empty_name)
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                return@setOnClickListener
            }

            val resultado = repository.actualizarTarea(tarea.id, nuevoNombre, categoriaSeleccionadaId)
            if (resultado.first) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                bindingDialog.tilNombreTarea.error = null
                bindingDialog.tilCategoria.error = null
                dialog.dismiss()
                Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
                cargarTareas()
            } else {
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                bindingDialog.tilNombreTarea.error = resultado.second
            }
        }
    }

    private fun confirmarEliminar(tarea: Tarea) {
        val cantidadEventos = repository.obtenerCantidadEventosPorTarea(tarea.id)
        if (cantidadEventos > 0) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cannot_delete_title)
                .setMessage(getString(R.string.dialog_task_has_events_message, cantidadEventos))
                .setPositiveButton(R.string.btn_understood, null)
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_task_title)
                .setMessage(getString(R.string.dialog_confirm_delete_task, tarea.nombre))
                .setPositiveButton(R.string.btn_delete) { _, _ ->
                    val resultado = repository.eliminarTarea(tarea.id)
                    Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
                    if (resultado.first) {
                        binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        cargarTareas()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }
}
