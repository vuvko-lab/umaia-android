package app.umaia.android.ui.screens.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.umaia.android.data.analytics.AnalyticsService
import app.umaia.android.data.auth.AuthService
import app.umaia.android.domain.repository.RewardClaim
import app.umaia.android.domain.repository.RewardClaimSubmission
import app.umaia.android.domain.repository.RewardDeliveryMethod
import app.umaia.android.domain.repository.RewardRepository
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.Gold
import app.umaia.android.ui.theme.NightBlue
import app.umaia.android.ui.theme.TC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val SIZES = listOf("XS", "S", "M", "L", "XL", "XXL")

data class RewardClaimUiState(
    val isSubmitting: Boolean = false,
    val claim: RewardClaim? = null,
    val error: String? = null,
)

@HiltViewModel
class RewardClaimViewModel @Inject constructor(
    private val rewardRepository: RewardRepository,
    private val analytics: AnalyticsService,
    val authService: AuthService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardClaimUiState())
    val uiState: StateFlow<RewardClaimUiState> = _uiState.asStateFlow()

    fun submit(submission: RewardClaimSubmission) {
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching { rewardRepository.submitClaim(submission) }
                .onSuccess { claim ->
                    analytics.rewardClaimSubmitted(submission.rewardId, submission.periodId, submission.deliveryMethod.wire)
                    _uiState.update { it.copy(isSubmitting = false, claim = claim) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Submit failed") }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardClaimSheet(
    rewardId: String,
    periodId: String,
    prefilledFullName: String?,
    onDismiss: () -> Unit,
    onSubmitted: (RewardClaim) -> Unit,
    viewModel: RewardClaimViewModel = hiltViewModel(),
) {
    val s = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var fullName by remember { mutableStateOf(prefilledFullName.orEmpty()) }
    var phone by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("M") }
    var pickup by remember { mutableStateOf(true) }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val canSubmit = fullName.isNotBlank() && phone.isNotBlank() && city.isNotBlank() &&
        (pickup || address.isNotBlank()) && !state.isSubmitting

    LaunchedEffect(state.claim) { state.claim?.let { onSubmitted(it) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TC.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TC.muted.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(s.rewardClaimTitle, color = TC.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            ClaimTextField(value = fullName, onChange = { fullName = it }, label = s.rewardClaimFullName)
            ClaimTextField(
                value = phone, onChange = { phone = it }, label = s.rewardClaimPhone,
                keyboard = KeyboardType.Phone
            )

            Text(s.rewardClaimSize, color = TC.muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SIZES.forEach { sz ->
                    val selected = sz == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Gold else TC.card,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { size = sz }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            sz,
                            color = if (selected) NightBlue else TC.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            DeliveryMethodPicker(pickup = pickup, onChange = { pickup = it })

            ClaimTextField(value = city, onChange = { city = it }, label = s.rewardClaimCity)

            if (!pickup) {
                ClaimTextField(value = address, onChange = { address = it }, label = s.rewardClaimAddress)
            }

            ClaimTextField(
                value = notes, onChange = { notes = it }, label = s.rewardClaimNotes,
                singleLine = false, minLines = 2
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.submit(
                        RewardClaimSubmission(
                            rewardId = rewardId,
                            periodId = periodId,
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            size = size,
                            deliveryMethod = if (pickup) RewardDeliveryMethod.PICKUP else RewardDeliveryMethod.SHIP,
                            city = city.trim(),
                            address = address.trim().takeIf { it.isNotBlank() },
                            notes = notes.trim().takeIf { it.isNotBlank() },
                        )
                    )
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = NightBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(s.rewardClaimSubmit, color = NightBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ClaimTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = TC.muted.copy(alpha = 0.5f),
            focusedLabelColor = Gold,
            unfocusedLabelColor = TC.muted,
            focusedTextColor = TC.text,
            unfocusedTextColor = TC.text,
            cursorColor = Gold
        )
    )
}

@Composable
private fun DeliveryMethodPicker(pickup: Boolean, onChange: (Boolean) -> Unit) {
    val s = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DeliveryOption(label = s.rewardClaimDeliveryPickup, selected = pickup) { onChange(true) }
        DeliveryOption(label = s.rewardClaimDeliveryShip, selected = !pickup) { onChange(false) }
    }
}

@Composable
private fun RowScope.DeliveryOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (selected) Gold.copy(alpha = 0.18f) else TC.card, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) Gold else TC.muted.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Gold else TC.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
