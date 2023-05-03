package cr.ac.una.gps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cr.ac.una.gps.entity.Ubicacion


@Dao
interface UbicacionDao {
    @Insert
    fun insert(entity: Ubicacion)

    @Query("SELECT * FROM Ubicacion")
    fun getAll(): List<Ubicacion>


    @Query("SELECT * FROM Ubicacion WHERE fecha BETWEEN :fecha AND :fecha + 86399999")
    fun getUbicacionesByFecha(fecha: Long): List<Ubicacion>
}