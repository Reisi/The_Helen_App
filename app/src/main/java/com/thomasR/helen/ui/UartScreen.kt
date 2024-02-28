package com.thomasR.helen.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomasR.helen.R
import com.thomasR.helen.profile.uart.data.UARTEventHandled
import com.thomasR.helen.profile.uart.data.UARTReceive
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.repository.UartRepository
import com.thomasR.helen.ui.theme.HelenTheme

@Composable
fun UartScreen(
    modifier: Modifier = Modifier,
    userName: String,
    deviceName: String,
    repository: UartRepository,
    viewModel: UartViewModel = viewModel(factory = UartViewModel.Companion.Factory(repository))
) {
    var commandDialog by rememberSaveable { mutableStateOf(false) }
    var content by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val message by viewModel.receivedMessage.collectAsState(UARTEventHandled)

    if (message is UARTReceive) {
        val list = viewModel.receivedMessage.replayCache
        val start = list.lastIndexOf(UARTEventHandled) + 1
        for (i in start until list.size) {
            if (list[i] is UARTReceive) content += (list[i] as UARTReceive).msg
        }
        viewModel.messageHandled()
    }

    LaunchedEffect(scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Text(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(scrollState)
            .clickable { commandDialog = true },
        text = buildAnnotatedString {
            var i = 0
            val pattern = listOf("$userName@$deviceName:")
            do {
                val result = content.findAnyOf(pattern, i, false)
                val sizeTo = result?.first ?: content.length
                if (sizeTo != 0) {
                    append(content.substring(i, sizeTo))
                    i = sizeTo
                }
                if (result != null) {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                    ) {append(pattern[0])}
                    i += pattern[0].length
                }
            } while (i < content.length)
        },
        style = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.sp
        )
    )

    if (commandDialog) {
        val userColor = MaterialTheme.colorScheme.primary

        CommandDialog(onCancel = { commandDialog = false }) {
            commandDialog = false
            content += "$userName@$deviceName: $it\n"
            viewModel.sendMessage(it)
        }
    }
}


@Composable
fun CommandDialog(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onSend: (String) -> Unit
) {
    var command by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = modifier
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(id = R.string.enter_command))
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    TextButton(
                        onClick = { onSend(command) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(id = R.string.send))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun StatusPreviewBright() {
    HelenTheme(theme = Theme.BRIGHT) {
        UartScreen(userName = "root", deviceName = "Helen", repository = UartRepository())
    }
}