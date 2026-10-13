@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object SemanaUtil {

    const val SEGUNDOS_POR_SEMANA = 604_800L // 168 horas = 604,800 segundos

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val rangoDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)

    fun inicioDeSemana(fecha: LocalDate): LocalDate {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun desdeISO(lunes: LocalDate): String {
        return lunes.atStartOfDay().format(isoFormatter)
    }

    fun hastaISO(lunes: LocalDate): String {
        return lunes.plusDays(7).atStartOfDay().format(isoFormatter)
    }

    fun etiquetaDeRango(lunes: LocalDate): String {
        val domingo = lunes.plusDays(6)
        val inicioStr = lunes.format(rangoDateFormatter)
        val finStr = domingo.format(rangoDateFormatter)
        return "Semana $inicioStr – $finStr"
    }

    fun porcentaje(segundos: Long): Double {
        if (segundos <= 0L) return 0.0
        return (segundos.toDouble() / SEGUNDOS_POR_SEMANA.toDouble()) * 100.0
    }

    fun formatearPorcentaje(porcentaje: Double): String {
        val pctVal = maxOf(0.0, porcentaje)
        return String.format(Locale.US, "%.1f %%", pctVal)
    }

    fun formatearPorcentajeFila(segundos: Long): String {
        if (segundos <= 0L) return "0.0 %"
        val pct = porcentaje(segundos)
        val formatted = String.format(Locale.US, "%.1f %%", pct)
        return if (formatted == "0.0 %") "< 0.1 %" else formatted
    }

    fun formatearPorcentajeSinRegistrar(registradoSegundos: Long, sinRegSegundos: Long): String {
        if (registradoSegundos <= 0L) return "100.0 %"
        val pct = porcentaje(sinRegSegundos)
        val formatted = String.format(Locale.US, "%.1f %%", pct)
        return if (formatted == "100.0 %") "> 99.9 %" else formatted
    }

    fun formatearPorcentajeExacto(segundos: Long): String {
        val pct = porcentaje(segundos)
        return String.format(Locale.US, "%.3f %%", maxOf(0.0, pct))
    }

    fun formatearTiempo(segundos: Long): String {
        if (segundos <= 0L) return "0 s"
        if (segundos < 60L) return "$segundos s"

        if (segundos < 3600L) {
            val minutos = segundos / 60L
            val seg = segundos % 60L
            return if (seg > 0L) "$minutos min $seg s" else "$minutos min"
        }

        val horas = segundos / 3600L
        val minutos = (segundos % 3600L) / 60L
        return if (minutos > 0L) {
            String.format(Locale.US, "%d h %02d min", horas, minutos)
        } else {
            "$horas h"
        }
    }

    fun sinRegistrarSegundos(sumaSegundosCategorias: Long): Long {
        return maxOf(0L, SEGUNDOS_POR_SEMANA - sumaSegundosCategorias)
    }
}
