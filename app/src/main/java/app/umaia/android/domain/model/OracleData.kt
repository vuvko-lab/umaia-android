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
    val recommendationRu: String
)

data class DeficiencyRisk(
    val nutrient: String,
    val nutrientRu: String,
    val description: String,
    val descriptionRu: String,
    val advice: String,
    val adviceRu: String
)

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
    if (bmi != null) {
        when {
            bmi >= 40 -> factors.add(RiskFactor("Severe obesity (BMI 40+)", "Тяжёлое ожирение (ИМТ 40+)", -10.0, "Oxford University", "Consult a doctor for a weight management plan", "Обратитесь к врачу для плана управления весом"))
            bmi >= 30 -> factors.add(RiskFactor("Obesity (BMI 30-40)", "Ожирение (ИМТ 30-40)", -3.0, "Oxford University", "Focus on gradual, sustainable weight loss", "Сосредоточьтесь на постепенном, устойчивом снижении веса"))
        }
    }

    // Smoking
    when (data.smoking) {
        "yes" -> factors.add(RiskFactor("Active smoking", "Активное курение", -10.0, "WHO, CDC", "Quitting smoking is the single best thing you can do for your health", "Бросить курить — лучшее, что вы можете сделать для здоровья"))
        "former" -> factors.add(RiskFactor("Former smoker", "Бывший курильщик", -3.0, "CDC", "Great that you quit! Risk decreases over time", "Отлично, что бросили! Риск снижается со временем"))
        "passive" -> factors.add(RiskFactor("Passive smoking exposure", "Пассивное курение", -2.0, "WHO", "Avoid secondhand smoke environments when possible", "По возможности избегайте мест с табачным дымом"))
    }

    // Activity
    when (data.activity) {
        "inactive" -> factors.add(RiskFactor("Physical inactivity", "Физическая неактивность", -6.0, "Aging-US", "Start with 15-minute daily walks", "Начните с 15-минутных ежедневных прогулок"))
        "light" -> factors.add(RiskFactor("Low activity level", "Низкий уровень активности", -2.0, "Aging-US", "Aim for 150 minutes of moderate activity per week", "Стремитесь к 150 минутам умеренной активности в неделю"))
        "high", "active" -> positive.add(RiskFactor("High physical activity", "Высокая физическая активность", 3.0, "AHA", "Keep up the great work!", "Продолжайте в том же духе!"))
    }

    // Alcohol
    when (data.alcohol) {
        "daily" -> factors.add(RiskFactor("Daily alcohol consumption", "Ежедневное употребление алкоголя", -5.0, "Lancet/GBD", "Reduce to occasional drinking or abstain", "Сократите до случайного употребления или откажитесь"))
        "weekend" -> factors.add(RiskFactor("Regular weekend drinking", "Регулярное употребление по выходным", -2.0, "Lancet/GBD", "Consider reducing frequency", "Рассмотрите снижение частоты"))
    }

    // Blood pressure
    if (data.pressureLevel == "high" || data.chronicDiseases == "highpressure") {
        factors.add(RiskFactor("Hypertension", "Гипертония", -4.4, "Aging-US", "Monitor blood pressure regularly; reduce salt intake", "Регулярно контролируйте давление; снизьте потребление соли"))
    } else if (data.pressureLevel == "elevated") {
        factors.add(RiskFactor("Elevated blood pressure", "Повышенное давление", -2.0, "Aging-US", "Lifestyle changes can prevent hypertension", "Изменения образа жизни могут предотвратить гипертонию"))
    }

    // Diabetes
    if (data.chronicDiseases == "diabetes1" || data.chronicDiseases == "diabetes2") {
        factors.add(RiskFactor("Diabetes", "Диабет", -8.9, "Aging-US", "Maintain strict blood sugar control with your doctor", "Строго контролируйте сахар в крови с врачом"))
    } else if (data.chronicDiseases == "prediabetes" || data.sugarLevel == "yes") {
        factors.add(RiskFactor("Prediabetes / elevated blood sugar", "Преддиабет / повышенный сахар", -3.0, "CDC", "Diet and exercise can reverse prediabetes", "Диета и упражнения могут обратить преддиабет"))
    }

    if (data.chronicDiseases == "cholesterol") {
        factors.add(RiskFactor("High cholesterol", "Высокий холестерин", -1.0, "Aging-US", "Increase fiber and omega-3 intake", "Увеличьте потребление клетчатки и омега-3"))
    }

    // Sleep
    if (data.sleepHours == "less_5" || data.sleepQuality == "poor") {
        factors.add(RiskFactor("Poor sleep", "Плохой сон", -4.0, "Mayo Clinic", "Prioritize sleep hygiene: consistent schedule, dark room, no screens", "Приоритизируйте гигиену сна: режим, тёмная комната, без экранов"))
    } else if ((data.sleepHours == "7_8" || data.sleepHours == "more_8") && (data.sleepQuality == "good" || data.sleepQuality == "excellent")) {
        positive.add(RiskFactor("Good sleep habits", "Хорошие привычки сна", 2.0, "Mayo Clinic", "Your sleep supports long-term health", "Ваш сон поддерживает долгосрочное здоровье"))
    }

    // Diet
    if (data.eatsVegetables == "yes" && data.eatsJunkFood == "no") {
        positive.add(RiskFactor("Healthy diet", "Здоровое питание", 2.0, "AHA", "Your diet is a strong foundation", "Ваше питание — прочный фундамент"))
    } else if (data.eatsJunkFood == "yes") {
        factors.add(RiskFactor("Unhealthy diet", "Нездоровое питание", -3.0, "AHA", "Replace processed foods with whole foods gradually", "Постепенно заменяйте обработанные продукты на натуральные"))
    }

    // Family history
    if (data.familyHistory == "yes") {
        factors.add(RiskFactor("Family history of cardiovascular disease", "Семейная история сердечно-сосудистых заболеваний", -2.0, "AHA", "Get regular check-ups and maintain a heart-healthy lifestyle", "Регулярно проходите обследования и ведите здоровый образ жизни"))
    }

    val totalImpact = factors.sumOf { it.impact } + positive.sumOf { it.impact }

    // Deficiencies
    val deficiencies = mutableListOf<DeficiencyRisk>()
    val symptomMap: Map<String, DeficiencyRisk> = mapOf(
        "fatigue" to DeficiencyRisk("Iron", "Железо", "Chronic fatigue can indicate iron deficiency", "Хроническая усталость может указывать на дефицит железа", "Red meat, spinach, lentils, fortified cereals", "Красное мясо, шпинат, чечевица, обогащённые каши"),
        "muscle_weakness" to DeficiencyRisk("Vitamin D", "Витамин D", "Muscle weakness linked to low vitamin D", "Мышечная слабость связана с низким уровнем витамина D", "Sunlight, fatty fish, fortified milk", "Солнечный свет, жирная рыба, обогащённое молоко"),
        "hair_loss" to DeficiencyRisk("Iron & Biotin", "Железо и биотин", "Hair loss may indicate iron or biotin deficiency", "Выпадение волос может указывать на дефицит железа или биотина", "Eggs, nuts, seeds, leafy greens", "Яйца, орехи, семена, листовая зелень"),
        "dry_skin" to DeficiencyRisk("Omega-3 & Vitamin E", "Омега-3 и витамин E", "Dry skin can signal essential fatty acid deficiency", "Сухая кожа может сигнализировать о дефиците жирных кислот", "Fatty fish, walnuts, flaxseed, avocado", "Жирная рыба, грецкие орехи, льняное семя, авокадо"),
        "depression" to DeficiencyRisk("Vitamin D & B12", "Витамин D и B12", "Low mood linked to vitamin D and B12 levels", "Пониженное настроение связано с уровнем витаминов D и B12", "Sunlight, fatty fish, fortified foods, eggs", "Солнечный свет, жирная рыба, обогащённые продукты, яйца"),
        "numbness_tingling" to DeficiencyRisk("Vitamin B12", "Витамин B12", "Numbness may indicate B12 deficiency", "Онемение может указывать на дефицит B12", "Meat, fish, dairy, fortified cereals", "Мясо, рыба, молочные продукты, обогащённые каши"),
        "bleeding_gums" to DeficiencyRisk("Vitamin C", "Витамин C", "Bleeding gums are a classic vitamin C sign", "Кровоточивость дёсен — классический признак дефицита витамина C", "Citrus fruits, bell peppers, strawberries", "Цитрусовые, болгарский перец, клубника"),
        "pale_skin" to DeficiencyRisk("Iron & B12", "Железо и B12", "Paleness can indicate anemia", "Бледность может указывать на анемию", "Red meat, beans, fortified cereals", "Красное мясо, бобы, обогащённые каши"),
        "low_immunity" to DeficiencyRisk("Vitamin C & Zinc", "Витамин C и цинк", "Frequent illness may signal immune nutrient gaps", "Частые болезни могут указывать на нехватку нутриентов для иммунитета", "Citrus, nuts, seeds, yogurt", "Цитрусовые, орехи, семена, йогурт"),
        "concentration" to DeficiencyRisk("Iron & B Vitamins", "Железо и витамины группы B", "Poor focus linked to iron and B vitamin levels", "Слабая концентрация связана с уровнем железа и витаминов B", "Whole grains, eggs, leafy greens, fish", "Цельнозерновые, яйца, листовая зелень, рыба")
    )
    val seenNutrients = mutableSetOf<String>()
    for (symptom in data.symptoms) {
        symptomMap[symptom]?.let { def ->
            if (seenNutrients.add(def.nutrient)) deficiencies.add(def)
        }
    }
    if ((data.activity == "inactive" || data.activity == "light") && "Vitamin D" !in seenNutrients) {
        deficiencies.add(DeficiencyRisk("Vitamin D", "Витамин D", "Low activity reduces sun exposure", "Низкая активность снижает пребывание на солнце", "Consider supplements, fatty fish, fortified foods", "Рассмотрите добавки, жирную рыбу, обогащённые продукты"))
    }
    if (data.gender == "female" && "Iron" !in seenNutrients) {
        deficiencies.add(DeficiencyRisk("Iron", "Железо", "Women are at higher risk for iron deficiency", "Женщины подвержены большему риску дефицита железа", "Red meat, spinach, lentils, vitamin C to aid absorption", "Красное мясо, шпинат, чечевица, витамин C для усвоения"))
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

fun tribalRoleEmoji(role: String): String = when (role) {
    "healer" -> "🌿"; "warrior", "guardian" -> "🛡️"; "shepherd", "builder" -> "⚒️"; "sage", "scout" -> "🔭"
    else -> "🔮"
}

fun tribalRoleTitle(role: String, ru: Boolean = false): String = if (ru) when (role) {
    "healer" -> "Целитель Степи"; "warrior", "guardian" -> "Страж Пути"
    "shepherd", "builder" -> "Строитель Очагов"; "sage", "scout" -> "Разведчик Ветров"
    else -> "Странник"
} else when (role) {
    "healer" -> "Healer of the Steppe"; "warrior", "guardian" -> "Guardian of the Path"
    "shepherd", "builder" -> "Builder of Hearths"; "sage", "scout" -> "Scout of the Winds"
    else -> "Wanderer"
}

fun tribalRoleFocus(role: String, ru: Boolean = false): String = if (ru) when (role) {
    "healer" -> "Травы, питание и задачи благополучия"
    "warrior", "guardian" -> "Выносливость, длинные прогулки и дозоры"
    "shepherd", "builder" -> "Строительство, сила и ремесло"
    "sage", "scout" -> "Исследования, разнообразие и открытия"
    else -> "Иди своим путём"
} else when (role) {
    "healer" -> "Herbs, nutrition, and wellbeing tasks"
    "warrior", "guardian" -> "Endurance, longer walks, and patrol tasks"
    "shepherd", "builder" -> "Construction, strength, and crafting tasks"
    "sage", "scout" -> "Exploration, variety, and discovery tasks"
    else -> "Walk your own path"
}

fun tribalRoleDescription(role: String, ru: Boolean = false): String = if (ru) when (role) {
    "healer" -> "Ты заботишься о здоровье и пропитании племени."
    "warrior", "guardian" -> "Ты защищаешь племя выносливостью и бдительностью."
    "shepherd", "builder" -> "Ты ставишь юрты и куёшь инструменты."
    "sage", "scout" -> "Ты исследуешь неизведанное и прокладываешь путь."
    else -> "Ты идёшь своим путём. Степь принимает всех."
} else when (role) {
    "healer" -> "You tend to the health and nourishment of the tribe."
    "warrior", "guardian" -> "You protect the tribe through endurance and vigilance."
    "shepherd", "builder" -> "You raise the yurts and forge the tools."
    "sage", "scout" -> "You explore the unknown and map the paths ahead."
    else -> "You walk your own path. The steppe welcomes all."
}

val tribalRoles: Map<String, RiskAssessmentResult> = mapOf(
    "warrior" to RiskAssessmentResult("warrior", "Warrior", "Воин",
        "You are strong and active. Your path is physical resilience and disciplined nutrition.",
        "Ты силён и активен. Твой путь — физическая стойкость и дисциплинированное питание.",
        "⚔️"),
    "sage" to RiskAssessmentResult("sage", "Sage", "Мудрец",
        "You seek knowledge and balance. Your path is mindful eating and stress management.",
        "Ты ищешь знания и баланс. Твой путь — осознанное питание и управление стрессом.",
        "📚"),
    "healer" to RiskAssessmentResult("healer", "Healer", "Целитель",
        "You prioritise recovery and sleep. Your path is rest, nourishment and inner calm.",
        "Ты ставишь в приоритет восстановление и сон. Твой путь — отдых, питание и внутренний покой.",
        "🌿"),
    "scout" to RiskAssessmentResult("scout", "Scout", "Разведчик",
        "You embrace balance and exploration. Your path is variety, movement and discovery.",
        "Ты принимаешь равновесие и исследование. Твой путь — разнообразие, движение и открытия.",
        "🏹"),
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
