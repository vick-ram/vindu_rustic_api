package org.example.domain.repo

import org.example.domain.models.catalog.ProductTag

interface ProductTagRepository : CrudRepository<ProductTag, String>