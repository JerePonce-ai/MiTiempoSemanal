package com.senati.mitiemposemanal.jponce.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SemanaUtilTest {

    @Test
    fun testInicioDeSemanaLunesMiercolesDomingo() {
        val lunes = LocalDate.of(2026, 10, 12)
        val miercoles = LocalDate.of(2026, 10, 14)
        val domingo = LocalDate.of(2026, 10, 18)

        val inicioEsperado = LocalDate.of(2026, 10, 12)

        assertEquals(inicioEsperado, SemanaUtil.inicioDeSemana(lunes))
        assertEquals(inicioEsperado, SemanaUtil.inicioDeSemana(miercoles))
        assertEquals(inicioEsperado, SemanaUtil.inicioDeSemana(domingo))
    }

    @Test
    fun testLimiteLunesInicioDeSemanaNueva() {
        val lunesNuevo = LocalDate.of(2026, 10, 12)
        val desdeIso = SemanaUtil.desdeISO(lunesNuevo)
        val hastaIso = SemanaUtil.hastaISO(lunesNuevo)

        assertEquals("2026-10-12T00:00:00", desdeIso)
        assertEquals("2026-10-19T00:00:00", hastaIso)

        // Evento que empieza exactamente el lunes a las 00:00:00
        val eventoInicioExacto = "2026-10-12T00:00:00"

        assertTrue(eventoInicioExacto >= desdeIso)
        assertTrue(eventoInicioExacto < hastaIso)
    }

    @Test
    fun testPorcentaje21Horas() {
        val segundos21Horas = 21L * 3600L // 75,600 segundos
        val pct = SemanaUtil.porcentaje(segundos21Horas)
        assertEquals(12.5, pct, 0.001)
    }

    @Test
    fun testFormatearPorcentaje() {
        val pctFormateado = SemanaUtil.formatearPorcentaje(12.5)
        assertEquals("12.5 %", pctFormateado)
    }

    @Test
    fun testFormatearPorcentajeFila() {
        assertEquals("< 0.1 %", SemanaUtil.formatearPorcentajeFila(1L))
        assertEquals("< 0.1 %", SemanaUtil.formatearPorcentajeFila(117L))
        assertEquals("0.0 %", SemanaUtil.formatearPorcentajeFila(0L))
        assertEquals("0.1 %", SemanaUtil.formatearPorcentajeFila(303L))
        assertEquals("12.5 %", SemanaUtil.formatearPorcentajeFila(75600L))
    }

    @Test
    fun testFormatearPorcentajeSinRegistrar() {
        val pctSinRegConRegistrado = SemanaUtil.formatearPorcentajeSinRegistrar(299L, 604501L)
        assertEquals("> 99.9 %", pctSinRegConRegistrado)

        val pctSinRegSinRegistrado = SemanaUtil.formatearPorcentajeSinRegistrar(0L, 604800L)
        assertEquals("100.0 %", pctSinRegSinRegistrado)
    }

    @Test
    fun testFormatearPorcentajeExacto() {
        assertEquals("0.019 %", SemanaUtil.formatearPorcentajeExacto(117L))
    }

    @Test
    fun testFormatearTiempoDistintosValores() {
        assertEquals("13 s", SemanaUtil.formatearTiempo(13L))
        assertEquals("1 min 5 s", SemanaUtil.formatearTiempo(65L))
        assertEquals("1 h", SemanaUtil.formatearTiempo(3600L))
        assertEquals("21 h", SemanaUtil.formatearTiempo(75600L))
        assertEquals("21 h 05 min", SemanaUtil.formatearTiempo(75900L))
    }

    @Test
    fun testSinRegistrarNuncaEsNegativo() {
        val normal = SemanaUtil.sinRegistrarSegundos(75600L)
        assertEquals(529200L, normal)

        val exceso = SemanaUtil.sinRegistrarSegundos(700000L)
        assertEquals(0L, exceso)
    }
}
