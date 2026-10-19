package com.budget.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budget.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE LOWER(name) = LOWER(:name))")
    suspend fun categoryNameExists(name: String): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM categories
            WHERE LOWER(name) = LOWER(:name) AND id != :excludeId
        )
        """
    )
    suspend fun categoryNameExistsExcluding(name: String, excludeId: Long): Boolean
}
