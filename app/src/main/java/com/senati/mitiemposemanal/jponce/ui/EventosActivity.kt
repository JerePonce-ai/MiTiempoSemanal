@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ActivityEventosBinding
import com.senati.mitiemposemanal.jponce.databinding.DialogNuevaTareaEventoBinding
import com.senati.mitiemposemanal.jponce.model.Evento
import com.senati.mitiemposemanal.jponce.model.Tarea
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventosBinding
    private lateinit var repository: Repository
    private lateinit var searchAdapter: EventSearchAdapter
    private lateinit var historyAdapter: EventHistoryAdapter

    private var selectedTarea: Tarea? = null
    private var eventoEnCurso: Evento? = null

    private val stopwatchHandler = Handler(Looper.getMainLooper())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val stopwatchRunnable = object : Runnable {
        override fun run() {
            eventoEnCurso?.let { evento ->
                val ahora = LocalDateTime.now()
                val duracion = Duration.between(evento.inicio, ahora)
                val totalSegundos = maxOf(0L, duracion.seconds)

                val horas = totalSegundos / 3600
                val minutos = (totalSegundos % 3600) / 60
                val segundos = totalSegundos % 60

                binding.tvLiveChronometer.text = String.format("%02d:%02d:%02d", horas, minutos, segundos)
                stopwatchHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.nestedScrollView.setPadding(
                0, 0, 0, systemBars.bottom
            )
            insets
        }

        binding.toolbarEventos.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        repository = Repository(this)

        setupAdapters()
        setupSearchInput()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoEvento()
        cargarHistorial()
    }

    override fun onPause() {
        super.onPause()
        stopwatchHandler.removeCallbacks(stopwatchRunnable)
    }

    private fun setupAdapters() {
        searchAdapter = EventSearchAdapter { item ->
            when (item) {
                is SearchResultItem.ExistingTask -> {
                    selectedTarea = item.tarea
                    binding.etBuscarTarea.setText(item.tarea.nombre)
                    binding.etBuscarTarea.setSelection(item.tarea.nombre.length)
                    binding.rvTaskSuggestions.visibility = View.GONE
                    mostrarChipCategoria(item.tarea.categoriaNombre)
                }
                is SearchResultItem.CreateNewTask -> {
                    binding.rvTaskSuggestions.visibility = View.GONE
                    mostrarDialogoCrearTareaYEvento(item.queryText)
                }
            }
        }

        binding.rvTaskSuggestions.layoutManager = LinearLayoutManager(this)
        binding.rvTaskSuggestions.adapter = searchAdapter

        historyAdapter = EventHistoryAdapter()
        binding.rvHistorialEventos.layoutManager = LinearLayoutManager(this)
        binding.rvHistorialEventos.adapter = historyAdapter
    }

    private fun setupSearchInput() {
        binding.etBuscarTarea.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilBuscarTarea.error = null
                val query = s?.toString()?.trim() ?: ""

                // Si selectedTarea ya no coincide exactamente con query, deseleccionar
                if (selectedTarea != null && !selectedTarea!!.nombre.equals(query, ignoreCase = true)) {
                    selectedTarea = null
                    binding.layoutSelectedCategory.visibility = View.GONE
                }

                if (query.length < 2) {
                    binding.rvTaskSuggestions.visibility = View.GONE
                    binding.layoutSelectedCategory.visibility = View.GONE
                    selectedTarea = null
                    return
                }

                val resultados = repository.buscarTareas(query)
                val listItems = mutableListOf<SearchResultItem>()

                resultados.forEach { listItems.add(SearchResultItem.ExistingTask(it)) }

                val existeCoincidenciaExacta = resultados.any { it.nombre.equals(query, ignoreCase = true) }
                if (!existeCoincidenciaExacta) {
                    listItems.add(SearchResultItem.CreateNewTask(query))
                    selectedTarea = null
                    binding.layoutSelectedCategory.visibility = View.GONE
                } else {
                    val tareaCoincidente = resultados.first { it.nombre.equals(query, ignoreCase = true) }
                    selectedTarea = tareaCoincidente
                    mostrarChipCategoria(tareaCoincidente.categoriaNombre)
                }

                searchAdapter.submitList(listItems)
                binding.rvTaskSuggestions.visibility = View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun mostrarChipCategoria(categoriaNombre: String) {
        val style = CategoryStyleHelper.getStyle(this, categoriaNombre)
        binding.ivSelectedCategoryAvatar.setImageResource(style.iconRes)
        binding.ivSelectedCategoryAvatar.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
        binding.ivSelectedCategoryAvatar.imageTintList = ColorStateList.valueOf(style.iconColor)
        binding.tvSelectedCategoryName.text = categoriaNombre
        binding.layoutSelectedCategory.visibility = View.VISIBLE
    }

    private fun setupButtons() {
        binding.btnIniciarEvento.setOnClickListener {
            binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val nombreInput = binding.etBuscarTarea.text.toString().trim()
            if (nombreInput.isEmpty()) {
                binding.tilBuscarTarea.error = getString(R.string.error_empty_task_input)
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                return@setOnClickListener
            }

            val tareaValida = if (selectedTarea != null && selectedTarea!!.nombre.equals(nombreInput, ignoreCase = true)) {
                selectedTarea
            } else {
                repository.obtenerTareaPorNombre(nombreInput)
            }

            if (tareaValida != null) {
                iniciarEventoExistente(tareaValida.id)
            } else {
                mostrarDialogoCrearTareaYEvento(nombreInput)
            }
        }

        binding.btnFinalizarEventoActivo.setOnClickListener {
            finalizarEventoActivo()
        }
    }

    private fun iniciarEventoExistente(tareaId: Int) {
        val resultado = repository.iniciarEvento(tareaId)
        if (resultado.first) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            binding.etBuscarTarea.text?.clear()
            binding.rvTaskSuggestions.visibility = View.GONE
            binding.layoutSelectedCategory.visibility = View.GONE
            selectedTarea = null
            actualizarEstadoEvento()
            cargarHistorial()
            Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
        } else {
            binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
            Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoCrearTareaYEvento(nombreTarea: String) {
        val categorias = repository.obtenerCategorias()
        if (categorias.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.error_create_category_first), Snackbar.LENGTH_LONG).show()
            return
        }

        val bindingDialog = DialogNuevaTareaEventoBinding.inflate(layoutInflater)
        bindingDialog.tvSubtitleNewTask.text = getString(R.string.dialog_new_task_for_event_subtitle, nombreTarea)

        val nombresCategorias = categorias.map { it.nombre }
        val adapterCategorias = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresCategorias)
        bindingDialog.actvCategoriaEvento.setAdapter(adapterCategorias)

        var categoriaSeleccionadaId: Int? = null
        bindingDialog.actvCategoriaEvento.setOnItemClickListener { _, _, position, _ ->
            categoriaSeleccionadaId = categorias[position].id
            bindingDialog.tilCategoriaEvento.error = null
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(bindingDialog.root)
            .setPositiveButton(R.string.btn_confirm, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (categoriaSeleccionadaId == null) {
                bindingDialog.tilCategoriaEvento.error = getString(R.string.error_no_category_selected)
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                return@setOnClickListener
            }

            val resultado = repository.iniciarEventoConTareaNueva(nombreTarea, categoriaSeleccionadaId!!)
            if (resultado.first) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                dialog.dismiss()
                binding.etBuscarTarea.text?.clear()
                binding.rvTaskSuggestions.visibility = View.GONE
                binding.layoutSelectedCategory.visibility = View.GONE
                selectedTarea = null
                actualizarEstadoEvento()
                cargarHistorial()
                Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
            } else {
                binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
                bindingDialog.tilCategoriaEvento.error = resultado.second
            }
        }
    }

    private fun finalizarEventoActivo() {
        val resultado = repository.finalizarEvento()
        if (resultado.first) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            actualizarEstadoEvento()
            cargarHistorial()
            Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_LONG).show()
        } else {
            binding.root.performHapticFeedback(HapticFeedbackConstants.REJECT)
            Snackbar.make(binding.root, resultado.second, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun actualizarEstadoEvento() {
        stopwatchHandler.removeCallbacks(stopwatchRunnable)
        eventoEnCurso = repository.obtenerEventoEnCurso()

        val activo = eventoEnCurso
        if (activo != null) {
            binding.cardNuevoEvento.visibility = View.GONE
            binding.cardEventoEnCurso.visibility = View.VISIBLE

            binding.tvActiveTaskName.text = activo.tareaNombre
            binding.tvActiveCategoryName.text = activo.categoriaNombre

            val style = CategoryStyleHelper.getStyle(this, activo.categoriaNombre)
            binding.ivActiveCategoryAvatar.setImageResource(style.iconRes)
            binding.ivActiveCategoryAvatar.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
            binding.ivActiveCategoryAvatar.imageTintList = ColorStateList.valueOf(style.iconColor)

            val inicioFormateado = activo.inicio.format(timeFormatter)
            binding.tvActiveStartTime.text = getString(R.string.event_started_at, inicioFormateado)

            binding.btnIniciarEventoActivo.isEnabled = false
            binding.btnFinalizarEventoActivo.isEnabled = true

            // Arrancar el cronómetro en vivo desde la hora guardada en BD
            stopwatchHandler.post(stopwatchRunnable)
        } else {
            binding.cardEventoEnCurso.visibility = View.GONE
            binding.cardNuevoEvento.visibility = View.VISIBLE

            binding.etBuscarTarea.isEnabled = true
            binding.tilBuscarTarea.isEnabled = true
            binding.btnIniciarEvento.isEnabled = true
            binding.btnFinalizarEvento.isEnabled = false
        }
    }

    private fun cargarHistorial() {
        val eventos = repository.obtenerEventos()
        val listItems = EventHistoryAdapter.processEventList(eventos, this)
        historyAdapter.submitList(listItems)

        if (listItems.isEmpty()) {
            binding.rvHistorialEventos.visibility = View.GONE
            binding.layoutEmptyEvents.visibility = View.VISIBLE
        } else {
            binding.rvHistorialEventos.visibility = View.VISIBLE
            binding.layoutEmptyEvents.visibility = View.GONE
        }
    }
}
