package ru.herobrine1st.e621

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.herobrine1st.e621.preference.proto.ProxyOuterClass.Proxy


class ProtobufTest {
    @Test
    fun testDefaultProxyInstance() {
        val auth = Proxy.getDefaultInstance().auth
        assertEquals("", auth.username)
        assertEquals("", auth.password)
    }
}