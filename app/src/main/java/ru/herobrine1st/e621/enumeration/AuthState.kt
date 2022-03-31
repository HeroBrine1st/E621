package ru.herobrine1st.e621.enumeration

enum class AuthState {
    NO_DATA, // default if no auth info at the start of app
    LOADING,
    AUTHORIZED,
    IO_ERROR,
    SQL_ERROR,
    UNAUTHORIZED // error when trying to authenticate
}