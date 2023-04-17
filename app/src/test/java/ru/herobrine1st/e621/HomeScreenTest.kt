package ru.herobrine1st.e621

// It does not compile after migration to Decompose
/*
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
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    var mockitoRule: MockitoRule = MockitoJUnit.rule()

    private fun setContent(
        viewModel: HomeViewModel,
        navigateToSearch: () -> Unit = {},
        navigateToFavorites: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            Home(navigateToSearch, navigateToFavorites, viewModel)
        }
    }

    private fun loginWith(login: String, password: String) {
        composeTestRule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .performTextInput(login)

        composeTestRule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .performTextInput(password)

        composeTestRule.onNodeWithText(stringResource(R.string.login_login))
            .assertIsDisplayed()
            .performClick()

    }

    @Test
    fun testSuccessAuthorization() {
        val state = mutableStateOf(LoginState.NO_AUTH)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer { state.value }
            on { login(eq("login"), eq("api key"), any()) } doAnswer {
                it.getArgument<(LoginState) -> Unit>(2).invoke(LoginState.AUTHORIZED)
                state.value = LoginState.AUTHORIZED
            }
        }
        setContent(vm)
        loginWith("login", "api key")
        composeTestRule.onNodeWithText(stringResource(R.string.login_logout)).assertIsDisplayed()
    }

    @Test
    fun testFailedAuthorization() {
        val state = mutableStateOf(LoginState.NO_AUTH)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer { state.value }
            on { login(any(), any(), any()) } doAnswer {
                it.getArgument<(LoginState) -> Unit>(2).invoke(LoginState.NO_AUTH)
                state.value = LoginState.NO_AUTH
            }
        }
        setContent(vm)
        loginWith("i don't know my login", "i don't know my api key")
        composeTestRule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .assertEditableTextEquals("i don't know my login")
        composeTestRule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .assertEditableTextMatches { isNotBlank() }
        // .assertEditableTextEquals("i don't know my api key") // Text transformation interfere
        composeTestRule.onNodeWithText(stringResource(R.string.login_login)).assertIsDisplayed()
    }

    @Test
    fun testIOExceptionAuthorization() {
        val state = mutableStateOf(LoginState.NO_AUTH)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer { state.value }
            on { login(any(), any(), any()) } doAnswer {
                it.getArgument<(LoginState) -> Unit>(2).invoke(LoginState.IO_ERROR)
            }
        }

        setContent(vm)
        loginWith("login", "password")
        composeTestRule.onNode(hasImeAction(ImeAction.Next))
            .assertIsDisplayed()
            .assertEditableTextEquals("login")
        composeTestRule.onNode(hasImeAction(ImeAction.Done))
            .assertIsDisplayed()
            .assertEditableTextMatches { isNotBlank() }
    }

    @Test
    fun testIOExceptionInitial() {
        val state = mutableStateOf(LoginState.IO_ERROR)
        val vm = mock<HomeViewModel> {
            on { this.state } doAnswer { state.value }
            on { checkAuthorization() } doAnswer {
                state.value = LoginState.AUTHORIZED
            }
        }

        setContent(vm)
        composeTestRule.onNodeWithText(stringResource(R.string.network_error))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.retry))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.login_logout)).assertIsDisplayed()
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
        composeTestRule.onNodeWithText(stringResource(R.string.search))
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
        composeTestRule.onNodeWithText(stringResource(R.string.favourites))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertEquals(true, flag)
    }

    private fun stringResource(@StringRes id: Int) = composeTestRule.activity.getString(id)
}
 */