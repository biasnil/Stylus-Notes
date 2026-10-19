package com.biasnil.stylusnotes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.PenPolicy

@Composable
fun inputModeTitle(m: InputMode): String = stringResource(
    when (m) {
        InputMode.PEN_ONLY -> R.string.mode_pen_only
        InputMode.PEN_AND_FINGER -> R.string.mode_pen_finger
        InputMode.AUTO -> R.string.mode_auto
    },
)

@Composable
fun inputModeDetail(m: InputMode): String = stringResource(
    when (m) {
        InputMode.PEN_ONLY -> R.string.mode_pen_only_detail
        InputMode.PEN_AND_FINGER -> R.string.mode_pen_finger_detail
        InputMode.AUTO -> R.string.mode_auto_detail
    },
)

@Composable
fun penPolicyTitle(p: PenPolicy): String = stringResource(
    when (p) {
        PenPolicy.REGISTERED_ONLY -> R.string.policy_registered
        PenPolicy.ANY_STYLUS -> R.string.policy_any
    },
)

@Composable
fun penPolicyDetail(p: PenPolicy): String = stringResource(
    when (p) {
        PenPolicy.REGISTERED_ONLY -> R.string.policy_registered_detail
        PenPolicy.ANY_STYLUS -> R.string.policy_any_detail
    },
)

/** Radio list for an enum-like setting. */
@Composable
fun <T> RadioOptions(
    options: List<T>,
    selected: T,
    title: @Composable (T) -> String,
    detail: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(title(option), style = MaterialTheme.typography.bodyLarge)
                    val d = detail(option)
                    if (d.isNotBlank()) Text(d, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
