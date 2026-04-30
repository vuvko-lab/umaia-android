package app.umaia.android.domain.model

import kotlinx.serialization.Serializable

enum class QuestionType { SINGLE, MULTI, NUMBER }

data class OracleOption(
    val id: String,
    val label: String,
    val labelRu: String,
    val role: String
)

data class OracleQuestion(
    val id: String,
    val oracleText: String,
    val oracleTextRu: String,
    val text: String,
    val textRu: String,
    val type: QuestionType,
    val options: List<OracleOption> = emptyList(),
    val dataKey: String,
    val nurReward: Int = 3,
    val conditional: ((HealthData) -> Boolean)? = null
)

@Serializable
data class HealthData(
    val goal: String = "",
    val gender: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val activity: String = "",
    val smoking: String = "",
    val alcohol: String = "",
    val menstrualCycle: String = "",
    val chronicDiseases: String = "",
    val symptoms: List<String> = emptyList(),
    val sleepHours: String = "",
    val sleepQuality: String = "",
    val pressureLevel: String = "",
    val sugarLevel: String = "",
    val eatsVegetables: String = "",
    val eatsJunkFood: String = "",
    val familyHistory: String = ""
) {
    fun set(key: String, value: String): HealthData = when (key) {
        "goal"             -> copy(goal = value)
        "gender"           -> copy(gender = value)
        "age"              -> copy(age = value)
        "height"           -> copy(height = value)
        "weight"           -> copy(weight = value)
        "activity"         -> copy(activity = value)
        "smoking"          -> copy(smoking = value)
        "alcohol"          -> copy(alcohol = value)
        "menstrualCycle"   -> copy(menstrualCycle = value)
        "chronicDiseases"  -> copy(chronicDiseases = value)
        "sleepHours"       -> copy(sleepHours = value)
        "sleepQuality"     -> copy(sleepQuality = value)
        "pressureLevel"    -> copy(pressureLevel = value)
        "sugarLevel"       -> copy(sugarLevel = value)
        "eatsVegetables"   -> copy(eatsVegetables = value)
        "eatsJunkFood"     -> copy(eatsJunkFood = value)
        "familyHistory"    -> copy(familyHistory = value)
        else               -> this
    }

    fun setMulti(key: String, values: List<String>): HealthData =
        if (key == "symptoms") copy(symptoms = values) else this
}

data class RiskFactor(
    val name: String,
    val nameRu: String,
    val impact: Double,
    val source: String,
    val recommendation: String,
    val recommendationRu: String,
    /** Ids into [OracleCitations.all]. Rendered inline below each FactorRow as
     *  clickable links to the underlying study (PMID / PMC / institutional). */
    val citationIds: List<String> = emptyList()
)

data class DeficiencyRisk(
    val nutrient: String,
    val nutrientRu: String,
    val description: String,
    val descriptionRu: String,
    val advice: String,
    val adviceRu: String,
    val citationIds: List<String> = emptyList()
)

data class OracleCitation(val id: String, val title: String, val url: String)

/**
 * Curated set of audited references used in the risk assessment. Each PMID/PMC
 * URL was personally verified against PubMed; the few inaccessible ones were
 * substituted with CDC/WHO equivalents. Match the iOS list in
 * `umaia-ios/Umaia/Domain/Model/OracleData.swift`.
 */
object OracleCitations {
    val all: List<OracleCitation> = listOf(
        OracleCitation("ref_1",  "PSC, Lancet 2009 — body-mass index and life expectancy", "https://pubmed.ncbi.nlm.nih.gov/19299006/"),
        OracleCitation("ref_2",  "Jha et al, NEJM 2013 — smoking and life expectancy", "https://pubmed.ncbi.nlm.nih.gov/23343063/"),
        OracleCitation("ref_3",  "WHO — secondhand tobacco smoke fact sheet", "https://www.who.int/news-room/fact-sheets/detail/tobacco"),
        OracleCitation("ref_4",  "Lee et al, Lancet 2012 — physical inactivity worldwide", "https://pubmed.ncbi.nlm.nih.gov/22818936/"),
        OracleCitation("ref_5",  "Wood et al, Lancet 2018 — alcohol and all-cause mortality", "https://pubmed.ncbi.nlm.nih.gov/29676281/"),
        OracleCitation("ref_6",  "Lewington et al, Lancet 2002 — blood pressure and vascular mortality", "https://pubmed.ncbi.nlm.nih.gov/12493255/"),
        OracleCitation("ref_7",  "Seshasai et al, NEJM 2011 — diabetes and risk of death", "https://pubmed.ncbi.nlm.nih.gov/21366474/"),
        OracleCitation("ref_8",  "CDC — prediabetes is reversible with lifestyle change", "https://www.cdc.gov/diabetes/prevention-type-2/about-prediabetes.html"),
        OracleCitation("ref_9",  "Cappuccio et al, Sleep 2010 — sleep duration and mortality", "https://pubmed.ncbi.nlm.nih.gov/20469800/"),
        OracleCitation("ref_10", "GBD 2017 Diet Collaborators — Lancet 2019", "https://pubmed.ncbi.nlm.nih.gov/30954305/"),
        OracleCitation("ref_11", "PREDIMED trial — NEJM 2018 (primary prevention with Mediterranean diet)", "https://pubmed.ncbi.nlm.nih.gov/29897866/"),
        OracleCitation("ref_12", "AHA — physical activity guidelines", "https://www.heart.org/en/healthy-living/fitness/fitness-basics/aha-recs-for-physical-activity-in-adults"),
        OracleCitation("ref_13", "CDC — high blood pressure facts", "https://www.cdc.gov/high-blood-pressure/about/index.html"),
        OracleCitation("ref_14", "CDC — heart disease and family history", "https://www.cdc.gov/heart-disease-family-history/about/index.html"),
        OracleCitation("ref_15", "CDC — LDL and HDL cholesterol", "https://www.cdc.gov/cholesterol/about/ldl-and-hdl-cholesterol.html"),
        OracleCitation("ref_16", "WHO — healthy diet fact sheet", "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"),
        OracleCitation("ref_17", "NIH — iron-deficiency anemia (RDA, food sources)", "https://ods.od.nih.gov/factsheets/Iron-HealthProfessional/"),
        OracleCitation("ref_18", "NIH — vitamin D fact sheet (sun + food sources)", "https://ods.od.nih.gov/factsheets/VitaminD-HealthProfessional/"),
        OracleCitation("ref_19", "NIH — vitamin B12 fact sheet", "https://ods.od.nih.gov/factsheets/VitaminB12-HealthProfessional/"),
        OracleCitation("ref_20", "NIH — vitamin C fact sheet", "https://ods.od.nih.gov/factsheets/VitaminC-HealthProfessional/"),
        OracleCitation("ref_21", "NIH — omega-3 fatty acids", "https://ods.od.nih.gov/factsheets/Omega3FattyAcids-HealthProfessional/"),
        OracleCitation("ref_22", "NIH — zinc fact sheet", "https://ods.od.nih.gov/factsheets/Zinc-HealthProfessional/"),
    )

    fun byId(id: String): OracleCitation? = all.firstOrNull { it.id == id }
}

data class RiskAssessmentResult(
    val tribalRole: String,
    val nameEn: String,
    val nameRu: String,
    val descEn: String,
    val descRu: String,
    val icon: String,
    val bmi: Double? = null,
    val bmiCategory: String = "unknown",
    val totalImpact: Double = 0.0,
    val riskFactors: List<RiskFactor> = emptyList(),
    val positiveFactors: List<RiskFactor> = emptyList(),
    val deficiencies: List<DeficiencyRisk> = emptyList()
)

fun calculateRiskAssessment(data: HealthData): RiskAssessmentResult {
    val factors = mutableListOf<RiskFactor>()
    val positive = mutableListOf<RiskFactor>()

    // BMI
    val heightM = (data.height.toDoubleOrNull() ?: 0.0) / 100.0
    val weightKg = data.weight.toDoubleOrNull() ?: 0.0
    val bmi: Double? = if (heightM > 0 && weightKg > 0) weightKg / (heightM * heightM) else null
    val bmiCat = when {
        bmi == null -> "unknown"
        bmi < 18.5 -> "underweight"
        bmi < 25.0 -> "normal"
        bmi < 30.0 -> "overweight"
        bmi < 40.0 -> "obese"
        else -> "severely_obese"
    }
    // BMI — values per PSC Lancet 2009 (PMID 19299006).
    if (bmi != null) {
        when {
            bmi >= 40 -> factors.add(RiskFactor("Severe obesity (BMI 40+)", "Тяжёлое ожирение (ИМТ 40+)", -10.0, "PSC Lancet 2009", "Consult a doctor for a weight management plan", "Обратитесь к врачу для плана управления весом", listOf("ref_1")))
            bmi >= 30 -> factors.add(RiskFactor("Obesity (BMI 30-40)", "Ожирение (ИМТ 30-40)", -3.0, "PSC Lancet 2009", "Focus on gradual, sustainable weight loss", "Сосредоточьтесь на постепенном, устойчивом снижении веса", listOf("ref_1")))
            bmi in 18.5..25.0 -> positive.add(RiskFactor("Healthy weight (BMI 18.5–25)", "Здоровый вес (ИМТ 18,5–25)", 1.0, "PSC Lancet 2009", "Maintaining this range supports longevity", "Поддержание этого диапазона способствует долголетию", listOf("ref_1")))
        }
    }

    // Smoking — Jha NEJM 2013 (PMID 23343063); WHO secondhand-smoke fact sheet.
    when (data.smoking) {
        "yes", "current" -> factors.add(RiskFactor("Active smoking", "Активное курение", -10.0, "Jha NEJM 2013", "Quitting smoking is the single best thing you can do for your health", "Бросить курить — лучшее, что вы можете сделать для здоровья", listOf("ref_2")))
        "former" -> factors.add(RiskFactor("Former smoker", "Бывший курильщик", -2.0, "Jha NEJM 2013 (residual)", "Great that you quit! Risk decreases over time", "Отлично, что бросили! Риск снижается со временем", listOf("ref_2")))
        "passive" -> factors.add(RiskFactor("Passive smoking exposure", "Пассивное курение", -1.0, "WHO", "Avoid secondhand smoke environments when possible", "По возможности избегайте мест с табачным дымом", listOf("ref_3")))
        "never" -> positive.add(RiskFactor("Non-smoker", "Не курю", 1.0, "Jha 2013 baseline", "Keep avoiding tobacco — it stays paying off", "Продолжайте избегать табака — польза накапливается", listOf("ref_2")))
    }

    // Activity — Lee Lancet 2012 (PMID 22818936).
    when (data.activity) {
        "inactive", "sedentary" -> factors.add(RiskFactor("Physical inactivity", "Физическая неактивность", -3.0, "Lee Lancet 2012", "Start with 15-minute daily walks", "Начните с 15-минутных ежедневных прогулок", listOf("ref_4")))
        "light" -> factors.add(RiskFactor("Low activity level", "Низкий уровень активности", -1.5, "Lee Lancet 2012", "Aim for 150 minutes of moderate activity per week", "Стремитесь к 150 минутам умеренной активности в неделю", listOf("ref_4")))
        "moderate" -> positive.add(RiskFactor("Moderate activity", "Умеренная активность", 1.0, "AHA guidelines", "You're hitting the floor — keep it up", "Вы достигаете базовой нормы — так держать", listOf("ref_12")))
        "high", "active", "very_active" -> positive.add(RiskFactor("High physical activity", "Высокая физическая активность", 2.5, "Wen / Lee 2012", "Keep up the great work!", "Продолжайте в том же духе!", listOf("ref_4")))
    }

    // Alcohol — Wood Lancet 2018 (PMID 29676281).
    when (data.alcohol) {
        "daily" -> factors.add(RiskFactor("Daily alcohol consumption", "Ежедневное употребление алкоголя", -4.0, "Wood Lancet 2018", "Reduce to occasional drinking or abstain", "Сократите до случайного употребления или откажитесь", listOf("ref_5")))
        "weekend", "weekly" -> factors.add(RiskFactor("Regular weekend drinking", "Регулярное употребление по выходным", -1.0, "Wood Lancet 2018", "Consider reducing frequency", "Рассмотрите снижение частоты", listOf("ref_5")))
        "never", "rare", "monthly" -> positive.add(RiskFactor("Little / no alcohol", "Мало / нет алкоголя", 1.0, "Wood Lancet 2018", "Zero alcohol is optimal for longevity", "Отсутствие алкоголя оптимально для долголетия", listOf("ref_5")))
    }

    // Blood pressure — Lewington Lancet 2002 (PMID 12493255).
    when {
        data.pressureLevel == "high" || data.chronicDiseases == "highpressure" || data.chronicDiseases == "hypertension" ->
            factors.add(RiskFactor("Hypertension", "Гипертония", -4.0, "Lewington Lancet 2002", "Monitor blood pressure regularly; reduce salt intake", "Регулярно контролируйте давление; снизьте потребление соли", listOf("ref_6", "ref_13")))
        data.pressureLevel == "elevated" ->
            factors.add(RiskFactor("Elevated blood pressure", "Повышенное давление", -1.5, "Lewington Lancet 2002", "Lifestyle changes can prevent hypertension", "Изменения образа жизни могут предотвратить гипертонию", listOf("ref_6")))
        data.pressureLevel == "normal" ->
            positive.add(RiskFactor("Normal blood pressure", "Нормальное давление", 0.5, "Lewington Lancet 2002", "Keep monitoring annually", "Продолжайте проверять давление ежегодно", listOf("ref_6")))
    }

    // Diabetes / prediabetes — Seshasai NEJM 2011 (PMID 21366474); CDC.
    when {
        data.chronicDiseases == "diabetes" || data.chronicDiseases == "diabetes1" || data.chronicDiseases == "diabetes2" ->
            factors.add(RiskFactor("Diabetes", "Диабет", -6.0, "Seshasai NEJM 2011", "Maintain strict blood sugar control with your doctor", "Строго контролируйте сахар в крови с врачом", listOf("ref_7")))
        data.chronicDiseases == "prediabetes" || data.sugarLevel == "high" || data.sugarLevel == "yes" ->
            factors.add(RiskFactor("Prediabetes / elevated blood sugar", "Преддиабет / повышенный сахар", -2.0, "CDC", "Diet and exercise can reverse prediabetes", "Диета и упражнения могут обратить преддиабет", listOf("ref_8")))
    }

    if (data.chronicDiseases == "none") {
        positive.add(RiskFactor("No chronic conditions", "Нет хронических заболеваний", 1.0, "—", "Stay ahead with regular check-ups", "Продолжайте плановые осмотры"))
    }
    if (data.chronicDiseases == "cholesterol") {
        factors.add(RiskFactor("High cholesterol", "Высокий холестерин", -1.0, "CDC", "Increase fiber and omega-3 intake", "Увеличьте потребление клетчатки и омега-3", listOf("ref_15")))
    }

    // Sleep — Cappuccio 2010 (PMID 20469800).
    if (data.sleepHours == "less_5" || data.sleepQuality == "poor") {
        factors.add(RiskFactor("Poor sleep", "Плохой сон", -2.0, "Cappuccio Sleep 2010", "Prioritize sleep hygiene: consistent schedule, dark room, no screens", "Приоритизируйте гигиену сна: режим, тёмная комната, без экранов", listOf("ref_9")))
    } else if ((data.sleepHours == "7_8" || data.sleepHours == "more_8") && (data.sleepQuality == "good" || data.sleepQuality == "excellent")) {
        positive.add(RiskFactor("Good sleep habits", "Хорошие привычки сна", 1.0, "Cappuccio Sleep 2010", "Your sleep supports long-term health", "Ваш сон поддерживает долгосрочное здоровье", listOf("ref_9")))
    }

    // Diet — GBD 2017 (PMID 30954305) for unhealthy; PREDIMED for healthy.
    if (data.eatsVegetables == "daily" && (data.eatsJunkFood == "never" || data.eatsJunkFood == "rare")) {
        positive.add(RiskFactor("Healthy diet", "Здоровое питание", 2.0, "PREDIMED", "Your diet is a strong foundation", "Ваше питание — прочный фундамент", listOf("ref_11", "ref_16")))
    } else if (data.eatsJunkFood == "daily" || data.eatsJunkFood == "yes") {
        factors.add(RiskFactor("Unhealthy diet", "Нездоровое питание", -2.0, "GBD 2017 Diet", "Replace processed foods with whole foods gradually", "Постепенно заменяйте обработанные продукты на натуральные", listOf("ref_10")))
    }

    // Family history — AHA / CDC.
    if (data.familyHistory != "none" && data.familyHistory.isNotEmpty()) {
        factors.add(RiskFactor("Family history of cardiovascular disease", "Семейная история сердечно-сосудистых заболеваний", -1.5, "CDC", "Get regular check-ups and maintain a heart-healthy lifestyle", "Регулярно проходите обследования и ведите здоровый образ жизни", listOf("ref_14")))
    }

    val totalImpact = factors.sumOf { it.impact } + positive.sumOf { it.impact }

    // Deficiencies
    val deficiencies = mutableListOf<DeficiencyRisk>()
    val symptomMap: Map<String, DeficiencyRisk> = mapOf(
        "fatigue" to DeficiencyRisk("Iron", "Железо", "Chronic fatigue can indicate iron deficiency", "Хроническая усталость может указывать на дефицит железа", "Red meat, spinach, lentils, fortified cereals", "Красное мясо, шпинат, чечевица, обогащённые каши", listOf("ref_17")),
        "muscle_weakness" to DeficiencyRisk("Vitamin D", "Витамин D", "Muscle weakness linked to low vitamin D", "Мышечная слабость связана с низким уровнем витамина D", "Sunlight, fatty fish, fortified milk", "Солнечный свет, жирная рыба, обогащённое молоко", listOf("ref_18")),
        "hair_loss" to DeficiencyRisk("Iron & Biotin", "Железо и биотин", "Hair loss may indicate iron or biotin deficiency", "Выпадение волос может указывать на дефицит железа или биотина", "Eggs, nuts, seeds, leafy greens", "Яйца, орехи, семена, листовая зелень", listOf("ref_17")),
        "dry_skin" to DeficiencyRisk("Omega-3 & Vitamin E", "Омега-3 и витамин E", "Dry skin can signal essential fatty acid deficiency", "Сухая кожа может сигнализировать о дефиците жирных кислот", "Fatty fish, walnuts, flaxseed, avocado", "Жирная рыба, грецкие орехи, льняное семя, авокадо", listOf("ref_21")),
        "depression" to DeficiencyRisk("Vitamin D & B12", "Витамин D и B12", "Low mood linked to vitamin D and B12 levels", "Пониженное настроение связано с уровнем витаминов D и B12", "Sunlight, fatty fish, fortified foods, eggs", "Солнечный свет, жирная рыба, обогащённые продукты, яйца", listOf("ref_18", "ref_19")),
        "numbness_tingling" to DeficiencyRisk("Vitamin B12", "Витамин B12", "Numbness may indicate B12 deficiency", "Онемение может указывать на дефицит B12", "Meat, fish, dairy, fortified cereals", "Мясо, рыба, молочные продукты, обогащённые каши", listOf("ref_19")),
        "bleeding_gums" to DeficiencyRisk("Vitamin C", "Витамин C", "Bleeding gums are a classic vitamin C sign", "Кровоточивость дёсен — классический признак дефицита витамина C", "Citrus fruits, bell peppers, strawberries", "Цитрусовые, болгарский перец, клубника", listOf("ref_20")),
        "pale_skin" to DeficiencyRisk("Iron & B12", "Железо и B12", "Paleness can indicate anemia", "Бледность может указывать на анемию", "Red meat, beans, fortified cereals", "Красное мясо, бобы, обогащённые каши", listOf("ref_17", "ref_19")),
        "low_immunity" to DeficiencyRisk("Vitamin C & Zinc", "Витамин C и цинк", "Frequent illness may signal immune nutrient gaps", "Частые болезни могут указывать на нехватку нутриентов для иммунитета", "Citrus, nuts, seeds, yogurt", "Цитрусовые, орехи, семена, йогурт", listOf("ref_20", "ref_22")),
        "concentration" to DeficiencyRisk("Iron & B Vitamins", "Железо и витамины группы B", "Poor focus linked to iron and B vitamin levels", "Слабая концентрация связана с уровнем железа и витаминов B", "Whole grains, eggs, leafy greens, fish", "Цельнозерновые, яйца, листовая зелень, рыба", listOf("ref_17", "ref_19"))
    )
    val seenNutrients = mutableSetOf<String>()
    for (symptom in data.symptoms) {
        symptomMap[symptom]?.let { def ->
            if (seenNutrients.add(def.nutrient)) deficiencies.add(def)
        }
    }
    if ((data.activity == "inactive" || data.activity == "sedentary" || data.activity == "light") && "Vitamin D" !in seenNutrients) {
        deficiencies.add(DeficiencyRisk("Vitamin D", "Витамин D", "Low activity reduces sun exposure", "Низкая активность снижает пребывание на солнце", "Consider supplements, fatty fish, fortified foods", "Рассмотрите добавки, жирную рыбу, обогащённые продукты", listOf("ref_18")))
    }
    if (data.gender == "female" && "Iron" !in seenNutrients) {
        deficiencies.add(DeficiencyRisk("Iron", "Железо", "Women are at higher risk for iron deficiency", "Женщины подвержены большему риску дефицита железа", "Red meat, spinach, lentils, vitamin C to aid absorption", "Красное мясо, шпинат, чечевица, витамин C для усвоения", listOf("ref_17")))
    }

    val role = assignTribalRole(data)
    val roleInfo = tribalRoles[role] ?: tribalRoles.values.first()
    return RiskAssessmentResult(
        tribalRole = role,
        nameEn = roleInfo.nameEn, nameRu = roleInfo.nameRu,
        descEn = roleInfo.descEn, descRu = roleInfo.descRu,
        icon = roleInfo.icon,
        bmi = bmi, bmiCategory = bmiCat, totalImpact = totalImpact,
        riskFactors = factors, positiveFactors = positive, deficiencies = deficiencies
    )
}

// v1.3 — wellness archetypes replace the prior tribal vocabulary. Internal
// keys remain stable (warrior/healer/sage/scout) so any persisted role survives
// the rename; only the user-visible titles/descriptions change.

fun tribalRoleEmoji(role: String): String = when (role) {
    "healer" -> "🌿"; "warrior", "guardian" -> "🛡️"; "shepherd", "builder" -> "🏗️"; "sage", "scout" -> "🧭"
    else -> "🚶"
}

fun tribalRoleTitle(role: String, ru: Boolean = false): String = if (ru) when (role) {
    "healer" -> "Целитель"
    "warrior", "guardian" -> "Хранитель"
    "shepherd", "builder" -> "Строитель"
    "sage", "scout" -> "Исследователь"
    else -> "Путник"
} else when (role) {
    "healer" -> "Healer"
    "warrior", "guardian" -> "Guardian"
    "shepherd", "builder" -> "Builder"
    "sage", "scout" -> "Explorer"
    else -> "Wayfarer"
}

fun tribalRoleFocus(role: String, ru: Boolean = false): String = if (ru) when (role) {
    "healer" -> "Восстановление, питание и сон"
    "warrior", "guardian" -> "Выносливость и регулярные тренировки"
    "shepherd", "builder" -> "Сила, дисциплина и постоянство"
    "sage", "scout" -> "Разнообразие, баланс и исследование"
    else -> "Иди своим путём"
} else when (role) {
    "healer" -> "Recovery, nutrition, and sleep"
    "warrior", "guardian" -> "Endurance and consistent training"
    "shepherd", "builder" -> "Strength, discipline, and consistency"
    "sage", "scout" -> "Variety, balance, and discovery"
    else -> "Walk your own path"
}

fun tribalRoleDescription(role: String, ru: Boolean = false): String = if (ru) when (role) {
    "healer" -> "Вы цените восстановление, спокойствие и качественный сон."
    "warrior", "guardian" -> "Вы поддерживаете форму регулярными нагрузками и дисциплиной."
    "shepherd", "builder" -> "Вы строите привычки шаг за шагом и доводите дело до конца."
    "sage", "scout" -> "Вы ищете баланс — движение, питание, восстановление."
    else -> "Вы идёте своим путём. Двигайтесь так, как удобно вам."
} else when (role) {
    "healer" -> "You value recovery, calm, and high-quality sleep."
    "warrior", "guardian" -> "You stay in shape through consistent training and discipline."
    "shepherd", "builder" -> "You build habits step by step and see them through."
    "sage", "scout" -> "You seek balance — movement, nutrition, and recovery."
    else -> "You walk your own path. Move in the way that works for you."
}

val tribalRoles: Map<String, RiskAssessmentResult> = mapOf(
    "warrior" to RiskAssessmentResult("warrior", "Guardian", "Хранитель",
        "You stay in shape through consistent training and disciplined recovery.",
        "Вы поддерживаете форму регулярными нагрузками и дисциплинированным восстановлением.",
        "🛡️"),
    "sage" to RiskAssessmentResult("sage", "Explorer", "Исследователь",
        "You seek balance — variety in movement, mindful eating, and steady recovery.",
        "Вы ищете баланс — разнообразие движения, осознанное питание и стабильное восстановление.",
        "🧭"),
    "healer" to RiskAssessmentResult("healer", "Healer", "Целитель",
        "You prioritise recovery, calm, and high-quality sleep.",
        "Вы цените восстановление, спокойствие и качественный сон.",
        "🌿"),
    "scout" to RiskAssessmentResult("scout", "Builder", "Строитель",
        "You build habits step by step — consistency over intensity.",
        "Вы строите привычки шаг за шагом — регулярность важнее интенсивности.",
        "🏗️"),
)

val oracleQuestions: List<OracleQuestion> = listOf(
    OracleQuestion("q1",
        oracleText = "The Seer looks into your heart...",
        oracleTextRu = "Провидец заглядывает в твоё сердце...",
        text = "What is your primary wellness goal?",
        textRu = "Какова твоя главная цель для здоровья?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("lose_weight",    "Lose weight",     "Похудеть",        "guardian"),
            OracleOption("build_strength", "Build strength",  "Набрать силу",    "warrior"),
            OracleOption("improve_energy", "Improve energy",  "Повысить энергию","guardian"),
            OracleOption("reduce_stress",  "Reduce stress",   "Снизить стресс",  "healer"),
            OracleOption("better_sleep",   "Better sleep",    "Улучшить сон",    "healer"),
            OracleOption("overall_health", "Overall health",  "Общее здоровье",  "scout"),
        ), dataKey = "goal"),
    OracleQuestion("q2",
        oracleText = "The spirits whisper of your form...",
        oracleTextRu = "Духи шепчут о твоём облике...",
        text = "What is your gender?",
        textRu = "Какой у тебя пол?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("male",   "Male",   "Мужской",  "warrior"),
            OracleOption("female", "Female", "Женский",  "healer"),
            OracleOption("other",  "Other",  "Другой",   "scout"),
        ), dataKey = "gender"),
    OracleQuestion("q3",
        oracleText = "How many summers have you seen?",
        oracleTextRu = "Сколько лет ты прожил?",
        text = "Your age",
        textRu = "Твой возраст",
        type = QuestionType.NUMBER,
        dataKey = "age"),
    OracleQuestion("q4",
        oracleText = "The oracle reads the body's map...",
        oracleTextRu = "Оракул читает карту тела...",
        text = "Your height (cm)",
        textRu = "Твой рост (см)",
        type = QuestionType.NUMBER,
        dataKey = "height"),
    OracleQuestion("q5",
        oracleText = "Every soul has its weight...",
        oracleTextRu = "У каждой души есть свой вес...",
        text = "Your weight (kg)",
        textRu = "Твой вес (кг)",
        type = QuestionType.NUMBER,
        dataKey = "weight"),
    OracleQuestion("q6",
        oracleText = "How does the warrior train?",
        oracleTextRu = "Как воин тренируется?",
        text = "How active are you?",
        textRu = "Насколько ты активен?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("sedentary",  "Sedentary (desk job, little exercise)",    "Сидячий образ жизни",           "healer"),
            OracleOption("light",      "Light (walks, light exercise 1–2×/week)",   "Лёгкая активность 1–2 раза/нед", "scout"),
            OracleOption("moderate",   "Moderate (exercise 3–4×/week)",             "Умеренная 3–4 раза/нед",         "scout"),
            OracleOption("active",     "Active (daily exercise or physical job)",   "Активный, ежедневно",            "warrior"),
            OracleOption("very_active","Very active (intense training or athlete)", "Очень активный, атлет",          "warrior"),
        ), dataKey = "activity"),
    OracleQuestion("q7",
        oracleText = "The smoke or the clean air?",
        oracleTextRu = "Дым или чистый воздух?",
        text = "Do you smoke?",
        textRu = "Ты куришь?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("never",   "Never",           "Никогда",         "warrior"),
            OracleOption("former",  "Former smoker",   "Бывший курильщик","scout"),
            OracleOption("current", "Current smoker",  "Курю сейчас",     "healer"),
        ), dataKey = "smoking"),
    OracleQuestion("q8",
        oracleText = "Does the spirit drink?",
        oracleTextRu = "Пьёт ли дух?",
        text = "How often do you drink alcohol?",
        textRu = "Как часто ты пьёшь алкоголь?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("never",   "Never",                  "Никогда",               "warrior"),
            OracleOption("rare",    "Rarely (few/year)",      "Редко (раз в год)",      "scout"),
            OracleOption("monthly", "Monthly",                "Ежемесячно",             "scout"),
            OracleOption("weekly",  "Weekly",                 "Еженедельно",            "healer"),
            OracleOption("daily",   "Daily",                  "Ежедневно",              "healer"),
        ), dataKey = "alcohol"),
    OracleQuestion("q9",
        oracleText = "The moon cycle speaks...",
        oracleTextRu = "Лунный цикл говорит...",
        text = "Do you have a menstrual cycle?",
        textRu = "Есть ли у тебя менструальный цикл?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("yes",     "Yes, regular",    "Да, регулярный",   "healer"),
            OracleOption("irregular","Irregular",      "Нерегулярный",     "healer"),
            OracleOption("no",      "No / N/A",        "Нет / Н/П",        "warrior"),
        ), dataKey = "menstrualCycle",
        conditional = { it.gender == "female" }),
    OracleQuestion("q10",
        oracleText = "What burdens do you carry?",
        oracleTextRu = "Какой груз ты несёшь?",
        text = "Do you have any chronic conditions?",
        textRu = "Есть ли у тебя хронические заболевания?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("none",       "None",                  "Нет",                    "warrior"),
            OracleOption("diabetes",   "Diabetes",              "Диабет",                 "healer"),
            OracleOption("hypertension","Hypertension",         "Гипертония",             "healer"),
            OracleOption("heart",      "Heart disease",         "Болезнь сердца",         "healer"),
            OracleOption("thyroid",    "Thyroid condition",     "Заболевание щитовидки",  "healer"),
            OracleOption("other",      "Other",                 "Другое",                 "scout"),
        ), dataKey = "chronicDiseases"),
    OracleQuestion("q11",
        oracleText = "What shadows visit?",
        oracleTextRu = "Какие тени посещают тебя?",
        text = "Any current symptoms? (Select all that apply)",
        textRu = "Какие симптомы ты испытываешь? (выбери все подходящие)",
        type = QuestionType.MULTI,
        options = listOf(
            OracleOption("fatigue",      "Chronic fatigue",     "Хроническая усталость",  "healer"),
            OracleOption("headaches",    "Frequent headaches",  "Частые головные боли",   "healer"),
            OracleOption("bloating",     "Bloating/gut issues", "Вздутие/проблемы ЖКТ",  "healer"),
            OracleOption("joint_pain",   "Joint pain",          "Боли в суставах",        "warrior"),
            OracleOption("mood",         "Mood swings",         "Перепады настроения",    "sage"),
            OracleOption("none",         "None",                "Нет",                    "warrior"),
        ), dataKey = "symptoms"),
    OracleQuestion("q12",
        oracleText = "Does the night restore you?",
        oracleTextRu = "Ночь восстанавливает тебя?",
        text = "How many hours of sleep do you get?",
        textRu = "Сколько часов ты спишь?",
        type = QuestionType.NUMBER,
        dataKey = "sleepHours"),
    OracleQuestion("q13",
        oracleText = "Is your sleep deep or shallow?",
        oracleTextRu = "Твой сон глубокий или поверхностный?",
        text = "Sleep quality",
        textRu = "Качество сна",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("excellent","Excellent",   "Отличное",     "warrior"),
            OracleOption("good",     "Good",        "Хорошее",      "scout"),
            OracleOption("fair",     "Fair",        "Среднее",      "sage"),
            OracleOption("poor",     "Poor",        "Плохое",       "healer"),
        ), dataKey = "sleepQuality"),
    OracleQuestion("q14",
        oracleText = "The blood pressure speaks...",
        oracleTextRu = "Кровяное давление говорит...",
        text = "Blood pressure level",
        textRu = "Уровень кровяного давления",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("normal", "Normal",     "Нормальное",         "warrior"),
            OracleOption("high",   "High",       "Высокое",            "healer"),
            OracleOption("low",    "Low",        "Низкое",             "healer"),
            OracleOption("unknown","Don't know", "Не знаю",            "scout"),
        ), dataKey = "pressureLevel"),
    OracleQuestion("q15",
        oracleText = "The sweetness in the blood...",
        oracleTextRu = "Сладость в крови...",
        text = "Blood sugar level",
        textRu = "Уровень сахара в крови",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("normal",  "Normal",    "Нормальный",    "warrior"),
            OracleOption("high",    "High / pre-diabetic", "Высокий / преддиабет", "healer"),
            OracleOption("unknown", "Don't know","Не знаю",       "scout"),
        ), dataKey = "sugarLevel"),
    OracleQuestion("q16",
        oracleText = "Does the earth feed you well?",
        oracleTextRu = "Земля хорошо кормит тебя?",
        text = "How often do you eat vegetables/fruit?",
        textRu = "Как часто ты ешь овощи и фрукты?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("daily",   "Daily",                 "Каждый день",                  "warrior"),
            OracleOption("often",   "Most days",             "Большинство дней",             "scout"),
            OracleOption("sometimes","Sometimes",            "Иногда",                       "sage"),
            OracleOption("rarely",  "Rarely",                "Редко",                        "healer"),
        ), dataKey = "eatsVegetables"),
    OracleQuestion("q17",
        oracleText = "Does shadow food tempt you?",
        oracleTextRu = "Тёмная еда искушает тебя?",
        text = "How often do you eat processed/junk food?",
        textRu = "Как часто ты ешь переработанную / вредную еду?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("never",   "Never",           "Никогда",              "warrior"),
            OracleOption("rare",    "Rarely",          "Редко",                "scout"),
            OracleOption("weekly",  "Weekly",          "Еженедельно",          "sage"),
            OracleOption("daily",   "Daily",           "Ежедневно",            "healer"),
        ), dataKey = "eatsJunkFood"),
    OracleQuestion("q18",
        oracleText = "What shadows live in your bloodline?",
        oracleTextRu = "Какие тени живут в твоём роду?",
        text = "Family history of illness?",
        textRu = "Есть ли в семье наследственные болезни?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("none",        "None",               "Нет",                     "warrior"),
            OracleOption("heart",       "Heart disease",      "Болезни сердца",          "healer"),
            OracleOption("diabetes",    "Diabetes",           "Диабет",                  "healer"),
            OracleOption("cancer",      "Cancer",             "Рак",                     "sage"),
            OracleOption("other",       "Other",              "Другое",                  "scout"),
        ), dataKey = "familyHistory"),
)

fun assignTribalRole(data: HealthData): String {
    val scores = mutableMapOf("warrior" to 0, "healer" to 0, "sage" to 0, "scout" to 0)

    fun score(role: String, pts: Int = 1) { scores[role] = (scores[role] ?: 0) + pts }

    // Goal
    when (data.goal) {
        "build_strength" -> score("warrior", 2)
        "reduce_stress", "better_sleep" -> score("healer", 2)
        "overall_health" -> score("scout")
        else -> score("warrior")
    }
    // Activity
    when (data.activity) {
        "active", "very_active" -> score("warrior", 2)
        "moderate" -> score("scout")
        "sedentary" -> score("healer")
    }
    // Smoking
    if (data.smoking == "current") score("healer", 2)
    if (data.smoking == "never") score("warrior")
    // Sleep
    val sleepHrs = data.sleepHours.toIntOrNull() ?: 7
    if (sleepHrs < 6) score("healer", 2) else if (sleepHrs >= 8) score("warrior")
    if (data.sleepQuality == "poor") score("healer")
    // Diet
    when (data.eatsVegetables) {
        "daily" -> score("warrior")
        "rarely" -> score("healer")
    }
    when (data.eatsJunkFood) {
        "daily" -> score("healer", 2)
        "never" -> score("warrior")
    }
    // Chronic
    if (data.chronicDiseases != "none" && data.chronicDiseases.isNotEmpty()) score("healer")
    // Symptoms
    if (data.symptoms.contains("mood")) score("sage")
    if (data.symptoms.contains("fatigue") || data.symptoms.contains("headaches")) score("healer")

    return scores.maxByOrNull { it.value }?.key ?: "scout"
}
