package com.senati.mitiemposemanal.jponce.ui

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.senati.mitiemposemanal.jponce.R

data class CategoryStyle(
    @DrawableRes val iconRes: Int,
    @ColorInt val backgroundColor: Int,
    @ColorInt val iconColor: Int
)

object CategoryStyleHelper {

    fun getStyle(context: Context, categoryName: String): CategoryStyle {
        val nameLower = categoryName.trim().lowercase()

        return when {
            nameLower.contains("estudio") || nameLower.contains("estudiar") -> {
                CategoryStyle(
                    iconRes = R.drawable.ic_school,
                    backgroundColor = ContextCompat.getColor(context, R.color.cat_estudio_bg),
                    iconColor = ContextCompat.getColor(context, R.color.cat_estudio_icon)
                )
            }
            nameLower.contains("trabajo") || nameLower.contains("trabajar") -> {
                CategoryStyle(
                    iconRes = R.drawable.ic_work,
                    backgroundColor = ContextCompat.getColor(context, R.color.cat_trabajo_bg),
                    iconColor = ContextCompat.getColor(context, R.color.cat_trabajo_icon)
                )
            }
            nameLower.contains("descanso") || nameLower.contains("dormir") || nameLower.contains("relax") -> {
                CategoryStyle(
                    iconRes = R.drawable.ic_bedtime,
                    backgroundColor = ContextCompat.getColor(context, R.color.cat_descanso_bg),
                    iconColor = ContextCompat.getColor(context, R.color.cat_descanso_icon)
                )
            }
            nameLower.contains("ocio") || nameLower.contains("juego") || nameLower.contains("game") -> {
                CategoryStyle(
                    iconRes = R.drawable.ic_sports_esports,
                    backgroundColor = ContextCompat.getColor(context, R.color.cat_ocio_bg),
                    iconColor = ContextCompat.getColor(context, R.color.cat_ocio_icon)
                )
            }
            else -> {
                val bgColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.cat_generic_1_bg),
                    ContextCompat.getColor(context, R.color.cat_generic_2_bg),
                    ContextCompat.getColor(context, R.color.cat_generic_3_bg),
                    ContextCompat.getColor(context, R.color.cat_generic_4_bg)
                )
                val iconColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.cat_generic_1_icon),
                    ContextCompat.getColor(context, R.color.cat_generic_2_icon),
                    ContextCompat.getColor(context, R.color.cat_generic_3_icon),
                    ContextCompat.getColor(context, R.color.cat_generic_4_icon)
                )
                val index = Math.abs(categoryName.hashCode()) % bgColors.size
                CategoryStyle(
                    iconRes = R.drawable.ic_label,
                    backgroundColor = bgColors[index],
                    iconColor = iconColors[index]
                )
            }
        }
    }
}
