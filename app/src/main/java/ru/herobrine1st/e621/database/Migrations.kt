package ru.herobrine1st.e621.database

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable.Entries(DeleteTable(tableName = "Auth"))
class Version2To3DeleteTableAuth: AutoMigrationSpec