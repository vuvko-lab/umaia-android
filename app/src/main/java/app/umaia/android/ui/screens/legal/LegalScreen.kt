package app.umaia.android.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.ui.theme.*

enum class LegalDocument { PRIVACY_POLICY, TERMS_OF_SERVICE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(document: LegalDocument, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TC.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TC.muted.copy(alpha = 0.3f)) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        when (document) {
                            LegalDocument.PRIVACY_POLICY -> "Privacy Policy\nПолитика конфиденциальности"
                            LegalDocument.TERMS_OF_SERVICE -> "Terms of Service\nДоговор публичной оферты"
                        },
                        color = GoldLight, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("✕", color = TC.muted.copy(alpha = 0.6f), fontSize = 18.sp)
                    }
                }
            }
            item {
                Text(
                    "Last updated: January 2026 / Последнее обновление: Январь 2026",
                    color = TC.text.copy(alpha = 0.4f), fontSize = 11.sp
                )
                HorizontalDivider(color = TC.text.copy(alpha = 0.1f), modifier = Modifier.padding(top = 8.dp))
            }

            when (document) {
                LegalDocument.PRIVACY_POLICY -> privacyPolicyItems()
                LegalDocument.TERMS_OF_SERVICE -> termsOfServiceItems()
            }

            // Company details
            item { HorizontalDivider(color = TC.text.copy(alpha = 0.1f)) }
            item {
                Text("Contact Us / Связаться с нами", color = GoldLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            item {
                LegalCaption("LLP \"Umaia\" / ТОО \"Umaia\"")
                LegalCaption("BIN: 240540021277 / БИН: 240540021277")
                LegalCaption("Republic of Kazakhstan, Almaty / Республика Казахстан, г. Алматы")
                LegalCaption("Email: info@umaia.tech")
                LegalCaption("Phone: +7 705 727 09 20 / Телефон: +7 705 727 09 20")
            }
        }
    }
}

// ── Privacy Policy ────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.privacyPolicyItems() {
    item { SectionHeader("1. Terms / 1. Термины") }
    item {
        BulletPoint("Website — umaia.tech and the UMAIA mobile application. All exclusive rights belong to the Operator.\nСайт — umaia.tech и мобильное приложение UMAIA. Все исключительные права принадлежат Оператору.")
        BulletPoint("User — a person using the Website or the Application.\nПользователь — лицо, использующее Сайт или Приложение.")
        BulletPoint("Personal Data — data the User provides during registration or while using the service.\nПерсональные данные — данные, которые Пользователь предоставляет при регистрации или в процессе использования сервиса.")
    }
    item { SectionHeader("2. Collection and Processing of Personal Data\n2. Сбор и обработка персональных данных") }
    item {
        LegalParagraph("The Operator collects and stores only those Personal Data that are necessary for the provision of Services and interaction with the User.\nОператор собирает и хранит только те Персональные данные, которые необходимы для оказания Услуг и взаимодействия с Пользователем.")
        LegalParagraph("Purposes of data processing / Цели обработки данных:")
        BulletPoint("Provision of Services to the User / Оказание Услуг Пользователю")
        BulletPoint("User identification / Идентификация Пользователя")
        BulletPoint("Interaction with the User / Взаимодействие с Пользователем")
        BulletPoint("Conducting statistical and other research / Проведение статистических и иных исследований")
        LegalParagraph("Processed data / Обрабатываемые данные:")
        BulletPoint("Full name / Фамилия, имя и отчество")
        BulletPoint("Email address / Адрес электронной почты")
        BulletPoint("Phone number / Номер телефона")
    }
    item { SectionHeader("3. Processing Procedure / 3. Порядок обработки данных") }
    item {
        LegalParagraph("The Operator undertakes to use Personal Data in accordance with the Law of the Republic of Kazakhstan \"On Personal Data and Their Protection\" dated May 21, 2013 No. 94-V.\nОператор обязуется использовать Персональные данные в соответствии с Законом Республики Казахстан «О персональных данных и их защите» от 21 мая 2013 года № 94-V.")
        LegalParagraph("Personal Data and other User Data remain confidential, except in cases where the specified data is publicly available.\nВ отношении Персональных данных и иных Данных Пользователя сохраняется их конфиденциальность, кроме случаев, когда указанные данные являются общедоступными.")
        LegalParagraph("The Operator has the right to store Personal Data on servers outside the territory of the Republic of Kazakhstan.\nОператор имеет право хранить Персональные данные на серверах вне территории Республики Казахстан.")
        LegalParagraph("The Operator has the right to transfer Personal Data without the consent of the User to state bodies upon their reasoned request, as well as in other cases provided for by the legislation of the Republic of Kazakhstan.\nОператор имеет право передавать Персональные данные без согласия Пользователя государственным органам по их мотивированному запросу, а также в иных случаях, предусмотренных законодательством РК.")
    }
    item { SectionHeader("4. Protection of Personal Data\n4. Защита персональных данных") }
    item {
        LegalParagraph("The Operator provides proper protection of Personal and other data in accordance with the Legislation and takes necessary and sufficient organizational and technical measures to protect Personal Data from unauthorized or accidental access, destruction, modification, blocking, copying, distribution, as well as from other illegal actions by third parties.\nОператор осуществляет надлежащую защиту Персональных и иных данных в соответствии с Законодательством и принимает необходимые и достаточные организационные и технические меры для защиты Персональных данных от неправомерного или случайного доступа, уничтожения, изменения, блокирования, копирования, распространения, а также от иных неправомерных действий с ними третьих лиц.")
    }
    item { SectionHeader("5. Other Provisions / 5. Иные положения") }
    item {
        LegalParagraph("The law of the Republic of Kazakhstan shall apply to this Privacy Policy. All possible disputes are subject to resolution at the place of registration of the Operator. The response time for a claim is 30 business days.\nК настоящей Политике конфиденциальности подлежит применению право Республики Казахстан. Все возможные споры подлежат разрешению по месту регистрации Оператора. Срок ответа на претензию — 30 рабочих дней.")
        LegalParagraph("The Operator has the right at any time to change the Privacy Policy unilaterally. All changes come into force from the moment they are posted on the Website.\nОператор имеет право в любой момент изменять Политику конфиденциальности в одностороннем порядке. Все изменения вступают в силу с момента размещения на Сайте.")
    }
}

// ── Terms of Service ──────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.termsOfServiceItems() {
    item {
        LegalParagraph("This agreement is concluded in accordance with paragraph 5 of Article 395 of the Civil Code of the Republic of Kazakhstan.\nНастоящий договор заключён в соответствии с пунктом 5 статьи 395 Гражданского кодекса Республики Казахстан.")
        LegalParagraph("The text of the Agreement is an offer (public offer) to use the online service umaia.tech and the UMAIA mobile application, access to which provides the opportunity to receive services and use information resources of LLP \"Umaia\" (hereinafter referred to as the Administrator).\nТекст Договора является предложением (публичной офертой) на использование онлайн сервиса umaia.tech и мобильного приложения UMAIA, доступ к которому предоставляет возможность получения услуг и пользования информационными ресурсами ТОО \"Umaia\" (далее – Администратор).")
    }
    item { SectionHeader("1. General Provisions / 1. Основные положения") }
    item {
        BulletPoint("\"Offer\" — public proposal to use the online service and mobile application.\n«Оферта» — публичное предложение на использование онлайн сервиса и мобильного приложения.")
        BulletPoint("\"Acceptance\" — unconditional acceptance by the User of the terms of the agreement in full.\n«Акцепт» — безоговорочное принятие Пользователем условий договора в полном объеме.")
        BulletPoint("\"Administrator\" — service provider who owns the website and application (LLP \"Umaia\").\n«Администратор» — сервис-провайдер, являющийся собственником сайта и приложения (ТОО \"Umaia\").")
        BulletPoint("\"User\" — any individual or legal entity that accepts the terms and uses the services.\n«Пользователь» — любое физическое/юридическое лицо, которое принимает условия договора и пользуется услугами.")
        BulletPoint("\"Services\" — AI-powered personalized health analysis services.\n«Услуги» — услуги персонализированного анализа здоровья на основе искусственного интеллекта.")
    }
    item { SectionHeader("2. Subject of the Offer / 2. Предмет оферты") }
    item {
        LegalParagraph("The Administrator provides personalized health analysis services via the Website and Application. The Administrator undertakes to provide technical maintenance and support.\nАдминистратор предоставляет услуги по персонализированному анализу здоровья через Сайт и Приложение. Администратор обязуется оказывать техническое обслуживание и поддержку.")
    }
    item { SectionHeader("3. Using the Service / 3. Использование сервиса") }
    item {
        LegalParagraph("To receive the Administrator's service, the User selects a tariff plan at their discretion, registers by providing personal data, and makes payment. User registration means unconditional and full agreement with the terms of the Agreement.\nДля получения услуги Администратора Пользователь по своему желанию выбирает тарифный план, проводит регистрацию путем предоставления персональных данных и производит оплату. Регистрация Пользователя означает безоговорочное и полное согласие с условиями Договора.")
    }
    item { SectionHeader("4. Registration and Data Protection\n4. Регистрация и защита персональных данных") }
    item {
        LegalParagraph("The Administrator undertakes not to disclose information received from the User.\nАдминистратор обязуется не разглашать полученную от Пользователя информацию.")
    }
    item { SectionHeader("5. Rights and Obligations / 5. Права и обязанности сторон") }
    item {
        LegalParagraph("Administrator / Администратор:")
        BulletPoint("Undertakes to provide technical support and full information within the package of Services.\nОбязуется оказывать техническую поддержку и предоставлять полную информацию в рамках пакетных Услуг.")
        BulletPoint("Undertakes not to disclose the User's personal data.\nОбязуется не разглашать персональные данные Пользователя.")
        BulletPoint("Has the right to unilaterally change the terms of service provision.\nИмеет право в одностороннем порядке изменять условия предоставления Услуг.")
        LegalParagraph("User / Пользователь:")
        BulletPoint("Bears full responsibility for the accuracy of information entered during registration.\nНесет полную ответственность за правильность информации, введенной при регистрации.")
        BulletPoint("Bears personal responsibility for any actions performed using their personal account.\nНесет личную ответственность за любые действия, совершенные с использованием своего личного кабинета.")
    }
    item { SectionHeader("6. Payment Procedure / 6. Порядок оплаты") }
    item {
        BulletPoint("Payment is made by bank cards or other non-cash methods.\nОплата производится банковскими картами или иными безналичными способами.")
        BulletPoint("Services are provided subject to 100% prepayment.\nУслуги предоставляются при условии 100% предоплаты.")
    }
    item { SectionHeader("7. Refund Policy / 7. Политика возврата денежных средств") }
    item {
        BulletPoint("Deadline: 14 calendar days from the date of receiving access.\nСрок подачи заявления: 14 календарных дней с даты получения доступа.")
        BulletPoint("Procedure: Written application to info@umaia.tech with reasons.\nПорядок подачи: Письменное заявление на info@umaia.tech с объяснением причин.")
        BulletPoint("Amount: Minus the amount for actually rendered Services.\nСумма возврата: За минусом суммы за фактически оказанные Услуги.")
        BulletPoint("Timing: Within 14 calendar days.\nСроки возврата: В течение 14 календарных дней.")
    }
    item { SectionHeader("8. Liability / 8. Ответственность сторон") }
    item {
        LegalParagraph("For non-fulfillment or improper fulfillment of obligations, the Parties are liable in accordance with the current legislation of the Republic of Kazakhstan.\nЗа неисполнение или ненадлежащее исполнение обязательств Стороны несут ответственность в соответствии с действующим законодательством РК.")
    }
    item { SectionHeader("9. Health & Exercise Disclaimer\n9. Отказ от ответственности за физические упражнения") }
    item {
        LegalParagraph("The information and recommendations provided by UMAIA, including but not limited to health tips, exercise suggestions, and wellness guidance, are for informational purposes only and do not constitute medical advice. Umaia assumes no responsibility or liability for any actions you take based on the recommendations provided within the application. All health-related recommendations should be reviewed and approved by a qualified healthcare professional before being followed. Please consult your physician before beginning any exercise program or making changes to your health routine. Use of this application and its recommendations is entirely at your own risk. Umaia is not responsible for any injury, illness, loss, or damage that may occur as a result of following these recommendations or exercises. Always exercise within your limits and use proper form and safety precautions.\n\nИнформация и рекомендации, предоставляемые UMAIA, включая, но не ограничиваясь, советы по здоровью, рекомендации по упражнениям и рекомендации по оздоровлению, носят исключительно информационный характер и не являются медицинскими рекомендациями. Umaia не несёт никакой ответственности за любые действия, предпринятые вами на основании рекомендаций, предоставленных в приложении. Все рекомендации, связанные со здоровьем, должны быть проверены и одобрены квалифицированным медицинским специалистом, прежде чем им следовать. Пожалуйста, проконсультируйтесь с врачом перед началом любой программы упражнений или изменением режима здоровья. Использование данного приложения и его рекомендаций осуществляется исключительно на ваш собственный риск. Umaia не несёт ответственности за любые травмы, заболевания, убытки или ущерб, которые могут возникнуть в результате следования данным рекомендациям или упражнениям.")
    }
    item { SectionHeader("10. Other Conditions / 10. Прочие условия") }
    item {
        LegalParagraph("The Administrator has the right to unilaterally change the terms of the Agreement. The Parties are released from liability during force majeure events. In all other matters the Parties are guided by the current legislation of the Republic of Kazakhstan.\nАдминистратор имеет право в одностороннем порядке изменить условия Договора. Стороны освобождаются от ответственности на время действий непреодолимой силы. Во всем остальном Стороны руководствуются действующим законодательством РК.")
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
fun SectionHeader(text: String) {
    Text(text, color = TC.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
         modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun LegalParagraph(text: String) {
    Text(text, color = TC.text.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
fun BulletPoint(text: String) {
    Row(Modifier.padding(start = 4.dp)) {
        Text("• ", color = Gold.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(text, color = TC.text.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun LegalCaption(text: String) {
    Text(text, color = TC.text.copy(alpha = 0.5f), fontSize = 11.sp)
}
