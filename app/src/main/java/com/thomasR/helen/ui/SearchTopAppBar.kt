package com.thomasR.helen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import com.thomasR.helen.R
import com.thomasR.helen.demo.DummyData
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.ui.theme.LocalHelenColorsPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar (
    modifier: Modifier = Modifier,
    dummyDevices: List<DummyData>,
    addDummyDevice: (DummyData) -> Unit,
    theme: Theme,
    changeTheme: (Theme) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    //var themeDialog by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.app_title)) },
        //colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalHelenColorsPalette.current.helen),
        navigationIcon = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.Menu, null)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    val menuStrings = stringArrayResource(R.array.main_menu).toList()
                    // demo mode
                    Text(menuStrings[0], Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)))
                    // TODO replace with a list of dummy devices
                    for (dummy in dummyDevices) {
                        DropdownMenuItem(
                            text = { Text(dummy.helenData.name!!) },
                            leadingIcon = { Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.helen_icon),   // TODO model specific icon
                                contentDescription = null,
                                //tint = MaterialTheme.colorScheme.onSurface
                            ) },
                            onClick = { menuExpanded = false; addDummyDevice(dummy) }
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = dimensionResource(id = R.dimen.padding_medium)))
                    // theme
                    Text(menuStrings[1], Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)))
                    val options = stringArrayResource(id = R.array.theme_options).toList()
                    for (i in options.indices) {
                        val icon = if (theme.ordinal == i) Icons.Default.RadioButtonChecked
                                   else Icons.Default.RadioButtonUnchecked
                        DropdownMenuItem(
                            text = { Text(options[i]) },
                            leadingIcon = { Icon(icon, null) },
                            onClick = { menuExpanded = false; changeTheme(Theme.encodeTheme(i) ?: Theme.SYSTEM_DEFAULT) })
                    }
                }
            }
        }
    )
}