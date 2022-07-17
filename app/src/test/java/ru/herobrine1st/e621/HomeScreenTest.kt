package ru.herobrine1st.e621

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
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
import ru.herobrine1st.e621.utils.assertEditableTextEquals
import ru.herobrine1st.e621.utils.assertEditableTextMatches


@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"]) // fixed in espresso 3.5
class HomeScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    var rule2: MockitoRule = MockitoJUnit.rule()

    private fun setContent(
        viewModel: HomeViewModel,
        navigateToSearch: () -> Unit = {},
        navigateToFavorites: () -> Unit = {},
    ) {
        rule.setContent {
            Home(navigateToSearch, navigateToFavorites, viewModel)
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
            vm
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
        setContent(vm)
        loginWith("i don't know my login", "i don't know my api key")
        rule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .assertEditableTextEquals("i don't know my login")
        rule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .assertEditableTextMatches { isNotBlank() }
        // .assertEditableTextEquals("i don't know my api key") // Text transformation interfere
        rule.onNodeWithText(stringResource(R.string.login_login)).assertIsDisplayed()
    }

    @Test
    fun testIOExceptionAuthorization() {
        val state = mutableStateOf(LoginState.NO_AUTH)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer {
                return@doAnswer state.value
            }
            on {
                login(any(), any(), any())
            } doAnswer {
                it.getArgument<(LoginState) -> Unit>(2).invoke(LoginState.IO_ERROR)
            }
        }

        setContent(vm)
        loginWith("login", "password")
        rule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .assertEditableTextEquals("login")
        rule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .assertEditableTextMatches { isNotBlank() }
    }

    @Test
    fun testIOExceptionInitial() {
        val state = mutableStateOf(LoginState.IO_ERROR)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer {
                return@doAnswer state.value
            }
            on { checkAuthorization() } doAnswer {
                state.value = LoginState.AUTHORIZED
            }
        }

        setContent(vm)
        rule.onNodeWithText(stringResource(R.string.network_error))
            .assertIsDisplayed()
        rule.onNodeWithText(stringResource(R.string.retry))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        rule.onNodeWithText(stringResource(R.string.login_logout)).assertIsDisplayed()
    }

    @Test
    fun testSearchButton() {
        val vm = mock<HomeViewModel> {
            on { this.state } doReturn LoginState.AUTHORIZED
        }
        var flag = false
        setContent(vm, navigateToSearch = {
            flag = true
        })
        assertEquals(false, flag)
        rule.onNodeWithText(stringResource(R.string.search))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertEquals(true, flag)
    }

    @Test
    fun testFavouritesButton() {
        val vm = mock<HomeViewModel> {
            on { this.state } doReturn LoginState.AUTHORIZED
        }
        var flag = false
        setContent(vm, navigateToFavorites = {
            flag = true
        })
        assertEquals(false, flag)
        rule.onNodeWithText(stringResource(R.string.favourites))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertEquals(true, flag)
    }

    private fun stringResource(@StringRes id: Int) = rule.activity.getString(id)
}