package com.budget.app.data.repository

import com.budget.app.data.local.dao.CategoryDao
import com.budget.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun categoryNameExists(name: String): Boolean = categoryDao.categoryNameExists(name)

    suspend fun addCategoryIfNameIsAvailable(name: String, iconKey: String, color: String): Long? {
        if (categoryNameExists(name)) return null
        return categoryDao.insertCategory(
            CategoryEntity(name = name, icon = iconKey, iconKey = iconKey, color = color)
        )
    }

    /** Обновляет собственную категорию; null — имя занято другой категорией. */
    suspend fun updateCategoryIfNameIsAvailable(category: CategoryEntity): CategoryEntity? {
        if (categoryDao.categoryNameExistsExcluding(category.name, category.id)) return null
        val updated = category.copy(icon = category.iconKey ?: category.icon)
        categoryDao.updateCategory(updated)
        return updated
    }

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)
}
