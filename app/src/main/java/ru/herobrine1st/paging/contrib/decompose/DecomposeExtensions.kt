/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2025 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.paging.contrib.decompose

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.essenty.statekeeper.StateKeeper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer
import ru.herobrine1st.paging.api.LoadStates
import ru.herobrine1st.paging.api.PagingItems
import ru.herobrine1st.paging.api.Snapshot
import ru.herobrine1st.paging.api.getStateForPreservation
import ru.herobrine1st.paging.internal.Page
import ru.herobrine1st.paging.internal.PagingItemsImpl

/**
 * Collects this pager as PagingItems in Decompose component
 *
 * @param coroutineScope scope to launch coroutine in
 * @param lifecycle Essenty [Lifecycle] of calling component
 * @param startImmediately If set to true, refresh is started as soon as component is RESUMED. If false, first refresh should be done manually via [PagingItems.refresh] method
 */
fun <Key : Any, Value : Any> Flow<Snapshot<Key, Value>>.connectToDecomposeComponentAsPagingItems(
    coroutineScope: CoroutineScope,
    lifecycle: Lifecycle,
    startImmediately: Boolean = true
): PagingItems<Value> {

    val pagingItems = PagingItemsImpl(this, startImmediately)

    // if startImmediately is true, pagingItems.state.refresh is already Loading here,
    // avoiding visual bugs

    lifecycle.doOnResume {
        val job = coroutineScope.launch {
            pagingItems.collectPagingData()
        }
        // Compose's collectAsPagingItems does not explicitly cancel job
        // but its coroutine scope is destroyed after composition is gone
        // At this time, decompose component is in STOPPED state, which allows to
        // match behavior of compose collector on decompose too
        // Pause/resume is used as more strict form of lifecycle (although I don't see difference between STARTED and RESUMED)
        lifecycle.doOnPause(isOneTime = true) {
            job.cancel()
        }
    }

    return pagingItems
}


/**
 * The state preservation feature of Paging allows to save paging state across process recreations.
 * This function saves pager state in [StateKeeper].
 *
 * @param key a key to be associated with the pager state value.
 * @param keySerializer a [KSerializer] for serializing the key.
 * @param valueSerializer a [KSerializer] for serializing the value.
 * @param supplier a supplier of strictly the result of [ru.herobrine1st.paging.createPager]. If violated, behavior is undefined.
 */
fun <Key : Any, Value : Any> StateKeeper.registerPagingState(
    key: String,
    // using KSerializer instead of SerializationStrategy as it is incredibly hard to create
    // ListSerializationStrategy and PairSerializationStrategy without using internals of kotlinx.serialization
    keySerializer: KSerializer<Key>,
    valueSerializer: KSerializer<Value>,
    supplier: () -> SharedFlow<Snapshot<Key, Value>>
) {
    val stateSerializer = PairSerializer(
        ListSerializer(Page.serializer(keySerializer, valueSerializer)),
        LoadStates.serializer()
    )

    register(key, stateSerializer) { supplier().getStateForPreservation() }
}

/**
 * The state preservation feature of Paging allows to save paging state across process recreations.
 * This function restores pager from [StateKeeper] and returns its state to be provided to [ru.herobrine1st.paging.createPager].
 *
 * This function should be called every time component is [Lifecycle.State.INITIALIZED] even if its result will be dropped instantly
 * due to [com.arkivanov.essenty.instancekeeper.InstanceKeeper.Instance] holding the same (or newer) state. The reason to do that is large memory footprint
 * of Paging, and while serialized, it consumes memory in StateKeeper despite being not used.
 *
 * @param key a key to look up.
 * @param keySerializer a [KSerializer] for deserializing the key.
 * @param valueSerializer a [KSerializer] for deserializing the value.
 * @return Pager state to be provided to [ru.herobrine1st.paging.createPager].
 */
fun <Key : Any, Value : Any> StateKeeper.consumePagingState(
    key: String,
    // using KSerializer instead of DeserializationStrategy as it is incredibly hard to create
    // ListDeserializationStrategy and PairDeserializationStrategy without using internals of kotlinx.serialization
    keySerializer: KSerializer<Key>,
    valueSerializer: KSerializer<Value>,
): Pair<List<Page<Key, Value>>, LoadStates>? {
    val stateSerializer = PairSerializer(
        ListSerializer(Page.serializer(keySerializer, valueSerializer)),
        LoadStates.serializer()
    )
    return consume(key, stateSerializer)
}