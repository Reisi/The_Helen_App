package com.thomasR.helen.ui

import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thomasR.helen.R
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.ui.theme.HelenTheme

@Composable
fun ReloadUpload(
    modifier: Modifier = Modifier,
    onReloadClick: () -> Unit,
    onUploadClick: () -> Unit,
    uploadEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End //spacedBy(dimensionResource(id = R.dimen.padding_small))
    ) {
        TextButton(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
            onClick = onReloadClick
        ) {
            Text(
                text = stringResource(id = R.string.reload),
                textAlign = TextAlign.Center
            )
        }
        TextButton(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
            onClick = onUploadClick,
            enabled = uploadEnabled
        ) {
            Text(
                text = stringResource(id = R.string.upload),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CommonDialog(
    modifier: Modifier = Modifier,
    heading: String,
    onDismiss: () -> Unit,
    onCancel: @Composable () -> Unit,
    onConfirm: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card {

    }
}

@Preview(showBackground = true, device = "spec:width=300dp,height=550dp")//, showSystemUi = true)
@Composable
private fun TestDialog() {
    HelenTheme (theme = Theme.BRIGHT) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {  }) {
                    Text(text = "ok")
                }
            },
            title = { Text(text = "Title", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()),
                text = "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                    "this is going to be a long text" +
                "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text" +
                        "this is going to be a long text")}
        )
    }
}