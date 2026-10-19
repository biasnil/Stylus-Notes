package com.biasnil.stylusnotes.ui

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.MainViewModel
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.input.AndroidDeviceLookup
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.PenIdentity
import com.biasnil.stylusnotes.input.PenPolicy
import com.biasnil.stylusnotes.input.StylusFilter

private data class Probe(val toolType: Int, val identity: PenIdentity)

@Composable
private fun toolTypeName(t: Int): String = when (t) {
    MotionEvent.TOOL_TYPE_STYLUS -> stringResource(R.string.tooltype_stylus)
    MotionEvent.TOOL_TYPE_ERASER -> stringResource(R.string.tooltype_eraser)
    MotionEvent.TOOL_TYPE_FINGER -> stringResource(R.string.tooltype_finger)
    MotionEvent.TOOL_TYPE_MOUSE -> stringResource(R.string.tooltype_mouse)
    else -> stringResource(R.string.tooltype_unknown, t)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RegisterPenScreen(vm: MainViewModel) {
    val c = Palette
    val settings by vm.inputSettings.collectAsState()
    val lastKey by vm.lastKey.collectAsState()
    val lookup = remember { AndroidDeviceLookup() }

    var probe by remember { mutableStateOf<Probe?>(null) }
    var fingerDeviceId by remember { mutableStateOf<Int?>(null) }

    BackHandler { vm.closeRegisterPen() }

    Scaffold(
        containerColor = c.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pen_setup)) },
                navigationIcon = {
                    IconButton(onClick = vm::closeRegisterPen) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = c.topBar,
                    titleContentColor = c.onTopBar,
                    navigationIconContentColor = c.onTopBar,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // ------------------------------------------------------ settings
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.what_can_draw), style = MaterialTheme.typography.titleMedium)
                        RadioOptions(
                            options = InputMode.entries,
                            selected = settings.mode,
                            title = { inputModeTitle(it) },
                            detail = { inputModeDetail(it) },
                            onSelect = vm::setInputMode,
                        )
                        HorizontalDivider()
                        Text(stringResource(R.string.which_pens), style = MaterialTheme.typography.titleMedium)
                        RadioOptions(
                            options = PenPolicy.entries,
                            selected = settings.policy,
                            title = { penPolicyTitle(it) },
                            detail = { penPolicyDetail(it) },
                            onSelect = vm::setPenPolicy,
                        )
                    }
                }

                // ------------------------------------------------------ registered pens
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.your_pens, settings.pens.size), style = MaterialTheme.typography.titleMedium)
                        if (settings.pens.isEmpty()) Text(stringResource(R.string.no_pens_yet))
                        settings.pens.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.label.ifBlank { stringResource(R.string.tool_pen) }, style = MaterialTheme.typography.bodyLarge)
                                    Mono("${p.name.ifBlank { "—" }} · #${p.deviceId}")
                                }
                                OutlinedButton(onClick = { vm.removePen(p) }) { Text(stringResource(R.string.remove)) }
                            }
                        }
                        if (settings.policy == PenPolicy.ANY_STYLUS && settings.pens.isNotEmpty()) {
                            Text(stringResource(R.string.any_stylus_list_unused), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // ------------------------------------------------------ probe
                Text(stringResource(R.string.add_pen_instructions))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .pointerInteropFilter { ev ->
                            val masked = ev.actionMasked
                            if (masked == MotionEvent.ACTION_DOWN || masked == MotionEvent.ACTION_POINTER_DOWN) {
                                val i = ev.actionIndex
                                val tool = ev.getToolType(i)
                                val id = lookup.identityOf(ev.deviceId) ?: PenIdentity(ev.deviceId, "", "", 0, 0)
                                if (tool == MotionEvent.TOOL_TYPE_FINGER) fingerDeviceId = ev.deviceId else probe = Probe(tool, id)
                            }
                            true
                        },
                ) {
                    Text(stringResource(R.string.touch_here_with_pen), style = MaterialTheme.typography.titleMedium)
                }

                probe?.let { p ->
                    val id = p.identity
                    val known = settings.pens.firstOrNull { StylusFilter.samePen(it, id) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.detected, toolTypeName(p.toolType)), style = MaterialTheme.typography.titleMedium)
                            if (known != null) Text(stringResource(R.string.already_in_list, known.label))
                            Mono("Name:       ${id.name.ifBlank { "—" }}")
                            Mono("Device ID:  ${id.deviceId}")
                            Mono("Vendor:     0x%04X  Product: 0x%04X".format(id.vendorId, id.productId))
                            Mono("Descriptor: ${id.descriptor.take(24)}${if (id.descriptor.length > 24) "…" else ""}")
                        }
                    }
                    val isPen = p.toolType == MotionEvent.TOOL_TYPE_STYLUS || p.toolType == MotionEvent.TOOL_TYPE_ERASER
                    if (isPen) {
                        Button(onClick = { vm.registerPen(id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (known != null) stringResource(R.string.update_pen, known.label) else stringResource(R.string.add_this_pen))
                        }
                    }
                    if (isPen && fingerDeviceId == id.deviceId) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.same_device_warning), modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                // ------------------------------------------------------ side buttons
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.side_buttons), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.side_buttons_detail))
                        Mono(lastKey ?: stringResource(R.string.no_key_yet))
                    }
                }

                Button(onClick = vm::closeRegisterPen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

@Composable
private fun Mono(text: String) {
    Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
}
