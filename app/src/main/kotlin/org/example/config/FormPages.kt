package org.example.config

object FormPages {
    private val formPages = listOf(
        FormPage(
            templates = setOf("admin/pages/dashboard"),
            paths = setOf("/admin/dashboard")
        ),
        FormPage(
            templates = setOf("admin/pages/users/index", "admin/pages/users/detail"),
            paths = setOf("/admin/users", "/admin/users/detail/{id}")
        ),
        FormPage(
            templates = setOf("admin/pages/products/categories", "admin/pages/products/new",
                "admin/pages/products/list", "admin/pages/products/discount",
                "admin/pages/products/reviews", "admin/pages/products/offer"),
            paths = setOf("/admin/products/categories", "/admin/products/new",
                "/admin/products/list", "/admin/products/discount",
                "/admin/products/reviews", "/admin/products/offer")
        ),
        FormPage(
            templates = setOf("admin/pages/orders/index"),
            paths = setOf("/admin/orders")
        ),
        FormPage(
            templates = setOf("admin/pages/analytics"),
            paths = setOf("/admin/analytics")
        ),
        FormPage(
            templates = setOf("admin/pages/settings"),
            paths = setOf("/admin/settings")
        ),
        FormPage(
            templates = setOf("admin/pages/help"),
            paths = setOf("/admin/help")
        ),
        FormPage(
            templates = setOf("admin/pages/profile"),
            paths = setOf("/admin/profile")
        ),
        FormPage(
            templates = setOf("auth/signin.html", "auth/signup.html", "auth/forgot-password.html"),
            paths = setOf("/signin", "/signup", "/forgot-password")
        ),
        FormPage(
            templates = setOf("pages/index"), // Home page
            paths = setOf("/")
        )
    )

    fun matches(template: String?, path: String): Boolean {
        if (template == null) return false
        return formPages.any { page ->
            template in page.templates || path in page.paths
        }
    }

    private data class FormPage(
        val templates: Set<String>,
        val paths: Set<String>
    )
}