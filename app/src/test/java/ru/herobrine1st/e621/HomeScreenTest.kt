package ru.herobrine1st.e621

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.input.ImeAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.herobrine1st.e621.ui.screen.home.Home
import ru.herobrine1st.e621.ui.screen.home.HomeViewModel
import ru.herobrine1st.e621.ui.screen.home.HomeViewModel.LoginState
import ru.herobrine1st.e621.ui.snackbar.LocalSnackbar
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.ui.snackbar.SnackbarMessage
import ru.herobrine1st.e621.utils.assertEditableTextEquals
import ru.herobrine1st.e621.utils.assertEditableTextMatches


@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"]) // fixed in espresso 3.5
class HomeScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    var rule2: MockitoRule = MockitoJUnit.rule()

    private fun setContent(viewModel: HomeViewModel, snackbar: SnackbarAdapter) {
        rule.setContent {
            CompositionLocalProvider(
                LocalSnackbar provides snackbar
            ) {
                Home(
                    navigateToSearch = { /*TODO*/ },
                    navigateToFavorites = { /*TODO*/ },
                    viewModel = viewModel
                )
            }
        }
    }

    private fun loginWith(login: String, password: String) {
        rule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .performTextInput(login)

        rule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .performTextInput(password)

        rule.onNodeWithText(stringResource(R.string.login_login))
            .assertIsDisplayed()
            .performClick()

    }

    @Test
    fun testSuccessAuthorization() {
        val state = mutableStateOf(LoginState.NO_AUTH)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer {
                return@doAnswer state.value
            }
            on {
                login(
                    eq("login"),
                    eq("api key"),
                    any()
                )
            } doAnswer {
                it.getArgument<(LoginState) -> Unit>(2).invoke(LoginState.AUTHORIZED)
                state.value = LoginState.AUTHORIZED
            }
        }
        setContent(
            vm,
            snackbar = mock {
                onBlocking { enqueueMessage(any(), any(), any()) } doThrow
                        RuntimeException("Snackbar should not be used")
            }
        )
        loginWith("login", "api key")
        rule.onNodeWithText(stringResource(R.string.login_logout)).assertIsDisplayed()
    }

    @Test
    fun testFailedAuthorization() {
        val state = mutableStateOf(LoginState.NO_AUTH)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer {
                return@doAnswer state.value
            }
            on {
                login(any(), any(), any())
            } doAnswer {
                it.getArgument<(LoginState) -> Unit>(2).invoke(LoginState.NO_AUTH)
                state.value = LoginState.NO_AUTH
            }
        }
        val messages = mutableListOf<SnackbarMessage>()
        val snackbar = mock<SnackbarAdapter> {
            onBlocking { enqueueMessage(any(), any(), any()) } doAnswer {
                val msg = it.arguments[0] as Int
                val duration = it.arguments[1] as SnackbarDuration

                @Suppress("UNCHECKED_CAST")
                val formatArgs = (it.arguments[2] as Array<out Any>)
                messages.add(SnackbarMessage(msg, duration, formatArgs))
                Unit
            }
        }
        setContent(vm, snackbar)
        loginWith("i don't know my login", "i don't know my api key")
        rule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .assertEditableTextEquals("i don't know my login")
        rule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .assertEditableTextMatches { isNotBlank() }
        // .assertEditableTextEquals("i don't know my api key") // Text transformation interfere
        rule.onNodeWithText(stringResource(R.string.login_login)).assertIsDisplayed()
        assertEquals(1, messages.size)
        val msg = messages[0]
        assertEquals(msg.formatArgs.size, 0)
        assertEquals(msg.duration, SnackbarDuration.Long)
        assertEquals(msg.stringId, R.string.login_unauthorized)
    }

    // TODO IO error and retrying
    // TODO navigation buttons

    private fun stringResource(@StringRes id: Int) = rule.activity.getString(id)
}