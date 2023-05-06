/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.util

import android.util.Log
import ru.herobrine1st.e621.preference.proto.ProxyOuterClass
import java.io.IOException
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class ProxyWithAuth(proxy: ProxyOuterClass.Proxy) : Proxy(
    when (proxy.type) {
        ProxyOuterClass.ProxyType.SOCKS5 -> Type.SOCKS
        null -> throw RuntimeException("Got null from proxy.type, which is impossible")
    },
    InetSocketAddress(proxy.hostname, proxy.port)
) {
    val auth = if (proxy.hasAuth()) PasswordAuthentication(
        proxy.auth.username,
        proxy.auth.password.toCharArray()
    ) else null
}

class ProxySelectorImpl(proxies: List<Proxy>) : ProxySelector() {
    private val proxies = proxies.ifEmpty { listOf(Proxy.NO_PROXY) }
    override fun select(uri: URI?) = proxies

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
}

class AuthenticatorImpl(private val proxies: List<ProxyWithAuth>) : Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication? {
        if (requestorType != RequestorType.PROXY) return null
        if (proxies.isEmpty()) return null

        debug {
            Log.d("Authenticator", "$requestorType $requestingSite $requestingHost $requestingPort")
        }
        return proxies.find {
            val address = it.address() as InetSocketAddress
            requestingHost == address.hostName && requestingPort == address.port
        }?.auth
    }
}