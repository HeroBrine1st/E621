package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.MessageQuote
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.ui.theme.disabledText

@Composable
fun RenderBB(text: String, modifier: Modifier = Modifier) {
    val parsed = remember(text) { parseBBCode(text) }
    RenderBB(parsed, modifier)
}

@Composable
fun RenderBB(data: List<MessageData<*>>, modifier: Modifier = Modifier) {
    Column(modifier) {
        data.forEach {
            RenderBB(it)
        }
    }
}

@Composable
fun RenderBB(data: MessageData<*>, modifier: Modifier = Modifier) {
    when (data) {
        is MessageQuote -> {
            Text(stringResource(R.string.quote_comments, data.userName))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.disabledText)
                )
                Spacer(Modifier.width(4.dp))
                RenderBB(
                    data = data.data,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, bottom = 4.dp)

                )
            }
        }
        is MessageText -> Text(data.text)
    }
}