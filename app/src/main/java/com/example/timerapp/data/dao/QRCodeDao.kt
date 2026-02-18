package com.example.timerapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerapp.models.QRCodeData
import kotlinx.coroutines.flow.Flow

@Dao
interface QRCodeDao {

    @Query("SELECT * FROM qr_codes ORDER BY name ASC")
    fun getAllQRCodes(): Flow<List<QRCodeData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQRCode(qrCode: QRCodeData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQRCodes(qrCodes: List<QRCodeData>)

    @Query("DELETE FROM qr_codes WHERE id = :id")
    suspend fun deleteQRCode(id: String)

    @Query("DELETE FROM qr_codes")
    suspend fun deleteAllQRCodes()
}
