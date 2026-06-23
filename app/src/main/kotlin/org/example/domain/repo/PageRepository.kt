package org.example.domain.repo

import org.example.domain.models.content.Page

interface PageRepository : CrudRepository<Page, String> {
}