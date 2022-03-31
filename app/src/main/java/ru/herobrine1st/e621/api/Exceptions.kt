package ru.herobrine1st.e621.api

import java.io.IOException

class ApiException(message: String, val statusCode: Int, cause: Throwable? = null): IOException(message, cause)