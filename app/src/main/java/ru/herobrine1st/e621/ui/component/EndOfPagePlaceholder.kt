package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import ru.herobrine1st.e621.R

// edge of page, start and end of page or anything, it just doesn't matter while the name is clear
fun LazyListScope.endOfPagePlaceholder(loadState: LoadState) {
    when (loadState) {
        is LoadState.Loading -> {
            item {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        is LoadState.Error -> {
            item {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.unknown_error))
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        else -> {}
    }
}