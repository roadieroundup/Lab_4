package cr.ac.una.gps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cr.ac.una.gps.entity.Poligono


@Dao
interface PoligonoDao {
    @Insert
    fun insert(entity: Poligono)

    @Query("SELECT * FROM Poligono")
    fun getAll(): List<Poligono>

    //delete by id
    @Query("DELETE FROM Poligono WHERE id = :id")
    fun deleteById(id: Long?)
}