package com.thomasR.helen.ui

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.helenProject.data.HPSHelenModeConfig
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.profile.kd2.data.KD2ChannelFeature
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.ui.theme.HelenTheme
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun ConfigScreen(
    modifier: Modifier = Modifier,
    helenViewModel: HelenViewModel,
    channelsConfigView: ChannelsConfigView = DefaultChannelsConfigView()
) {
    var selectedMode by remember { mutableStateOf<Int?>(null) }
    val helenData by helenViewModel.data.collectAsState()
    val projectData by helenViewModel.hpsData.collectAsState()

    val modeConfig = projectData.modes
    val features = projectData.feature

    if (modeConfig == null || features == null)
        return  // TODO: display loading animation?

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            val modeConfig = projectData.modes  // TODO check if this is necessary
            //Log.d("reorder", "from ${from.index} to ${to.index}")
            if (modeConfig == null || from.index % 2 != 0)
                return@rememberReorderableLazyListState

            // make a copy of the old mode list
            val newModeList: MutableList<MutableList<HPSModeConfig>> = emptyList<MutableList<HPSModeConfig>>().toMutableList()
            for (group in modeConfig) {
                newModeList.add(group.toMutableList())
            }

            // now search the two groups the dragged divider is separating
            val position = from.index / 2   // represents the mode which is below the dragged group devider
            var searchPosition = 0
            var groupIndexBelow = 0         // the index of the lower group

            for (i in 0 until newModeList.size) {
                if (searchPosition + newModeList[groupIndexBelow].size > position) break
                searchPosition += newModeList[groupIndexBelow].size
                groupIndexBelow++
            }

            // rearrange modes
            if (groupIndexBelow == 0) {                         // top divider has been dragged -> generate new group
                newModeList.add(0, mutableListOf(newModeList.first().first()))
                newModeList[1].removeFirst()
            } else if (groupIndexBelow == newModeList.size) {   // lower divider has been dragged -> generate new group
                newModeList.add(mutableListOf(newModeList.last().last()))
                newModeList[newModeList.size - 2].removeLast()
            } else {                                            // middle divider has been dragged -> rearrange groups
                if (from.index > to.index) {                    // dragged up -> move last mode from above to lower group
                    newModeList[groupIndexBelow].add(0, newModeList[groupIndexBelow - 1].last())
                    newModeList[groupIndexBelow - 1].removeLast()
                    if (newModeList[groupIndexBelow - 1].size == 0) newModeList.removeAt(groupIndexBelow - 1)
                } else {                                        // dragged down -> move first from lower to end of upper
                    newModeList[groupIndexBelow - 1].add(newModeList[groupIndexBelow].first())
                    newModeList[groupIndexBelow].removeFirst()
                    if (newModeList[groupIndexBelow].size == 0) newModeList.removeAt(groupIndexBelow)
                }
            }

            helenViewModel.updateGroups(newModeList)
        }
    )

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .reorderable(state),
            verticalArrangement = Arrangement.spacedBy((-16).dp)
        ) {
            var itemNo = 0  // TODO necessary?

            item {
                ReorderableItem(state, key = itemNo) { isDragging ->
                    val elevation = animateDpAsState(targetValue = if (isDragging) 16.dp else 0.dp)
                    TopGroupDivider(
                        modifier = Modifier
                            .detectReorderAfterLongPress(state)
                            .shadow(elevation.value)
                    )
                }
                itemNo++
            }

            for (groupIndex in modeConfig.indices) {
                val group = modeConfig[groupIndex]
                for (modeIndex in group.indices) {
                    val mode = group[modeIndex]

                    // calculate the mode number
                    var modeNo = modeIndex
                    for (i in 0 until groupIndex) {
                        modeNo += modeConfig[i].size
                    }

                    // add mode item
                    item {
                        ModeListItem(
                            modeNo = modeNo,
                            profile = helenData.setupProfile,
                            config = mode,
                            channelSize = features.channelSize,
                            channelView = {profile, config, size -> channelsConfigView.ChannelsView(profile, config, size, Modifier)},
                            onModeClick = {
                                selectedMode = modeNo
                            }
                        )
                    }
                    itemNo++

                    // add divider, either ModeDivider (aka invisible spacer) or GroupDivider
                    if (groupIndex != modeConfig.size - 1 || modeIndex != group.size - 1) {
                        if (modeIndex == group.size - 1) {
                            item {
                                ReorderableItem(state, key = itemNo) { isDragging ->
                                    val elevation =
                                        animateDpAsState(targetValue = if (isDragging) 16.dp else 0.dp)
                                    GroupDivider(
                                        modifier = Modifier
                                            .detectReorderAfterLongPress(state)
                                            .shadow(elevation.value)
                                    )
                                }
                            }
                        } else {
                            item { ModeDivider() }
                        }
                        itemNo++
                    }
                }
            }

            item {
                ReorderableItem(state, key = itemNo) { isDragging ->
                    val elevation = animateDpAsState(targetValue = if (isDragging) 16.dp else 0.dp)
                    BottomGroupDivider(
                        modifier = Modifier
                            .detectReorderAfterLongPress(state)
                            .shadow(elevation.value)
                    )
                }
                itemNo++
            }

            item {
                ReloadUpload(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .background(MaterialTheme.colorScheme.background),
                    onReloadClick = { helenViewModel.reloadModes() },
                    onUploadClick = {helenViewModel.writeModes(modeConfig)}
                )
            }

            item { BottomSpacer() }
        }
    }

    if (selectedMode != null) {
        var modeIndex = selectedMode!!
        var modeCfg: HPSModeConfig? = null
        for (group in modeConfig) {
            if (modeIndex < group.size) {
                modeCfg = group[modeIndex]
                break
            }
            else {
                modeIndex -= group.size
            }
        }
        if (modeCfg != null) {
            var modeOverride: ((List<HPSChannelConfig>?) -> Unit)? = { helenViewModel.overrideMode(it) }
            if (projectData.feature?.feature?.modeOverrideSupported != true) modeOverride = null

            ConfigDialog(
                modeNo = selectedMode!!,
                config = modeCfg,
                channelSize = features.channelSize,
                profile = helenData.setupProfile,
                channelsConfig = channelsConfigView,
                onChannelsChanged = modeOverride,
                controlPointEnabled = projectData.isControlPointEnabled == true,
                onCancel = { selectedMode = null },
                onDone = {
                    if (selectedMode != null) {
                        helenViewModel.updateMode(selectedMode!!, it)
                    }
                    selectedMode = null
                }
            )
        }
    }
}

@Composable
private fun TopGroupDivider(
    modifier: Modifier = Modifier
) {
    val backGroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(40.dp)) {
        val path = Path()
        path.moveTo(0.dp.toPx(), 0.dp.toPx())
        path.lineTo(size.width, 0.dp.toPx())
        path.lineTo(size.width, 40.dp.toPx())
        path.arcTo(Rect(Offset(size.width - 24.dp.toPx(), 40.dp.toPx()), 24.dp.toPx()), 0f, -90f, false)
        path.arcTo(Rect(Offset(24.dp.toPx(), 40.dp.toPx()), 24.dp.toPx()), 270f, -90f, false)
        path.lineTo(0f, 40.dp.toPx())
        path.close()
        drawPath(path, backGroundColor, style = Fill)
    }
}

@Composable
private fun GroupDivider(
    modifier: Modifier = Modifier
) {
    val backGroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(64.dp)) {
        val path = Path()
        path.moveTo(0.dp.toPx(), 0.dp.toPx())
        path.arcTo(Rect(Offset(24.dp.toPx(), 0.dp.toPx()), 24.dp.toPx()), 180f, -90f, false)
        path.arcTo(Rect(Offset(size.width - 24.dp.toPx(), 0.dp.toPx()), 24.dp.toPx()), 90f, -90f, false)
        path.lineTo(size.width, 0f)
        path.lineTo(size.width, 64.dp.toPx())
        path.arcTo(Rect(Offset(size.width - 24.dp.toPx(), 64.dp.toPx()), 24.dp.toPx()), 0f, -90f, false)
        path.arcTo(Rect(Offset(24.dp.toPx(), 64.dp.toPx()), 24.dp.toPx()), 270f, -90f, false)
        path.lineTo(0f, 64.dp.toPx())
        path.close()
        drawPath(path, backGroundColor, style = Fill)
    }
}

@Composable
private fun ModeDivider(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(40.dp))
}

@Composable
private fun BottomGroupDivider(
    modifier: Modifier = Modifier
) {
    val backGroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(40.dp)) {
        val path = Path()
        path.moveTo(0.dp.toPx(), 0.dp.toPx())
        path.arcTo(Rect(Offset(24.dp.toPx(), 0.dp.toPx()), 24.dp.toPx()), 180f, -90f, false)
        path.arcTo(Rect(Offset(size.width - 24.dp.toPx(), 0.dp.toPx()), 24.dp.toPx()), 90f, -90f, false)
        path.lineTo(size.width, 0f)
        path.lineTo(size.width, 40.dp.toPx())
        path.lineTo(0f, 40.dp.toPx())
        path.close()
        drawPath(path, backGroundColor, style = Fill)
    }
}

@Composable
private fun BottomSpacer(
    modifier: Modifier = Modifier
) {
    val backGroundColor = MaterialTheme.colorScheme.background
    val height = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(height)) {
        val path = Path()
        path.moveTo(0.dp.toPx(), 16.dp.toPx())
        path.lineTo(size.width, 16.dp.toPx())
        path.lineTo(size.width, size.height)
        path.lineTo(0.dp.toPx(), size.height)
        path.close()
        drawPath(path, backGroundColor, style = Fill)
    }
}

/*data class ChannelInfo (
    val intensity: Int,
    val type: HPSFeatureChannelDescription
)*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeListItem(
    modifier: Modifier = Modifier,
    modeNo: Int,
    profile: Int?,
    config: HPSModeConfig,
    channelSize: List<HPSFeatureChannelSize>,
    channelView: @Composable() ((profile: Int?, List<HPSChannelConfig>, List<HPSFeatureChannelSize>) -> Unit),
    onModeClick: () -> Unit
) {
    val preferred = config.helen.preferred
    val ignored = config.helen.ignored
    val temporary = config.helen.temporary
    val off = config.helen.off
    var description = ""

    if (!preferred && !temporary && !off) description = (modeNo + 1).toString()
    else {
        if (preferred) description += "P"
        if (temporary) description += "T"
        if (off) description += "O"
    }

    /*var enabled = false
    for (channel in config.channel) {
        if (channel.intensity != 0) {
            enabled = true
            break
        }
    }*/

    val color = if (/*enabled &&*/ !ignored) CardDefaults.outlinedCardColors() else CardDefaults.cardColors()
    //val border = CardDefaults.outlinedCardBorder(enabled && !ignored)

    Card(
        onClick = { onModeClick() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_small)),
        colors = color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                modifier = Modifier.padding(16.dp),
                label = description,
            )
            channelView(profile, config.channel, channelSize)
        }
    }
}

@Composable
private fun Avatar(
    modifier: Modifier = Modifier,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color)
        }
        if (label != null) {
            Row {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun StatusPreviewBright() {
    HelenTheme (theme = Theme.DARK) {

        ModeListItem(
            modeNo = 1,
            config = HPSModeConfig(
                helen = HPSHelenModeConfig(false, false, false, false),
                channel = listOf(
                    HPSChannelConfig(10, 0),
                    HPSChannelConfig(5, 0)
                )
            ),
            channelSize = listOf(
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.PWM)
            ),
            profile = 0,//null,
            channelView = { profile, config, size ->
                KD21ChannelsConfigView(features = KD2ChannelFeature(true)).ChannelsView(profile, config, size, Modifier)
            },
            onModeClick = {})
    }
}