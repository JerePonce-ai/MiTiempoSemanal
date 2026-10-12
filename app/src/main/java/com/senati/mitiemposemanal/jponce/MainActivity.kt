@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ActivityMainBinding
import com.senati.mitiemposemanal.jponce.model.Evento
import com.senati.mitiemposemanal.jponce.ui.CategoriasActivity
import com.senati.mitiemposemanal.jponce.ui.CategoryStyleHelper
import com.senati.mitiemposemanal.jponce.ui.EventosActivity
import com.senati.mitiemposemanal.jponce.ui.TareasActivity
import java.time.Duration
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository

    private var activeEvent: Evento? = null
    private val bannerHandler = Handler(Looper.getMainLooper())

    private val bannerRunnable = object : Runnable {
        override fun run() {
            activeEvent?.let { evento ->
                val ahora = LocalDateTime.now()
                val duracion = Duration.between(evento.inicio, ahora)
                val totalSegundos = maxOf(0L, duracion.seconds)

                val horas = totalSegundos / 3600
                val minutos = (totalSegundos % 3600) / 60
                val segundos = totalSegundos % 60

                val timeStr = String.format("%02d:%02d:%02d", horas, minutos, segundos)
                binding.tvBannerText.text = getString(R.string.main_banner_in_progress, evento.tareaNombre, timeStr)

                bannerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.main.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Subtítulo exacto requerido: "[NOMBRE] · v0.2.0 (2)"
        binding.tvSubtitle.text = getString(
            R.string.main_subtitle_format,
            getString(R.string.user_name),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        binding.btnCategorias.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }

        binding.btnTareas.setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }

        binding.btnEventos.setOnClickListener {
            startActivity(Intent(this, EventosActivity::class.java))
        }

        binding.cardActiveBanner.setOnClickListener {
            startActivity(Intent(this, EventosActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarSaludo()
        actualizarContadores()
        actualizarBannerEnCurso()
        actualizarTarjeTuSemana()
    }

    override fun onPause() {
        super.onPause()
        bannerHandler.removeCallbacks(bannerRunnable)
    }

    private fun actualizarSaludo() {
        val hora = LocalDateTime.now().hour
        val saludo = when (hora) {
            in 5..11 -> getString(R.string.greeting_morning)
            in 12..18 -> getString(R.string.greeting_afternoon)
            else -> getString(R.string.greeting_evening)
        }
        binding.tvGreeting.text = "¡$saludo!"
    }

    private fun actualizarContadores() {
        val catCount = repository.obtenerCantidadCategorias()
        binding.tvCountCategorias.text = resources.getQuantityString(
            R.plurals.cantidad_categorias,
            catCount,
            catCount
        )

        val taskCount = repository.obtenerCantidadTareas()
        binding.tvCountTareas.text = resources.getQuantityString(
            R.plurals.cantidad_tareas,
            taskCount,
            taskCount
        )

        val eventCount = repository.obtenerCantidadEventos()
        binding.tvCountEventos.text = resources.getQuantityString(
            R.plurals.cantidad_eventos,
            eventCount,
            eventCount
        )
    }

    private fun actualizarBannerEnCurso() {
        bannerHandler.removeCallbacks(bannerRunnable)
        activeEvent = repository.obtenerEventoEnCurso()

        if (activeEvent != null) {
            binding.cardActiveBanner.visibility = View.VISIBLE
            bannerHandler.post(bannerRunnable)
        } else {
            binding.cardActiveBanner.visibility = View.GONE
        }
    }

    private fun actualizarTarjeTuSemana() {
        val stats = repository.obtenerEstadisticasSemanales()
        if (stats.isEmpty()) {
            binding.tvWeekEmptyState.visibility = View.VISIBLE
            binding.cardSegmentedBarContainer.visibility = View.GONE
            binding.layoutWeekLegend.visibility = View.GONE
            binding.tvWeekTotalTime.visibility = View.GONE
        } else {
            binding.tvWeekEmptyState.visibility = View.GONE
            binding.cardSegmentedBarContainer.visibility = View.VISIBLE
            binding.layoutWeekLegend.visibility = View.VISIBLE
            binding.tvWeekTotalTime.visibility = View.VISIBLE

            var totalSegundosSemana = 0L
            stats.forEach { totalSegundosSemana += it.duracionTotal.seconds }

            val totalStr = Repository.formatearDuracionLegible(totalSegundosSemana)
            binding.tvWeekTotalTime.text = getString(R.string.week_summary_total, totalStr)

            // Renderizar barra segmentada
            binding.layoutSegmentedBar.removeAllViews()
            binding.layoutWeekLegend.removeAllViews()

            val colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            val colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLUE)

            for (stat in stats) {
                val style = CategoryStyleHelper.getStyle(this, stat.categoriaNombre)

                // Segmento en la barra
                val segment = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        stat.porcentaje
                    )
                    setBackgroundColor(style.iconColor)
                }
                binding.layoutSegmentedBar.addView(segment)

                // Ítem en la leyenda
                val legendRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)

                    val dot = View(context).apply {
                        val size = (10 * resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(size, size).apply {
                            setMargins(0, (4 * resources.displayMetrics.density).toInt(), (8 * resources.displayMetrics.density).toInt(), 0)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(style.iconColor)
                        }
                    }

                    val label = TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        text = "${stat.categoriaNombre} (${stat.porcentaje.toInt()}%)"
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                        setTextColor(colorOnSurface)
                    }

                    val time = TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        text = Repository.formatearDuracionLegible(stat.duracionTotal.seconds)
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                        setTextColor(colorPrimary)
                    }

                    addView(dot)
                    addView(label)
                    addView(time)
                }

                binding.layoutWeekLegend.addView(legendRow)
            }
        }
    }
}
