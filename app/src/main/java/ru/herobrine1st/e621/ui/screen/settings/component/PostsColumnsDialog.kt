package ru.herobrine1st.e621.ui.screen.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.Preferences
import ru.herobrine1st.e621.ui.screen.posts.PostsScreenDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsColumnsDialog(
    getInitialState: () -> Preferences.PostsColumns,
    onApply: (Preferences.PostsColumns) -> Unit,
    onDismissRequest: () -> Unit
) {
    var preference by remember { mutableStateOf(getInitialState()) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()

    AlertDialog(
        icon = {
            Icon(Icons.Default.Science, contentDescription = null)
        },
        title = {
            Text(stringResource(R.string.settings_posts_columns_title))
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        onClick = {
                            (preference as? Preferences.PostsColumns.Adaptive)
                                ?.let { columnWidthToColumnCount(it.widthDp, screenWidthDp) }
                                ?.let { preference = Preferences.PostsColumns.Fixed(it.toInt()) }
                        },
                        selected = preference is Preferences.PostsColumns.Fixed
                    ) {
                        Text(stringResource(R.string.settings_posts_columns_dialog_mode_fixed))
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        onClick = {
                            (preference as? Preferences.PostsColumns.Fixed)
                                ?.let { columnCountToColumnWidth(it.columnCount.toFloat(), screenWidthDp) }
                                ?.let { preference = Preferences.PostsColumns.Adaptive(it) }
                        },
                        selected = preference is Preferences.PostsColumns.Adaptive
                    ) {
                        Text(stringResource(R.string.settings_posts_columns_dialog_mode_adaptive))
                    }
                }
                when (val preferenceCasted = preference) {
                    is Preferences.PostsColumns.Adaptive -> {
                        // This transforms a "continuous" number of columns to column width and back
                        // This is exact calculation (well, as exact as float) and non-linearity is compensated by showing exact width under the slider
                        val value = columnWidthToColumnCount(preferenceCasted.widthDp, screenWidthDp)
                        Slider(
                            value = value,
                            onValueChange = { value ->
                                preference = Preferences.PostsColumns.Adaptive(
                                    columnCountToColumnWidth(value, screenWidthDp)
                                )
                            },
                            valueRange = 1f..8.5f
                        )
                        Text(
                            stringResource(
                                R.string.settings_posts_columns_dialog_mode_adaptive_hint,
                                value.toInt(),
                                screenWidthDp.toInt(),
                                preferenceCasted.widthDp
                            )
                        )
                    }

                    is Preferences.PostsColumns.Fixed -> {
                        Slider(
                            value = preferenceCasted.columnCount.toFloat(),
                            onValueChange = {
                                preference = Preferences.PostsColumns.Fixed(it.toInt())
                            },
                            valueRange = 1f..8f,
                            steps = 8
                        )
                        Row {
                            Spacer(Modifier.weight(1f))
                            Text(
                                stringResource(
                                    R.string.settings_posts_columns_dialog_mode_fixed_hint,
                                    preferenceCasted.columnCount
                                )
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = {
                onApply(preference)
            }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = {
                onDismissRequest()
            }) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun columnWidthToColumnCount(columnWidthDp: Float, screenWidthDp: Float) =
    (screenWidthDp - PostsScreenDefaults.EDGE_PADDING.value) /
            (columnWidthDp + PostsScreenDefaults.HORIZONTAL_SPACING.value)

private fun columnCountToColumnWidth(columnCount: Float, screenWidthDp: Float) =
    (screenWidthDp - PostsScreenDefaults.EDGE_PADDING.value) / columnCount - PostsScreenDefaults.HORIZONTAL_SPACING.value
