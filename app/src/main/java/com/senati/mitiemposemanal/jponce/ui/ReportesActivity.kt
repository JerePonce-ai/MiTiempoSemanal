@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.senati.mitiemposemanal.jponce.R
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ActivityReportesBinding
import com.senati.mitiemposemanal.jponce.databinding.ItemResumenSemanaBinding
import com.senati.mitiemposemanal.jponce.model.ReporteItem
import com.senati.mitiemposemanal.jponce.model.SemanaReporte
import com.senati.mitiemposemanal.jponce.util.SemanaUtil

class ReportesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportesBinding
    private lateinit var repository: Repository
    private lateinit var historyAdapter: EventHistoryAdapter

    private var listaSemanas: List<SemanaReporte> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReportesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.nestedScrollView.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        binding.toolbarReportes.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        repository = Repository(this)

        historyAdapter = EventHistoryAdapter()
        binding.rvReporteEventos.layoutManager = LinearLayoutManager(this)
        binding.rvReporteEventos.adapter = historyAdapter
    }

    override fun onResume() {
        super.onResume()
        cargarSemanasYReporte()
    }

    private fun cargarSemanasYReporte() {
        listaSemanas = repository.obtenerSemanasConReporte()

        if (listaSemanas.isEmpty()) {
            binding.cardSpinnerContainer.visibility = View.GONE
            binding.layoutReportContent.visibility = View.GONE
            binding.layoutEmptyReportes.visibility = View.VISIBLE
        } else {
            binding.cardSpinnerContainer.visibility = View.VISIBLE
            binding.layoutReportContent.visibility = View.VISIBLE
            binding.layoutEmptyReportes.visibility = View.GONE

            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listaSemanas
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spinnerSemanas.adapter = spinnerAdapter
            binding.spinnerSemanas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val semanaSeleccionada = listaSemanas[position]
                    cargarDetalleDeSemana(semanaSeleccionada)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Cargar la primera semana por defecto (la más reciente)
            cargarDetalleDeSemana(listaSemanas[0])
        }
    }

    private fun cargarDetalleDeSemana(semana: SemanaReporte) {
        val detalles = repository.obtenerDetalleReporte(semana.semanaInicio)
        binding.containerReporteDetalle.removeAllViews()

        var sumaSegundosCategorias = 0L

        val outlineColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline, Color.GRAY)
        val surfaceVariantColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.LTGRAY)

        for (item in detalles) {
            sumaSegundosCategorias += item.segundos
            val pctStr = SemanaUtil.formatearPorcentajeFila(item.segundos)
            val tiempoStr = SemanaUtil.formatearTiempo(item.segundos)
            val style = CategoryStyleHelper.getStyle(this, item.nombre)

            val rowBinding = ItemResumenSemanaBinding.inflate(layoutInflater, binding.containerReporteDetalle, false)
            rowBinding.tvResumenName.text = item.nombre
            rowBinding.tvResumenTime.text = tiempoStr
            rowBinding.tvResumenPercentage.text = pctStr

            rowBinding.ivResumenIcon.setImageResource(style.iconRes)
            rowBinding.ivResumenIcon.backgroundTintList = ColorStateList.valueOf(style.backgroundColor)
            rowBinding.ivResumenIcon.imageTintList = ColorStateList.valueOf(style.iconColor)

            rowBinding.progressResumen.setIndicatorColor(style.iconColor)

            val pctVal = SemanaUtil.porcentaje(item.segundos)
            val progressVal = if (item.segundos > 0L) {
                maxOf(150, (pctVal * 100.0).toInt().coerceIn(0, 10000))
            } else {
                0
            }
            rowBinding.progressResumen.setProgressCompat(progressVal, false)

            rowBinding.layoutResumenRow.contentDescription = getString(R.string.cd_resumen_row, item.nombre, pctStr, tiempoStr)

            rowBinding.layoutResumenRow.setOnClickListener {
                val pctExacto = SemanaUtil.formatearPorcentajeExacto(item.segundos)
                val mensaje = getString(R.string.snackbar_resumen_exacto, item.nombre, tiempoStr, pctExacto)
                Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
            }

            binding.containerReporteDetalle.addView(rowBinding.root)
        }

        // Fila "Sin registrar"
        val sinRegSegundos = SemanaUtil.sinRegistrarSegundos(sumaSegundosCategorias)
        val pctSinRegStr = SemanaUtil.formatearPorcentajeSinRegistrar(sumaSegundosCategorias, sinRegSegundos)
        val tiempoSinRegStr = SemanaUtil.formatearTiempo(sinRegSegundos)
        val sinRegNombre = getString(R.string.unregistered_label)

        val rowSinRegBinding = ItemResumenSemanaBinding.inflate(layoutInflater, binding.containerReporteDetalle, false)
        rowSinRegBinding.tvResumenName.text = sinRegNombre
        rowSinRegBinding.tvResumenTime.text = tiempoSinRegStr
        rowSinRegBinding.tvResumenPercentage.text = pctSinRegStr

        rowSinRegBinding.ivResumenIcon.setImageResource(R.drawable.ic_timer)
        rowSinRegBinding.ivResumenIcon.backgroundTintList = ColorStateList.valueOf(surfaceVariantColor)
        rowSinRegBinding.ivResumenIcon.imageTintList = ColorStateList.valueOf(outlineColor)

        rowSinRegBinding.progressResumen.setIndicatorColor(outlineColor)

        val pctSinRegVal = SemanaUtil.porcentaje(sinRegSegundos)
        val progressSinRegVal = if (sinRegSegundos > 0L) {
            maxOf(150, (pctSinRegVal * 100.0).toInt().coerceIn(0, 10000))
        } else {
            0
        }
        rowSinRegBinding.progressResumen.setProgressCompat(progressSinRegVal, false)

        rowSinRegBinding.layoutResumenRow.contentDescription = getString(R.string.cd_resumen_row, sinRegNombre, pctSinRegStr, tiempoSinRegStr)

        rowSinRegBinding.layoutResumenRow.setOnClickListener {
            val pctExacto = SemanaUtil.formatearPorcentajeExacto(sinRegSegundos)
            val mensaje = getString(R.string.snackbar_resumen_exacto, sinRegNombre, tiempoSinRegStr, pctExacto)
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
        }

        binding.containerReporteDetalle.addView(rowSinRegBinding.root)

        // Cargar lista de eventos de esa semana
        val eventosSemana = repository.obtenerEventosDeSemana(semana.semanaInicio)
        val listItems = EventHistoryAdapter.processEventList(eventosSemana, this)
        historyAdapter.submitList(listItems)
    }
}
