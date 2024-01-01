package ru.herobrine1st.e621

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.herobrine1st.e621.api.FavouritesSearchOptions
import ru.herobrine1st.e621.navigation.config.Config

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test() {
        val test: Config = Config.PostListing(
            FavouritesSearchOptions("test", 1),
            0
        )
        val str = Json.encodeToString(Config.serializer(), test)
        println(str)
        val test2 = Json.decodeFromString(Config.serializer(), str)
        assertEquals(test, test2)
    }
}