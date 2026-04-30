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
    // v1.3.4: ported verbatim from iOS `OracleCitations.all`. Risk-factor
    // citationIds use the institutional named ids (oxford_bmi, who_tobacco,
    // …); deficiency citationIds use the PubMed/PMC ref_N pool. Match the
    // iOS list line-for-line so deep links open the exact same paper.
    val all: List<OracleCitation> = listOf(
        // Institutional / guideline sources used by lifestyle risk factors
        OracleCitation("oxford_bmi", "Body-mass index and cause-specific mortality (Oxford Prospective Studies Collaboration, Lancet 2009)", "https://pubmed.ncbi.nlm.nih.gov/19299006/"),
        OracleCitation("who_tobacco", "WHO — Tobacco fact sheet", "https://www.who.int/news-room/fact-sheets/detail/tobacco"),
        OracleCitation("cdc_tobacco", "U.S. CDC — Smoking & Tobacco Use", "https://www.cdc.gov/tobacco/about/index.html"),
        OracleCitation("who_secondhand", "WHO — Protecting people from tobacco smoke", "https://www.who.int/activities/protecting-people-from-tobacco-smoke"),
        OracleCitation("aging_us_inactivity", "Accelerated aging mediates unhealthy lifestyles (PubMed PMID 37789775)", "https://pubmed.ncbi.nlm.nih.gov/37789775/"),
        OracleCitation("aha_activity", "U.S. CDC — Adult Physical Activity Guidelines", "https://www.cdc.gov/physical-activity-basics/guidelines/adults.html"),
        OracleCitation("lancet_alcohol", "GBD 2016 Alcohol Collaborators — Lancet 2018 (PMID 30146330)", "https://pubmed.ncbi.nlm.nih.gov/30146330/"),
        OracleCitation("aging_us_hypertension", "U.S. CDC — About High Blood Pressure", "https://www.cdc.gov/high-blood-pressure/about/index.html"),
        OracleCitation("aging_us_diabetes", "U.S. CDC — Diabetes Basics", "https://www.cdc.gov/diabetes/about/index.html"),
        OracleCitation("cdc_prediabetes", "U.S. CDC — Prediabetes: Your Chance to Prevent Type 2 Diabetes", "https://www.cdc.gov/diabetes/prevention-type-2/prediabetes-prevent-type-2.html"),
        OracleCitation("aging_us_cholesterol", "U.S. CDC — LDL and HDL Cholesterol and Triglycerides", "https://www.cdc.gov/cholesterol/about/ldl-and-hdl-cholesterol-and-triglycerides.html"),
        OracleCitation("mayo_sleep", "Mayo Clinic — How many hours of sleep are enough for good health?", "https://www.mayoclinic.org/healthy-lifestyle/adult-health/expert-answers/how-many-hours-of-sleep-are-enough/faq-20057898"),
        OracleCitation("aha_diet", "WHO — Healthy diet fact sheet", "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"),
        OracleCitation("aha_family_history", "U.S. CDC — Family history of heart disease", "https://www.cdc.gov/heart-disease-family-history/about/index.html"),

        // Peer-reviewed nutrient deficiency references — each verified to a
        // real PubMed/PMC article on the corresponding topic.
        OracleCitation("ref_1",  "Hyperpigmentation in Vitamin B12 Deficiency (PubMed PMID 35020987)", "https://pubmed.ncbi.nlm.nih.gov/35020987/"),
        OracleCitation("ref_2",  "Mokta JK et al., 2017 — Hypovitaminosis D & proximal muscle weakness (PubMed PMID 29322711)", "https://pubmed.ncbi.nlm.nih.gov/29322711/"),
        OracleCitation("ref_3",  "Gilbert C., 2013 — Eye signs of vitamin A deficiency (PMC3936686)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC3936686/"),
        OracleCitation("ref_4",  "Scurvy: Rediscovering a Forgotten Disease — review (PMC10296835)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC10296835/"),
        OracleCitation("ref_5",  "Bechara N. et al., 2022 — Vitamin C in tissue healing (PMC9405326)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC9405326/"),
        OracleCitation("ref_6",  "Rubino P. et al., 2015 — Vitamin A deficiency case series (PubMed PMID 26509090)", "https://pubmed.ncbi.nlm.nih.gov/26509090/"),
        OracleCitation("ref_7",  "Miller K.C. et al., 2021 — Exercise-associated muscle cramps review (PMC8775277)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC8775277/"),
        OracleCitation("ref_8",  "Borgna-Pignatti C., Zanella S., 2016 — Pica & iron deficiency (PubMed PMID 27701928)", "https://pubmed.ncbi.nlm.nih.gov/27701928/"),
        OracleCitation("ref_9",  "Sebo P. et al., 2014 — Magnesium for nocturnal leg cramps (PubMed PMID 24280947)", "https://pubmed.ncbi.nlm.nih.gov/24280947/"),
        OracleCitation("ref_10", "Thompson K.G., Kim N., 2021 — Dietary supplements in dermatology (PubMed PMID 32360756)", "https://pubmed.ncbi.nlm.nih.gov/32360756/"),
        OracleCitation("ref_11", "Leung A.K.C. et al., 2024 — Iron Deficiency Anemia review (PubMed PMID 37497686)", "https://pubmed.ncbi.nlm.nih.gov/37497686/"),
        OracleCitation("ref_12", "Wang J. et al., 2018 — Zinc, Magnesium, Selenium and Depression (PMC5986464)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC5986464/"),
        OracleCitation("ref_13", "Trost L.B. et al., 2006 — Iron deficiency & hair loss (PubMed PMID 16635664)", "https://pubmed.ncbi.nlm.nih.gov/16635664/"),
        OracleCitation("ref_14", "Köbe T. et al., 2016 — Vitamin B12, memory, hippocampus (PubMed PMID 26912492)", "https://pubmed.ncbi.nlm.nih.gov/26912492/"),
        OracleCitation("ref_15", "Gombart A.F. et al., 2020 — Micronutrients & immune system (PubMed PMID 31963293)", "https://pubmed.ncbi.nlm.nih.gov/31963293/"),
        OracleCitation("ref_16", "Sawada Y. et al., 2021 — Omega-3 fatty acids & skin diseases (PMC7892455)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC7892455/"),
        OracleCitation("ref_17", "Yokoi K., Konomi A., 2017 — Iron deficiency without anaemia & fatigue (PubMed PMID 28625177)", "https://pubmed.ncbi.nlm.nih.gov/28625177/"),
        OracleCitation("ref_18", "Staff N.P., Windebank A.J., 2014 — Peripheral neuropathy review (PMC4208100)", "https://pmc.ncbi.nlm.nih.gov/articles/PMC4208100/"),
        OracleCitation("ref_19", "Chiang C.P. et al., 2019 — Atrophic glossitis & nutrient deficiency (PubMed PMID 31076315)", "https://pubmed.ncbi.nlm.nih.gov/31076315/"),
        OracleCitation("ref_20", "Jagadeesan S., Kaliyadan F. — Acrodermatitis Enteropathica (StatPearls NBK441835)", "https://www.ncbi.nlm.nih.gov/books/NBK441835/"),
        OracleCitation("ref_21", "Iorizzo M. et al., 2022 — Leukonychia: What Can White Nails Tell Us? (PubMed PMID 35112320)", "https://pubmed.ncbi.nlm.nih.gov/35112320/"),
        OracleCitation("ref_22", "Redzic S. et al. — Niacin Deficiency / Pellagra (StatPearls NBK557728)", "https://www.ncbi.nlm.nih.gov/books/NBK557728/"),
        OracleCitation("ref_23", "Helali J. et al., 2019 — Thiamine & cardiac beriberi (PubMed PMID 31193878)", "https://pubmed.ncbi.nlm.nih.gov/31193878/"),
        OracleCitation("ref_24", "Skelton W.P. III & Skelton N.K., 1989 — Thiamine neuropathy (PubMed PMID 2542916)", "https://pubmed.ncbi.nlm.nih.gov/2542916/"),
        OracleCitation("ref_25", "Fatemi Naieni F. et al., 2012 — Copper deficiency & premature graying (PubMed PMID 21979243)", "https://pubmed.ncbi.nlm.nih.gov/21979243/"),
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
            bmi >= 40 -> factors.add(RiskFactor("Severe obesity (BMI 40+)", "Тяжёлое ожирение (ИМТ 40+)", -10.0, "PSC Lancet 2009", "Consult a doctor for a weight management plan", "Обратитесь к врачу для плана управления весом", listOf("oxford_bmi")))
            bmi >= 30 -> factors.add(RiskFactor("Obesity (BMI 30-40)", "Ожирение (ИМТ 30-40)", -3.0, "PSC Lancet 2009", "Focus on gradual, sustainable weight loss", "Сосредоточьтесь на постепенном, устойчивом снижении веса", listOf("oxford_bmi")))
            bmi in 18.5..25.0 -> positive.add(RiskFactor("Healthy weight (BMI 18.5–25)", "Здоровый вес (ИМТ 18,5–25)", 1.0, "PSC Lancet 2009", "Maintaining this range supports longevity", "Поддержание этого диапазона способствует долголетию", listOf("oxford_bmi")))
        }
    }

    // Smoking — iOS ids: yes / former / passive / no.
    when (data.smoking) {
        "yes" -> factors.add(RiskFactor("Active smoking", "Активное курение", -10.0, "WHO / CDC", "Quitting smoking is the single best thing you can do for your health", "Бросить курить — лучшее, что вы можете сделать для здоровья", listOf("who_tobacco", "cdc_tobacco")))
        "former" -> factors.add(RiskFactor("Former smoker", "Бывший курильщик", -2.0, "CDC", "Great that you quit! Risk decreases over time", "Отлично, что бросили! Риск снижается со временем", listOf("cdc_tobacco")))
        "passive" -> factors.add(RiskFactor("Passive smoking exposure", "Пассивное курение", -1.0, "WHO", "Avoid secondhand smoke environments when possible", "По возможности избегайте мест с табачным дымом", listOf("who_secondhand")))
        "no" -> positive.add(RiskFactor("Non-smoker", "Не курит", 1.0, "WHO baseline", "Staying smoke-free is one of the strongest things you can do for long life", "Отказ от курения — одно из самых сильных решений для долгой жизни", listOf("who_tobacco")))
    }

    // Activity — Lee Lancet 2012 (PMID 22818936). iOS ids: inactive / light / moderate / active / high.
    when (data.activity) {
        "inactive" -> factors.add(RiskFactor("Physical inactivity", "Физическая неактивность", -3.0, "PMID 37789775", "Start with 15-minute daily walks", "Начните с 15-минутных ежедневных прогулок", listOf("aging_us_inactivity")))
        "light" -> factors.add(RiskFactor("Low activity level", "Низкий уровень активности", -1.5, "PMID 37789775", "Aim for 150 minutes of moderate activity per week", "Стремитесь к 150 минутам умеренной активности в неделю", listOf("aging_us_inactivity")))
        "moderate" -> positive.add(RiskFactor("Moderate physical activity", "Умеренная физическая активность", 1.0, "CDC guidelines", "You're meeting baseline guidelines — pushing to 5+ days/week adds even more years", "Вы выполняете базовые рекомендации — 5+ дней/нед добавит ещё больше лет", listOf("aha_activity")))
        "high", "active" -> positive.add(RiskFactor("High physical activity", "Высокая физическая активность", 2.5, "CDC guidelines", "Keep up the great work!", "Продолжайте в том же духе!", listOf("aha_activity")))
    }

    // Alcohol — Wood Lancet 2018 (PMID 29676281). iOS ids: never / rarely / weekend / daily.
    when (data.alcohol) {
        "daily" -> factors.add(RiskFactor("Daily alcohol consumption", "Ежедневное употребление алкоголя", -4.0, "Lancet GBD 2018", "Reduce to occasional drinking or abstain", "Сократите до случайного употребления или откажитесь", listOf("lancet_alcohol")))
        "weekend" -> factors.add(RiskFactor("Regular weekend drinking", "Регулярное употребление по выходным", -1.0, "Lancet GBD 2018", "Consider reducing frequency", "Рассмотрите снижение частоты", listOf("lancet_alcohol")))
        "never", "rarely" -> positive.add(RiskFactor("Little or no alcohol", "Мало или нет алкоголя", 1.0, "Lancet GBD 2018", "Lowest-risk alcohol consumption is none — keep it that way", "Наименьший риск — отсутствие алкоголя; так и держите", listOf("lancet_alcohol")))
    }

    // Blood pressure — iOS ids: normal / elevated / high / dont_know.
    when {
        data.pressureLevel == "high" || data.chronicDiseases == "highpressure" ->
            factors.add(RiskFactor("Hypertension", "Гипертония", -4.0, "CDC", "Monitor blood pressure regularly; reduce salt intake", "Регулярно контролируйте давление; снизьте потребление соли", listOf("aging_us_hypertension")))
        data.pressureLevel == "elevated" ->
            factors.add(RiskFactor("Elevated blood pressure", "Повышенное давление", -1.5, "CDC", "Lifestyle changes can prevent hypertension", "Изменения образа жизни могут предотвратить гипертонию", listOf("aging_us_hypertension")))
        data.pressureLevel == "normal" ->
            positive.add(RiskFactor("Normal blood pressure", "Нормальное давление", 0.5, "CDC", "Re-check yearly to catch any drift early", "Проверяйте давление раз в год, чтобы вовремя заметить изменения", listOf("aging_us_hypertension")))
    }

    // Diabetes / prediabetes — iOS ids: chronicDiseases ∈ {diabetes1, diabetes2, prediabetes, …}; sugarLevel ∈ {no, yes, dont_know}.
    when {
        data.chronicDiseases == "diabetes1" || data.chronicDiseases == "diabetes2" ->
            factors.add(RiskFactor("Diabetes", "Диабет", -6.0, "CDC", "Maintain strict blood sugar control with your doctor", "Строго контролируйте сахар в крови с врачом", listOf("aging_us_diabetes")))
        data.chronicDiseases == "prediabetes" || data.sugarLevel == "yes" ->
            factors.add(RiskFactor("Prediabetes / elevated blood sugar", "Преддиабет / повышенный сахар", -2.0, "CDC", "Diet and exercise can reverse prediabetes", "Диета и упражнения могут обратить преддиабет", listOf("cdc_prediabetes")))
        data.chronicDiseases == "none" && data.sugarLevel == "no" ->
            positive.add(RiskFactor("No chronic conditions", "Нет хронических заболеваний", 1.0, "CDC", "Keep up routine check-ups to maintain this baseline", "Продолжайте регулярные осмотры, чтобы сохранить этот результат", listOf("aging_us_diabetes")))
    }

    if (data.chronicDiseases == "cholesterol") {
        factors.add(RiskFactor("High cholesterol", "Высокий холестерин", -1.0, "CDC", "Increase fiber and omega-3 intake", "Увеличьте потребление клетчатки и омега-3", listOf("aging_us_cholesterol")))
    }

    // Sleep — Cappuccio 2010 (PMID 20469800).
    if (data.sleepHours == "less_5" || data.sleepQuality == "poor") {
        factors.add(RiskFactor("Poor sleep", "Плохой сон", -2.0, "Mayo Clinic", "Prioritize sleep hygiene: consistent schedule, dark room, no screens", "Приоритизируйте гигиену сна: режим, тёмная комната, без экранов", listOf("mayo_sleep")))
    } else if ((data.sleepHours == "7_8" || data.sleepHours == "more_8") && (data.sleepQuality == "good" || data.sleepQuality == "excellent")) {
        positive.add(RiskFactor("Good sleep habits", "Хорошие привычки сна", 1.0, "Mayo Clinic", "Your sleep supports long-term health", "Ваш сон поддерживает долгосрочное здоровье", listOf("mayo_sleep")))
    }

    // Diet — iOS ids: eatsVegetables ∈ {yes, sometimes, no}; eatsJunkFood ∈ {no, sometimes, yes}.
    if (data.eatsVegetables == "yes" && data.eatsJunkFood == "no") {
        positive.add(RiskFactor("Healthy diet", "Здоровое питание", 2.0, "WHO", "Your diet is a strong foundation", "Ваше питание — прочный фундамент", listOf("aha_diet")))
    } else if (data.eatsJunkFood == "yes") {
        factors.add(RiskFactor("Unhealthy diet", "Нездоровое питание", -2.0, "WHO", "Replace processed foods with whole foods gradually", "Постепенно заменяйте обработанные продукты на натуральные", listOf("aha_diet")))
    }

    // Family history — iOS ids: yes / no / dont_know.
    if (data.familyHistory == "yes") {
        factors.add(RiskFactor("Family history of cardiovascular disease", "Семейная история сердечно-сосудистых заболеваний", -1.5, "CDC", "Get regular check-ups and maintain a heart-healthy lifestyle", "Регулярно проходите обследования и ведите здоровый образ жизни", listOf("aha_family_history")))
    }

    val totalImpact = factors.sumOf { it.impact } + positive.sumOf { it.impact }

    // Deficiencies
    val deficiencies = mutableListOf<DeficiencyRisk>()
    // v1.3.4: ref_N now point to the iOS-aligned PubMed/PMC papers. Mapping
    // taken from iOS `OracleData.swift` symptomMap so each row deep-links to
    // the same study on both platforms.
    val symptomMap: Map<String, DeficiencyRisk> = mapOf(
        "fatigue" to DeficiencyRisk("Iron", "Железо", "Chronic fatigue can indicate iron deficiency", "Хроническая усталость может указывать на дефицит железа", "Red meat, spinach, lentils, fortified cereals", "Красное мясо, шпинат, чечевица, обогащённые каши", listOf("ref_17", "ref_8")),
        "muscle_weakness" to DeficiencyRisk("Vitamin D", "Витамин D", "Muscle weakness linked to low vitamin D", "Мышечная слабость связана с низким уровнем витамина D", "Sunlight, fatty fish, fortified milk", "Солнечный свет, жирная рыба, обогащённое молоко", listOf("ref_2")),
        "hair_loss" to DeficiencyRisk("Iron & Biotin", "Железо и биотин", "Hair loss may indicate iron or biotin deficiency", "Выпадение волос может указывать на дефицит железа или биотина", "Eggs, nuts, seeds, leafy greens", "Яйца, орехи, семена, листовая зелень", listOf("ref_13")),
        "dry_skin" to DeficiencyRisk("Omega-3 & Vitamin E", "Омега-3 и витамин E", "Dry skin can signal essential fatty acid deficiency", "Сухая кожа может сигнализировать о дефиците жирных кислот", "Fatty fish, walnuts, flaxseed, avocado", "Жирная рыба, грецкие орехи, льняное семя, авокадо", listOf("ref_16")),
        "depression" to DeficiencyRisk("Vitamin D & B12", "Витамин D и B12", "Low mood linked to vitamin D and B12 levels", "Пониженное настроение связано с уровнем витаминов D и B12", "Sunlight, fatty fish, fortified foods, eggs", "Солнечный свет, жирная рыба, обогащённые продукты, яйца", listOf("ref_12")),
        "numbness_tingling" to DeficiencyRisk("Vitamin B12", "Витамин B12", "Numbness may indicate B12 deficiency", "Онемение может указывать на дефицит B12", "Meat, fish, dairy, fortified cereals", "Мясо, рыба, молочные продукты, обогащённые каши", listOf("ref_18")),
        "bleeding_gums" to DeficiencyRisk("Vitamin C", "Витамин C", "Bleeding gums are a classic vitamin C sign", "Кровоточивость дёсен — классический признак дефицита витамина C", "Citrus fruits, bell peppers, strawberries", "Цитрусовые, болгарский перец, клубника", listOf("ref_4")),
        "pale_skin" to DeficiencyRisk("Iron & B12", "Железо и B12", "Paleness can indicate anemia", "Бледность может указывать на анемию", "Red meat, beans, fortified cereals", "Красное мясо, бобы, обогащённые каши", listOf("ref_11")),
        "low_immunity" to DeficiencyRisk("Vitamin C & Zinc", "Витамин C и цинк", "Frequent illness may signal immune nutrient gaps", "Частые болезни могут указывать на нехватку нутриентов для иммунитета", "Citrus, nuts, seeds, yogurt", "Цитрусовые, орехи, семена, йогурт", listOf("ref_15")),
        "concentration" to DeficiencyRisk("Iron & B Vitamins", "Железо и витамины группы B", "Poor focus linked to iron and B vitamin levels", "Слабая концентрация связана с уровнем железа и витаминов B", "Whole grains, eggs, leafy greens, fish", "Цельнозерновые, яйца, листовая зелень, рыба", listOf("ref_14"))
    )
    val seenNutrients = mutableSetOf<String>()
    for (symptom in data.symptoms) {
        symptomMap[symptom]?.let { def ->
            if (seenNutrients.add(def.nutrient)) deficiencies.add(def)
        }
    }
    if ((data.activity == "inactive" || data.activity == "light") && "Vitamin D" !in seenNutrients) {
        deficiencies.add(DeficiencyRisk("Vitamin D", "Витамин D", "Low activity reduces sun exposure", "Низкая активность снижает пребывание на солнце", "Consider supplements, fatty fish, fortified foods", "Рассмотрите добавки, жирную рыбу, обогащённые продукты", listOf("ref_2")))
    }
    if (data.gender == "female" && "Iron" !in seenNutrients) {
        deficiencies.add(DeficiencyRisk("Iron", "Железо", "Women are at higher risk for iron deficiency", "Женщины подвержены большему риску дефицита железа", "Red meat, spinach, lentils, vitamin C to aid absorption", "Красное мясо, шпинат, чечевица, витамин C для усвоения", listOf("ref_11")))
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

// v1.3.4: Oracle questionnaire ported verbatim from iOS `OracleData.swift`
// (`oracleQuestions`). Earlier Android revisions had drifted — option ids
// like `current` for smoking, NUMBER input for sleepHours, free-form q11
// symptoms — none of which `calculateRiskAssessment` or `symptomMap` matched.
// The questionnaire UI now sends ids the assessment logic actually consumes
// (e.g. `less_5/5_6/6_7/7_8/more_8` for sleep, `fatigue/muscle_weakness/…`
// for symptoms, `yes/no/dont_know` for sugar/family-history).
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
        oracleText = "The stars reveal your nature...",
        oracleTextRu = "Звёзды раскрывают твою природу...",
        text = "What is your gender?",
        textRu = "Какой у тебя пол?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("male",   "Male",   "Мужской",  ""),
            OracleOption("female", "Female", "Женский",  ""),
        ), dataKey = "gender"),
    OracleQuestion("q3",
        oracleText = "The rings of the ancient tree...",
        oracleTextRu = "Кольца древнего дерева...",
        text = "What is your age?",
        textRu = "Сколько тебе лет?",
        type = QuestionType.NUMBER,
        dataKey = "age"),
    OracleQuestion("q4",
        oracleText = "The Seer measures your shadow...",
        oracleTextRu = "Провидец измеряет твою тень...",
        text = "What is your height (cm)?",
        textRu = "Какой у тебя рост (см)?",
        type = QuestionType.NUMBER,
        dataKey = "height"),
    OracleQuestion("q5",
        oracleText = "The earth feels your weight...",
        oracleTextRu = "Земля чувствует твой вес...",
        text = "What is your weight (kg)?",
        textRu = "Какой у тебя вес (кг)?",
        type = QuestionType.NUMBER,
        dataKey = "weight"),
    OracleQuestion("q6",
        oracleText = "The wind tests your endurance...",
        oracleTextRu = "Ветер испытывает твою выносливость...",
        text = "How would you describe your activity level?",
        textRu = "Как бы ты описал свой уровень активности?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("inactive", "Inactive (sedentary)",        "Неактивный (сидячий)",            "healer"),
            OracleOption("light",    "Light (1-2 days/week)",       "Лёгкий (1-2 дня/нед)",            "healer"),
            OracleOption("moderate", "Moderate (3-4 days/week)",    "Умеренный (3-4 дня/нед)",          "scout"),
            OracleOption("active",   "Active (5+ days/week)",       "Активный (5+ дней/нед)",          "warrior"),
            OracleOption("high",     "High (daily intense)",        "Высокий (ежедневно интенсивно)",   "warrior"),
        ), dataKey = "activity"),
    OracleQuestion("q7",
        oracleText = "The Seer sniffs the air around you...",
        oracleTextRu = "Провидец чувствует воздух вокруг тебя...",
        text = "Do you smoke or use tobacco?",
        textRu = "Ты куришь или употребляешь табак?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("no",      "No, never",                       "Нет, никогда",          "scout"),
            OracleOption("former",  "Former smoker",                   "Бывший курильщик",       ""),
            OracleOption("passive", "Exposed to secondhand smoke",     "Пассивное курение",      ""),
            OracleOption("yes",     "Yes, currently",                  "Да, сейчас курю",        "healer"),
        ), dataKey = "smoking"),
    OracleQuestion("q8",
        oracleText = "The Seer peers into your cup...",
        oracleTextRu = "Провидец заглядывает в твою чашу...",
        text = "How often do you consume alcohol?",
        textRu = "Как часто ты употребляешь алкоголь?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("never",   "Never",                            "Никогда",                 ""),
            OracleOption("rarely",  "Rarely (special occasions)",       "Редко (по особым случаям)", ""),
            OracleOption("weekend", "Weekends",                         "По выходным",             ""),
            OracleOption("daily",   "Daily",                            "Ежедневно",               ""),
        ), dataKey = "alcohol"),
    OracleQuestion("q9",
        oracleText = "The moon's cycle speaks to the Seer...",
        oracleTextRu = "Лунный цикл говорит с Провидцем...",
        text = "How is your menstrual cycle?",
        textRu = "Как твой менструальный цикл?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("regular",         "Regular",                    "Регулярный",                  ""),
            OracleOption("irregular",       "Irregular",                  "Нерегулярный",                ""),
            OracleOption("menopause",       "Menopause",                  "Менопауза",                   ""),
            OracleOption("pregnant",        "Pregnant/breastfeeding",     "Беременность/кормление",      ""),
            OracleOption("prefer_not_say",  "Prefer not to say",          "Предпочитаю не отвечать",     ""),
        ), dataKey = "menstrualCycle",
        conditional = { it.gender == "female" }),
    OracleQuestion("q10",
        oracleText = "The Seer reads the lines of your palm...",
        oracleTextRu = "Провидец читает линии твоей ладони...",
        text = "Do you have any chronic conditions?",
        textRu = "Есть ли у тебя хронические заболевания?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("none",         "None",                "Нет",                ""),
            OracleOption("diabetes1",    "Type 1 Diabetes",     "Диабет 1 типа",      ""),
            OracleOption("diabetes2",    "Type 2 Diabetes",     "Диабет 2 типа",      ""),
            OracleOption("prediabetes",  "Prediabetes",         "Преддиабет",         ""),
            OracleOption("highpressure", "High blood pressure", "Высокое давление",   ""),
            OracleOption("cholesterol",  "High cholesterol",    "Высокий холестерин", ""),
            OracleOption("heart",        "Heart disease",       "Болезни сердца",     ""),
            OracleOption("other",        "Other",               "Другое",             ""),
        ), dataKey = "chronicDiseases"),
    OracleQuestion("q11",
        oracleText = "The Seer listens to your body's whispers...",
        oracleTextRu = "Провидец слушает шёпот твоего тела...",
        text = "Do you experience any of these symptoms?",
        textRu = "Испытываешь ли ты какие-либо из этих симптомов?",
        type = QuestionType.MULTI,
        options = listOf(
            OracleOption("none",              "None",                   "Нет",                       ""),
            OracleOption("fatigue",           "Chronic fatigue",        "Хроническая усталость",     ""),
            OracleOption("muscle_weakness",   "Muscle weakness",        "Мышечная слабость",         ""),
            OracleOption("hair_loss",         "Hair loss",              "Выпадение волос",           ""),
            OracleOption("dry_skin",          "Dry skin",               "Сухая кожа",                ""),
            OracleOption("depression",        "Low mood / depression",  "Подавленность / депрессия", ""),
            OracleOption("numbness_tingling", "Numbness / tingling",    "Онемение / покалывание",    ""),
            OracleOption("bleeding_gums",     "Bleeding gums",          "Кровоточивость дёсен",      ""),
            OracleOption("pale_skin",         "Pale skin",              "Бледная кожа",              ""),
            OracleOption("low_immunity",      "Frequent illness",       "Частые болезни",            ""),
            OracleOption("concentration",     "Poor concentration",     "Плохая концентрация",       ""),
        ), dataKey = "symptoms"),
    OracleQuestion("q12",
        oracleText = "The night sky reveals your rest...",
        oracleTextRu = "Ночное небо раскрывает твой отдых...",
        text = "How many hours of sleep do you typically get?",
        textRu = "Сколько часов ты обычно спишь?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("less_5", "Less than 5",  "Менее 5",   "healer"),
            OracleOption("5_6",    "5-6 hours",    "5-6 часов", "healer"),
            OracleOption("6_7",    "6-7 hours",    "6-7 часов", ""),
            OracleOption("7_8",    "7-8 hours",    "7-8 часов", "scout"),
            OracleOption("more_8", "More than 8",  "Более 8",   "scout"),
        ), dataKey = "sleepHours"),
    OracleQuestion("q13",
        oracleText = "The Seer feels the depth of your rest...",
        oracleTextRu = "Провидец чувствует глубину твоего отдыха...",
        text = "How would you rate your sleep quality?",
        textRu = "Как бы ты оценил качество своего сна?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("poor",      "Poor",      "Плохое",            ""),
            OracleOption("fair",      "Fair",      "Удовлетворительное", ""),
            OracleOption("good",      "Good",      "Хорошее",           ""),
            OracleOption("excellent", "Excellent", "Отличное",          ""),
        ), dataKey = "sleepQuality"),
    OracleQuestion("q14",
        oracleText = "The Seer places a hand on your heart...",
        oracleTextRu = "Провидец кладёт руку на твоё сердце...",
        text = "Do you know your blood pressure level?",
        textRu = "Знаешь ли ты свой уровень давления?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("normal",     "Normal",       "Нормальное", ""),
            OracleOption("elevated",   "Elevated",     "Повышенное", ""),
            OracleOption("high",       "High",         "Высокое",    ""),
            OracleOption("dont_know",  "I don't know", "Не знаю",    ""),
        ), dataKey = "pressureLevel"),
    OracleQuestion("q15",
        oracleText = "The sweetness in your blood speaks...",
        oracleTextRu = "Сладость в твоей крови говорит...",
        text = "Have you been told your blood sugar is elevated?",
        textRu = "Говорили ли тебе, что сахар в крови повышен?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("no",         "No",            "Нет",     ""),
            OracleOption("yes",        "Yes",           "Да",      ""),
            OracleOption("dont_know",  "I don't know",  "Не знаю", ""),
        ), dataKey = "sugarLevel"),
    OracleQuestion("q16",
        oracleText = "The earth's bounty is measured...",
        oracleTextRu = "Щедрость земли измеряется...",
        text = "Do you eat fruits and vegetables regularly?",
        textRu = "Ешь ли ты фрукты и овощи регулярно?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("yes",       "Yes, daily",       "Да, ежедневно",       "healer"),
            OracleOption("sometimes", "Sometimes",        "Иногда",              ""),
            OracleOption("no",        "Rarely or never",  "Редко или никогда",   ""),
        ), dataKey = "eatsVegetables"),
    OracleQuestion("q17",
        oracleText = "The Seer examines your provisions...",
        oracleTextRu = "Провидец изучает твои припасы...",
        text = "Do you eat fast food or sugary drinks often?",
        textRu = "Часто ли ты ешь фаст-фуд или сладкие напитки?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("no",        "No, rarely",      "Нет, редко", ""),
            OracleOption("sometimes", "Sometimes",       "Иногда",     ""),
            OracleOption("yes",       "Yes, frequently", "Да, часто",  ""),
        ), dataKey = "eatsJunkFood"),
    OracleQuestion("q18",
        oracleText = "The Seer reads the bloodline...",
        oracleTextRu = "Провидец читает родословную...",
        text = "Family history of heart disease or stroke before age 60?",
        textRu = "Болезни сердца или инсульт в семье до 60 лет?",
        type = QuestionType.SINGLE,
        options = listOf(
            OracleOption("no",         "No",           "Нет",     ""),
            OracleOption("yes",        "Yes",          "Да",      ""),
            OracleOption("dont_know",  "I don't know", "Не знаю", ""),
        ), dataKey = "familyHistory"),
)

/**
 * v1.3.4: option ids re-aligned with the iOS questionnaire so this scoring
 * function consumes the keys the questionnaire UI actually emits. Earlier
 * Android versions had drifted (e.g. `current` smoking, NUMBER sleepHours,
 * `mood`/`headaches` symptoms) and the role assignment quietly defaulted.
 */
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
    // Activity (iOS ids: inactive / light / moderate / active / high)
    when (data.activity) {
        "active", "high" -> score("warrior", 2)
        "moderate" -> score("scout")
        "inactive" -> score("healer")
    }
    // Smoking (iOS ids: no / former / passive / yes)
    if (data.smoking == "yes") score("healer", 2)
    if (data.smoking == "no") score("warrior")
    // Sleep (iOS ids: less_5 / 5_6 / 6_7 / 7_8 / more_8)
    when (data.sleepHours) {
        "less_5", "5_6" -> score("healer", 2)
        "7_8", "more_8" -> score("warrior")
    }
    if (data.sleepQuality == "poor") score("healer")
    // Diet (iOS ids: yes / sometimes / no for vegetables; no / sometimes / yes for junk)
    when (data.eatsVegetables) {
        "yes" -> score("warrior")
        "no" -> score("healer")
    }
    when (data.eatsJunkFood) {
        "yes" -> score("healer", 2)
        "no" -> score("warrior")
    }
    // Chronic
    if (data.chronicDiseases != "none" && data.chronicDiseases.isNotEmpty()) score("healer")
    // Symptoms (iOS ids: depression for mood, no `headaches` key — fatigue / depression are the proxies)
    if (data.symptoms.contains("depression")) score("sage")
    if (data.symptoms.contains("fatigue") || data.symptoms.contains("low_immunity")) score("healer")

    return scores.maxByOrNull { it.value }?.key ?: "scout"
}
