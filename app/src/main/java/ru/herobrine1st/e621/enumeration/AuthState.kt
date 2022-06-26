package ru.herobrine1st.e621.enumeration

enum class AuthState {
    /**
     * No credentials are stored
     */
    NO_DATA,

    /**
     * Initial state
     */
    LOADING,

    /**
     * Credentials are stored and valid
     */
    AUTHORIZED,

    /**
     * Couldn't perform a request to the API
     */
    IO_ERROR,

    /**
     * Internal error occurred
     */
    DATABASE_ERROR,

    /**
     * API error occurred while trying to authenticate
     */
    UNAUTHORIZED
}