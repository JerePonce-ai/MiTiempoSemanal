package com.senati.mitiemposemanal.jponce

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.senati.mitiemposemanal.jponce.data.Repository
import com.senati.mitiemposemanal.jponce.databinding.ActivityMainBinding
import com.senati.mitiemposemanal.jponce.ui.CategoriasActivity
import com.senati.mitiemposemanal.jponce.ui.TareasActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Configurar subtítulo con el formato exacto requerido: "[NOMBRE] · v0.1.0 (1)"
        binding.tvSubtitle.text = getString(
            R.string.main_subtitle_format,
            "Jeremy Ponce",
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        binding.btnCategorias.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }

        binding.btnTareas.setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarContadores()
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
    }
}
