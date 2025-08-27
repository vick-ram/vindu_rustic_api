package org.example.services

import org.example.domain.models.Product
import org.example.domain.repo.CategoryRepository
import org.example.domain.repo.ProductRepository
import org.example.plugins.NotFoundException

class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend fun createProduct(product: Product): Product {
        // Verify category exists
        val category = categoryRepository.read(product.categoryId)
            ?: throw NotFoundException("Category not found")

        return productRepository.create(product.copy(categoryId = category.id))
    }

    suspend fun updateStock(productId: String, available: Int): Product? {
        return productRepository.updateProductStock(productId, available)
    }

    suspend fun getProductsWithLowStock(): List<Product> {
        return productRepository.getProductsWithLowStock()
    }

    suspend fun getProduct(productId: String): Product? {
        return productRepository.read(productId)
    }

    suspend fun getProducts(offset: Long, limit: Int, queryParams: Map<String, String>): List<Product> {
        return productRepository.readAll(offset.toInt(), limit, queryParams)
    }

    suspend fun updateProduct(productId: String, product: Product): Product? {
        return productRepository.update(productId, product)
    }

    suspend fun deleteProduct(productId: String): Boolean {
        return productRepository.delete(productId)
    }
}