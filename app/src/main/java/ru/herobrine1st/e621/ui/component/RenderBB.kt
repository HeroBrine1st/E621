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
import ru.herobrine1st.e621.api.MessageQuote
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.ui.theme.disabledText

@Composable
fun RenderBB(text: String, modifier: Modifier = Modifier) {
    val parsed = remember(text) { parseBBCode(text) }
    Column(modifier) {
        parsed.forEach {
            if (it is MessageQuote) {
                Text(stringResource(R.string.quote_comments, it.userName))
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
                    Text(
                        it.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp, bottom = 4.dp)

                    )
                }
            } else if (it is MessageText) Text(it.text)
        }
    }
}