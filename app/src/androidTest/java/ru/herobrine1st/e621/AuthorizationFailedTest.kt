package ru.herobrine1st.e621

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.awaitResponse

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthorizationFailedTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    lateinit var api: API

    @Before
    fun prepare() {
        launchActivity<MainActivity>().use { scenario ->
            scenario.onActivity {
                val helper = EntryPoints.get(it, HelperEntryPoint::class.java)
                api = helper.getAPI()
            }
        }
        hiltRule.inject()
    }

    @Test
    fun testUnauthorizedResponseCode(): Unit = runBlocking {
        val code = try {
            val response = api.getUser("", credentials = Credentials.basic("bawfhyfmuiea", "qdvbjuuqd3fjthgb")).awaitResponse()
            response.code().also { response.raw().close() }
        } catch (e: ApiException) {
            e.statusCode
        }
        assertEquals(401, code)
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface HelperEntryPoint {
        fun getAPI(): API
    }
}