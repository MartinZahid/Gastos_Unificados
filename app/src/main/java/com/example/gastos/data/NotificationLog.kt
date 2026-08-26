package com.example.gastos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val parsed: Boolean,
    val merchant: String? = null,
    val amount: Double? = null,
    val bank: String? = null,
    val reason: String? = null,
    val inTargetList: Boolean = false,
    val type: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    // true una vez que el usuario ya vio esta entrada como "no reconocida"
    // en Modo dev (vía el botón "Marcar como revisadas"). Sirve para no
    // repetir la alerta proactiva de notificaciones sin parsear.
    val reviewed: Boolean = false
)
