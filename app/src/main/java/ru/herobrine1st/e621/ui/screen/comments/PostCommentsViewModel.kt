package ru.herobrine1st.e621.ui.screen.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter


class PostCommentsViewModel @AssistedInject constructor(
    api: API,
    snackbar: SnackbarAdapter,
    @Assisted postId: Int
): ViewModel() {
    private val pager = Pager(
        PagingConfig(
            pageSize = BuildConfig.PAGER_PAGE_SIZE,
            initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
        )
    ) {
        PostCommentsSource(api, snackbar, postId)
    }

    val commentsFlow = pager.flow.cachedIn(viewModelScope)

    // Assisted inject stuff

    @AssistedFactory
    interface Factory {
        fun create(postId: Int): PostCommentsViewModel
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface FactoryProvider {
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("providePostCommentsViewModelFactory")
        fun provideFactory(): Factory
    }

    companion object {
        const val TAG = "PostCommentsViewModel"
        @Suppress("UNCHECKED_CAST")
        fun provideFactory(
            assistedFactory: Factory,
            postId: Int,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(postId) as T
            }
        }
    }
}