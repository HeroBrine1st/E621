package ru.herobrine1st.e621.api

import java.io.IOException

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
open class ApiException(message: String, val statusCode: Int, cause: Throwable? = null) :
    IOException("$message (http code $statusCode)", cause) {
}

class NotFoundException(cause: Throwable? = null): ApiException("Not found", 404, cause)