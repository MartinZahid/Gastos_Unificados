package com.Flood.gastometro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {

    @Insert
    suspend fun insert(log: NotificationLog)

    // Cuenta reenvíos recientes e idénticos (misma app, mismo título y texto).
    // Samsung y algunos bancos entregan la misma notificación dos veces o la
    // vuelven a publicar; sirve para no crear una transacción por cada copia.
    @Query(
        "SELECT COUNT(*) FROM notification_log " +
            "WHERE packageName = :packageName AND title = :title AND text = :text " +
            "AND dateMillis >= :sinceMillis"
    )
    suspend fun countRecentDuplicates(
        packageName: String,
        title: String,
        text: String,
        sinceMillis: Long
    ): Int

    @Query("SELECT * FROM notification_log ORDER BY dateMillis DESC, id DESC LIMIT 200")
    fun observeAll(): Flow<List<NotificationLog>>

    @Query(
        "DELETE FROM notification_log WHERE id NOT IN (" +
            "SELECT id FROM notification_log " +
            "ORDER BY inTargetList DESC, dateMillis DESC, id DESC LIMIT 200)"
    )
    suspend fun prune()

    @Query("DELETE FROM notification_log")
    suspend fun clear()

    @Query("UPDATE notification_log SET type = :type WHERE id = :id")
    suspend fun setType(id: Long, type: String)

    // Notificaciones de un banco soportado (inTargetList) que el parser NO
    // logró leer y que el usuario todavía no ha revisado. Es la señal para
    // avisar de forma proactiva "se te pueden estar perdiendo gastos".
    // Los fallos "sin monto" (promos, saldos, avisos) no cuentan: un gasto
    // real siempre trae monto, y los avisos de bancos como Santander lloverían
    // en el contador disparando alertas falsas.
    @Query(
        "SELECT COUNT(*) FROM notification_log " +
            "WHERE parsed = 0 AND inTargetList = 1 AND reviewed = 0 " +
            "AND reason != 'sin monto'"
    )
    fun observeUnreviewedFailureCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM notification_log " +
            "WHERE parsed = 0 AND inTargetList = 1 AND reviewed = 0 " +
            "AND reason != 'sin monto'"
    )
    suspend fun unreviewedFailureCount(): Int

    // Se llama cuando el usuario abre/revisa el log en Modo dev, para que la
    // alerta no se repita con las mismas entradas ya vistas.
    @Query(
        "UPDATE notification_log SET reviewed = 1 " +
            "WHERE parsed = 0 AND inTargetList = 1 AND reviewed = 0 " +
            "AND reason != 'sin monto'"
    )
    suspend fun markFailuresReviewed()
}
