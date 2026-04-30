package app.umaia.android.ui.strings

import app.umaia.android.domain.model.OracleOption
import app.umaia.android.domain.model.OracleQuestion
import app.umaia.android.domain.model.StepMilestone

/**
 * Per-language accessors for domain models that carry separate `nameEn` / `nameRu` /
 * `nameKk` (and `desc*`) fields.
 *
 * Fallback: if the requested language is missing, return Russian. If the requested code
 * is "en", return English. This matches the Umaia target audience (Kazakhstan / CIS),
 * where Russian is the lingua franca and English is the explicit minority choice.
 */

private fun pick(code: String, en: String, ru: String, kk: String): String = when (code) {
    "en" -> en
    "kk" -> if (kk.isNotBlank()) kk else ru
    else -> ru // "ru" or any unknown code falls back to Russian
}

fun StepMilestone.localizedName(code: String): String = pick(code, nameEn, nameRu, nameKk)
fun StepMilestone.localizedDesc(code: String): String = pick(code, descEn, descRu, descKk)

// Oracle questionnaire — only EN/RU exist in the data model. KK falls back to RU.
fun OracleQuestion.localizedText(code: String): String =
    if (code == "en") text else textRu

fun OracleQuestion.localizedOracleText(code: String): String =
    if (code == "en") oracleText else oracleTextRu

fun OracleOption.localizedLabel(code: String): String =
    if (code == "en") label else labelRu
