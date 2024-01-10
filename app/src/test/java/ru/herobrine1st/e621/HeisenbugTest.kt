package ru.herobrine1st.e621

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.herobrine1st.e621.api.search.FavouritesSearchOptions
import ru.herobrine1st.e621.navigation.config.Config

class HeisenbugTest {

    @Test
    fun test() {
        val test: Config = Config.PostListing(
            FavouritesSearchOptions("test", 1),
            0
        )
        // under some circumstances, it does not serialize
        val str = Json.encodeToString(Config.serializer(), test)
        println(str)
        val test2 = Json.decodeFromString(Config.serializer(), str)
        assertEquals(test, test2)
    }
}