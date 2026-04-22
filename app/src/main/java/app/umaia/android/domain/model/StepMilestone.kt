package app.umaia.android.domain.model

data class StepMilestone(
    val steps: Int,
    val nurBonus: Int,
    val nameEn: String,
    val nameRu: String,
    val nameKk: String,
    val descEn: String,
    val descRu: String,
    val descKk: String
)

val allMilestones: List<StepMilestone> = listOf(
    StepMilestone(2_000, 5,
        "Morning Walk", "Утренняя прогулка", "Таңғы дұға",
        "2,000 steps — a good start to the day.",
        "2 000 шагов — хорошее начало дня.",
        "Алғашқы қадамдарың ауылды жылытады."),
    StepMilestone(5_000, 12,
        "Active Day", "Активный день", "Дән себілді",
        "5,000 steps — you're keeping the tribe moving.",
        "5 000 шагов — племя движется вперёд.",
        "Дәндер руды тамақтандырады."),
    StepMilestone(10_000, 22,
        "Warrior Path", "Путь Воина", "Ұмай ананың батасы",
        "10,000 steps — the warrior's daily march.",
        "10 000 шагов — дневной марш воина.",
        "Ұмай ана сенің адалдығыңа күледі."),
    StepMilestone(20_000, 35,
        "Epic Journey", "Эпический путь", "Аңызға айналған жолаушы",
        "20,000 steps — an epic nomadic journey.",
        "20 000 шагов — эпическое кочевое путешествие.",
        "Дала сенің төзіміңе бас иеді."),
)
