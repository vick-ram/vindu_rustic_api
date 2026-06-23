package org.example.domain.repo

import org.example.domain.models.catalog.ProductMedia

interface ProductMediaRepository: CrudRepository<ProductMedia, String> {
}