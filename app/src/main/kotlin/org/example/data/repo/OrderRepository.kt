package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.example.data.mappers.OrderMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Address
import org.example.domain.models.inventory.InventoryMovement
import org.example.domain.models.inventory.InventoryReservation
import org.example.domain.models.inventory.Warehouse
import org.example.domain.models.marketing.Coupon
import org.example.domain.models.payments.Payment
import org.example.domain.models.production.ProductionJob
import org.example.domain.models.production.ProductionStage
import org.example.domain.models.sales.CartItem
import org.example.domain.models.sales.Order
import org.example.domain.models.sales.OrderItem
import org.example.domain.models.sales.OrderStatusHistory
import org.example.domain.models.shipping.Shipment
import org.example.domain.models.system.AuditLog
import org.example.domain.models.system.OutboxEvent
import org.example.plugins.EmptyCartException
import org.example.plugins.InsufficientInventoryException
import org.example.plugins.InvalidCouponException
import org.example.plugins.NotFoundException
import java.math.BigDecimal
import java.net.InetAddress
import java.time.OffsetDateTime

@Component
class OrderRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    orderMapper: OrderMapper,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val shoppingCartRepository: ShoppingCartRepository,
    private val addressRepository: AddressRepository,
    private val cartItemRepository: CartItemRepository,
    private val inventoryRepository: InventoryRepository,
    private val inventoryMovementRepository: InventoryMovementRepository,
    private val inventoryReservationRepository: InventoryReservationRepository,
    private val paymentRepository: PaymentRepository,
    private val couponRepository: CouponRepository,
    private val productionJobRepository: ProductionJobRepository,
    private val productionStageRepository: ProductionStageRepository,
    private val shipmentRepository: ShipmentRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val auditLogRepository: AuditLogRepository
) : CrudRepository<Order, String>(
    connectionFactory = connectionFactory,
    tableName = "orders",
    mapper = orderMapper
) {
    override val generatedColumns = listOf("id", "placed_at", "updated_at")

    /**
     * Main order placement flow
     */
    suspend fun createOrder(
        userId: String?,
        cartId: String,
        shippingAddressId: String,
        billingAddressId: String? = null,
        couponCode: String? = null,
        notes: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): OrderConfirmation {

        // Validate cart and shipping address
        val cart = shoppingCartRepository.read(cartId)
            ?: throw NotFoundException("Cart with id $cartId not found")

        val shippingAddress = addressRepository.read(shippingAddressId)
            ?: throw NotFoundException("shipping address with id $shippingAddressId not found")

        // Get cart items with full product/variant details
        val cartItems = cartItemRepository.findByCartId(cartId)
        if (cartItems.isEmpty()) {
            throw EmptyCartException(cartId)
        }

        // Process the entire order in a single transaction
        return connectionFactory.withTransaction { connection ->

            // 1. VALIDATE INVENTORY AND CREATE RESERVATIONS
            val reservations = reserveInventory(cartItems, cartId)

            // 2. CREATE ORDER
            val order = createOrderRecord(
                userId = userId,
                shippingAddress = shippingAddress,
                billingAddressId = billingAddressId ?: shippingAddressId,
                cartItems = cartItems,
                couponCode = couponCode,
                notes = notes,
            )

            // 3. CREATE ORDER ITEMS FROM CART ITEMS
            val orderItems = createOrderItems(
                orderId = order.id,
                cartItems = cartItems,
                reservations = reservations,
            )

            // 4. UPDATE RESERVATIONS WITH ORDER ID
            updateReservationsWithOrderId(
                reservations = reservations,
                orderId = order.id,
            )

            // 5. RECORD INVENTORY MOVEMENTS
            recordInventoryMovements(orderItems = orderItems)

            // 6. CLEAR THE CART
            cartItemRepository.clearCart(cartId)

            // 7. INITIALIZE PAYMENT
            val payment = initializePayment(order)

            // 8. CREATE PRODUCTION JOBS FOR CUSTOMIZABLE ITEMS
            createProductionJobsIfNeeded(orderItems)

            // 9. RECORD ORDER STATUS HISTORY
            recordStatusHistory(
                orderId = order.id,
                oldStatus = null,
                newStatus = "pending",
            )

            // 10. LOG AUDIT
            auditLogRepository.create(
                AuditLog(
                    actorId = userId,
                    actorType = "user",
                    action = "PLACE_ORDER",
                    entityType = "Order",
                    entityId = order.id,
                    changes = buildJsonObject {
                        put("order_total", JsonPrimitive(order.totalAmount.toString()))
                        put("item_count", JsonPrimitive(orderItems.size))
                        put("shipping_address", JsonPrimitive(shippingAddressId))
                    },
                    metadata = buildJsonObject {
                        put("ip_address", JsonPrimitive(ipAddress ?: "unknown"))
                        put("user_agent", JsonPrimitive(userAgent ?: "unknown"))
                        put("cart_id", JsonPrimitive(cartId))
                    },
                    ipAddress = ipAddress?.let { InetAddress.getByName(it) },
                    userAgent = userAgent
                ),
            )

            // 11. EMIT OUTBOX EVENTS
            emitOrderEvents(order, orderItems, payment)

            // 12. BUILD RESPONSE
            OrderConfirmation(
                order = order,
                orderItems = orderItems,
                payment = payment,
                shippingAddress = shippingAddress,
                reservations = reservations
            )
        }
    }

    /**
     * Reserve inventory for cart items
     */
    private suspend fun reserveInventory(
        cartItems: List<CartItem>,
        cartId: String,
    ): List<InventoryReservation> {
        return cartItems.map { cartItem ->
            val variant = productVariantRepository.read(cartItem.variantId)
                ?: throw NotFoundException(cartItem.variantId)

            val warehouse = findOptimalWarehouse(cartItem.variantId, cartItem.quantity)
                ?: throw InsufficientInventoryException(
                    variantId = cartItem.variantId,
                    required = cartItem.quantity,
                    available = 0
                )

            // Use the existing reserveStock method from InventoryRepository
            try {
                inventoryRepository.reserveStock(
                    variantId = cartItem.variantId,
                    warehouseId = warehouse.id,
                    quantity = cartItem.quantity
                )
            } catch (e: Exception) {
                throw InsufficientInventoryException(
                    variantId = cartItem.variantId,
                    required = cartItem.quantity,
                    available = 0,
                    cause = e
                )
            }

            // Create reservation record
            val reservation = InventoryReservation(
                variantId = cartItem.variantId,
                warehouseId = warehouse.id,
                cartId = cartId,
                quantity = cartItem.quantity,
                status = "active",
                expiresAt = OffsetDateTime.now().plusMinutes(30)
            )

            inventoryReservationRepository.create(reservation)
        }
    }

    /**
     * Find optimal warehouse with sufficient stock
     */
    private suspend fun findOptimalWarehouse(
        variantId: String,
        requiredQuantity: Int
    ): Warehouse? {
        // Query to find warehouses with sufficient stock,
        // prioritizing those with most available stock
        val sql = """
            SELECT w.*, i.available_quantity
            FROM warehouses w
            INNER JOIN inventories i ON w.id = i.warehouse_id
            WHERE i.variant_id = :variantId
              AND i.available_quantity >= :requiredQuantity
            ORDER BY i.available_quantity DESC, w.created_at ASC
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("variantId" to variantId, "requiredQuantity" to requiredQuantity))
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    Warehouse(
                        id = row.get("id", String::class.java)!!,
                        name = row.get("name", String::class.java)!!,
                        addressId = row.get("address_id", String::class.java)!!,
                        createdAt = row.get("created_at", OffsetDateTime::class.java)!!
                    )
                }
                .awaitFirstOrNull()
        }
    }

    private suspend fun createOrderRecord(
        userId: String?,
        shippingAddress: Address,
        billingAddressId: String,
        cartItems: List<CartItem>,
        couponCode: String?,
        notes: String?,
    ): Order {
        val subtotal = calculateSubtotal(cartItems)
        val shippingCost = calculateShippingCost(shippingAddress, cartItems)
        val taxAmount = calculateTax(subtotal, shippingAddress)
        val discountAmount = calculateDiscount(cartItems, couponCode, subtotal)

        val totalAmount = subtotal + shippingCost + taxAmount - discountAmount

        val order = Order(
            userId = userId,
            orderNumber = org.example.utils.generateOrderNumber(),
            shippingAddressId = shippingAddress.id,
            billingAddressId = billingAddressId,
            status = "pending",
            paymentStatus = "pending",
            fulfillmentStatus = "unfulfilled",
            currency = "kes",
            subtotal = subtotal,
            shippingCost = shippingCost,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            couponCode = couponCode,
            notes = notes,
            placedAt = OffsetDateTime.now()
        )
        return create(order)
    }

    private suspend fun createOrderItems(
        orderId: String,
        cartItems: List<CartItem>,
        reservations: List<InventoryReservation>,
    ): List<OrderItem> {
        return cartItems.map { cartItem ->
            val variant = productVariantRepository.read(cartItem.variantId)!!
            val product = productRepository.read(variant.productId)!!

            val reservation = reservations.find {
                it.variantId == cartItem.variantId
            }!!

            val orderItem = OrderItem(
                orderId = orderId,
                productId = variant.productId,
                variantId = cartItem.variantId,
                warehouseId = reservation.warehouseId,
                quantity = cartItem.quantity,
                unitPrice = variant.price,
                totalPrice = variant.price * cartItem.quantity.toBigDecimal(),
                customizationSnapshot = cartItem.customizationDetails?.let {
                    JsonObject(it.mapValues { entry -> JsonPrimitive(entry.value.toString()) })
                },
                productSnapshot = buildJsonObject {
                    put("title", JsonPrimitive(product.title))
                    put("sku", JsonPrimitive(variant.sku))
                    put("variant_title", JsonPrimitive(variant.title ?: ""))
                    put("brand", JsonPrimitive(product.brand ?: ""))
                    put("category", JsonPrimitive(product.categoryId ?: ""))
                }
            )
            orderItemRepository.create(orderItem)
        }
    }

    /**
     * Update reservations with order ID
     */
    private suspend fun updateReservationsWithOrderId(
        reservations: List<InventoryReservation>,
        orderId: String,
    ) {
        reservations.forEach { reservation ->
            inventoryReservationRepository.update(
                reservation.id,
                reservation.copy(
                    orderId = orderId,
                    status = "completed",
                    cartId = null
                )
            )
        }
    }

    /**
     * Record inventory movements
     */
    private suspend fun recordInventoryMovements(
        orderItems: List<OrderItem>,
    ) {
        orderItems.forEach { item ->
            val movement = InventoryMovement(
                variantId = item.variantId,
                warehouseId = item.warehouseId!!,
                movementType = "reservation_hold",
                quantity = item.quantity,
                referenceType = "order",
                referenceId = item.orderId,
                notes = "Order placement - inventory reserved",
                createdAt = OffsetDateTime.now()
            )

            inventoryMovementRepository.create(movement)
        }
    }

    /**
     * Initialize payment record
     */
    private suspend fun initializePayment(
        order: Order
    ): Payment {
        val payment = Payment(
            orderId = order.id,
            provider = "mpesa", // Default provider, can be dynamic
            amount = order.totalAmount,
            currency = order.currency,
            status = "initiated",
            paymentMethod = "pending",
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        return paymentRepository.create(payment)
    }

    /**
     * Create production jobs for customizable items
     */
    private suspend fun createProductionJobsIfNeeded(
        orderItems: List<OrderItem>,
    ): List<ProductionJob> {
        val customizableItems = orderItems.filter { item ->
            !item.customizationSnapshot.isNullOrEmpty()
        }

        return customizableItems.map { item ->
            val job = ProductionJob(
                orderItemId = item.id,
                productId = item.productId,
                status = "queued",
                priority = "normal",
                quantity = item.quantity,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

            val createdJob = productionJobRepository.create(job)

            // Create default production stages
            createDefaultProductionStages(createdJob.id)

            createdJob
        }
    }

    /**
     * Create default production stages for a job
     */
    private suspend fun createDefaultProductionStages(jobId: String) {
        val defaultStages = listOf(
            "Design Review",
            "Material Preparation",
            "Production",
            "Quality Check",
            "Packaging"
        )

        defaultStages.forEach { stageName ->
            productionStageRepository.create(
                ProductionStage(
                    jobId = jobId,
                    stageName = stageName,
                    status = "pending"
                )
            )
        }
    }

    /**
     * Record order status history
     */
    private suspend fun recordStatusHistory(
        orderId: String,
        oldStatus: String?,
        newStatus: String,
        changedBy: String? = null,
        comment: String? = null,
    ) {
        val statusHistory = OrderStatusHistory(
            orderId = orderId,
            oldStatus = oldStatus,
            newStatus = newStatus,
            changedBy = changedBy,
            comment = comment ?: "Order placed",
            createdAt = OffsetDateTime.now()
        )

        orderStatusHistoryRepository.create(statusHistory)
    }

    /**
     * Emit outbox events for async processing
     */
    private suspend fun emitOrderEvents(
        order: Order,
        orderItems: List<OrderItem>,
        payment: Payment,
    ) {
        // Order Created Event
        outboxEventRepository.create(
            OutboxEvent(
                aggregateType = "Order",
                aggregateId = order.id,
                eventType = "OrderCreated",
                payload = buildJsonObject {
                    put("order_id", JsonPrimitive(order.id))
                    put("order_number", JsonPrimitive(order.orderNumber ?: ""))
                    put("user_id", JsonPrimitive(order.userId ?: ""))
                    put("total_amount", JsonPrimitive(order.totalAmount.toString()))
                    put("currency", JsonPrimitive(order.currency))
                    put("status", JsonPrimitive(order.status))
                    put("item_count", JsonPrimitive(orderItems.size))
                    put("payment_id", JsonPrimitive(payment.id))
                    put("created_at", JsonPrimitive(order.placedAt.toString()))
                },
                createdAt = OffsetDateTime.now()
            )
        )

        // Inventory Reserved Event
        outboxEventRepository.create(
            OutboxEvent(
                aggregateType = "Inventory",
                aggregateId = order.id,
                eventType = "InventoryReserved",
                payload = buildJsonObject {
                    put("order_id", JsonPrimitive(order.id))
                    put("items", JsonArray(orderItems.map { item ->
                        buildJsonObject {
                            put("variant_id", JsonPrimitive(item.variantId))
                            put("quantity", JsonPrimitive(item.quantity))
                            put("warehouse_id", JsonPrimitive(item.warehouseId ?: ""))
                        }
                    }))
                },
                createdAt = OffsetDateTime.now()
            )
        )

        // Payment Initiated Event
        outboxEventRepository.create(
            OutboxEvent(
                aggregateType = "Payment",
                aggregateId = payment.id,
                eventType = "PaymentInitiated",
                payload = buildJsonObject {
                    put("payment_id", JsonPrimitive(payment.id))
                    put("order_id", JsonPrimitive(order.id))
                    put("amount", JsonPrimitive(payment.amount.toString()))
                    put("currency", JsonPrimitive(payment.currency))
                    put("provider", JsonPrimitive(payment.provider))
                },
                createdAt = OffsetDateTime.now()
            )
        )

        // If customizable items exist, emit production events
        val customizableItems = orderItems.filter {
            !it.customizationSnapshot.isNullOrEmpty()
        }

        if (customizableItems.isNotEmpty()) {
            outboxEventRepository.create(
                OutboxEvent(
                    aggregateType = "Production",
                    aggregateId = order.id,
                    eventType = "ProductionRequired",
                    payload = buildJsonObject {
                        put("order_id", JsonPrimitive(order.id))
                        put("items", JsonArray(customizableItems.map { item ->
                            buildJsonObject {
                                put("order_item_id", JsonPrimitive(item.id))
                                put("product_id", JsonPrimitive(item.productId))
                                put("quantity", JsonPrimitive(item.quantity))
                            }
                        }))
                    },
                    createdAt = OffsetDateTime.now()
                )
            )
        }
    }

    /**
     * Calculate order subtotal
     */
    private suspend fun calculateSubtotal(cartItems: List<CartItem>): BigDecimal {
        return cartItems.sumOf { cartItem ->
            val variant = productVariantRepository.read(cartItem.variantId)
            (variant?.price ?: BigDecimal.ZERO) * cartItem.quantity.toBigDecimal()
        }
    }

    /**
     * Calculate shipping cost
     */
    private suspend fun calculateShippingCost(
        shippingAddress: Address,
        cartItems: List<CartItem>
    ): BigDecimal {
        // Basic shipping calculation - can be enhanced with real shipping APIs
        val totalWeight = cartItems.sumOf { cartItem ->
            val variant = productVariantRepository.read(cartItem.variantId)
            (variant?.weightGrams ?: 0) * cartItem.quantity
        }

        return when {
            totalWeight == 0 -> BigDecimal.ZERO
            totalWeight <= 500 -> BigDecimal("5.00")
            totalWeight <= 1000 -> BigDecimal("10.00")
            totalWeight <= 5000 -> BigDecimal("15.00")
            else -> BigDecimal("25.00")
        }
    }

    /**
     * Calculate tax
     */
    private fun calculateTax(subtotal: BigDecimal, shippingAddress: Address): BigDecimal {
        // Kenya VAT is 16% for standard-rated supplies
        val vatRate = BigDecimal("0.16")
        return subtotal * vatRate
    }

    /**
     * Calculate discount from coupon
     */
    private suspend fun calculateDiscount(
        cartItems: List<CartItem>,
        couponCode: String?,
        subtotal: BigDecimal
    ): BigDecimal {
        if (couponCode == null) return BigDecimal.ZERO

        // Validate coupon
        val coupon = couponRepository.validateCoupon(couponCode, subtotal)
            ?: throw InvalidCouponException("Coupon is invalid or expired: $couponCode")

        // Check if coupon applies to the order
        if (!couponAppliesToOrder(coupon, cartItems)) {
            throw InvalidCouponException("Coupon does not apply to items in your cart")
        }

        // Calculate discount based on type
        val discountAmount = when (coupon.discountType) {
            "percentage" -> {
                subtotal * (coupon.discountValue / BigDecimal("100"))
            }

            "fixed_amount" -> {
                coupon.discountValue
            }

            "free_shipping" -> {
                // Will be applied in shipping calculation
                BigDecimal.ZERO
            }

            else -> throw InvalidCouponException("Unknown discount type: ${coupon.discountType}")
        }

        // Apply maximum discount cap if set
        return if (coupon.maxDiscountAmount != null && discountAmount > coupon.maxDiscountAmount) {
            coupon.maxDiscountAmount
        } else {
            discountAmount
        }
    }

    /**
     * Check if coupon applies to items in the order
     */
    private suspend fun couponAppliesToOrder(coupon: Coupon, cartItems: List<CartItem>): Boolean {
        return when (coupon.appliesToType) {
            "all" -> true
            "category" -> {
                val applicableCategoryId = coupon.appliesToId ?: return false
                cartItems.any { item ->
                    val variant = productVariantRepository.read(item.variantId)
                    val product = variant?.let { productRepository.read(it.productId) }
                    product?.categoryId == applicableCategoryId
                }
            }

            "product" -> {
                val applicableProductId = coupon.appliesToId ?: return false
                cartItems.any { item ->
                    val variant = productVariantRepository.read(item.variantId)
                    variant?.productId == applicableProductId
                }
            }

            "variant" -> {
                val applicableVariantId = coupon.appliesToId ?: return false
                cartItems.any { it.variantId == applicableVariantId }
            }

            else -> false
        }
    }

    /**
     * Apply coupon to order
     */
    private suspend fun applyCouponToOrder(
        couponCode: String,
        orderId: String,
        userId: String,
        discountAmount: BigDecimal,
        connection: Connection
    ) {
        val coupon = couponRepository.findByCode(couponCode)
            ?: throw NotFoundException("Coupon not found: $couponCode")

        // Record coupon usage
        couponRepository.applyCoupon(
            couponId = coupon.id,
            orderId = orderId,
            userId = userId,
            discountAmount = discountAmount
        )

        // Increment usage count
        couponRepository.incrementUsage(coupon.id)

        // Update shipping cost if free shipping coupon
        if (coupon.discountType == "free_shipping") {
            val updateSql = """
                UPDATE orders 
                SET shipping_cost = 0, 
                    total_amount = subtotal + tax_amount - discount_amount,
                    updated_at = :now
                WHERE id = :orderId
            """.trimIndent()

            connection.createNamedStatement(
                updateSql, mapOf("orderId" to orderId, "now" to OffsetDateTime.now())
            )
                .execute()
                .awaitSingle()
        }
    }

    /**
     * Get order with all related data
     */
    suspend fun getOrderWithDetails(orderId: String): OrderDetails? {
        val order = read(orderId) ?: return null

        val orderItems = orderItemRepository.findByOrderId(orderId)
        val shippingAddress = order.shippingAddressId.let {
            addressRepository.read(it)
        }
        val billingAddress = order.billingAddressId?.let {
            addressRepository.read(it)
        }
        val payments = paymentRepository.findByOrderId(orderId)
        val statusHistory = orderStatusHistoryRepository.findByOrderId(orderId)
        val shipments = shipmentRepository.findByOrderId(orderId)

        return OrderDetails(
            order = order,
            items = orderItems,
            shippingAddress = shippingAddress,
            billingAddress = billingAddress,
            payments = payments,
            statusHistory = statusHistory,
            shipments = shipments
        )
    }

    // Find order by order number
    suspend fun findByOrderNumber(orderNumber: String): Order? {
        val sql = "SELECT * FROM $tableName WHERE order_number = :orderNumber"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("orderNumber" to orderNumber))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Find orders by user
    suspend fun findByUserId(
        userId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "userId" to userId,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Find orders by email
    suspend fun findByEmail(
        email: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE email = :email 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "email" to email,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Find orders by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "status" to status,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Find orders by payment status
    suspend fun findByPaymentStatus(
        paymentStatus: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE payment_status = :paymentStatus 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "paymentStatus" to paymentStatus,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Find orders by fulfillment status
    suspend fun findByFulfillmentStatus(
        fulfillmentStatus: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE fulfillment_status = :fulfillmentStatus 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "fulfillmentStatus" to fulfillmentStatus,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Update order status
    suspend fun updateStatus(
        id: String,
        status: String,
        paymentStatus: String? = null,
        fulfillmentStatus: String? = null,
        connection: Connection? = null
    ): Order? {
        val setClauses = mutableListOf("status = :status", "updated_at = :updatedAt")
        val params = mutableMapOf<String, Any>(
            "id" to id,
            "status" to status,
            "updatedAt" to OffsetDateTime.now()
        )

        paymentStatus?.let {
            setClauses.add("payment_status = :paymentStatus")
            params["paymentStatus"] = it
        }

        fulfillmentStatus?.let {
            setClauses.add("fulfillment_status = :fulfillmentStatus")
            params["fulfillmentStatus"] = it
        }

        val sql = """
            UPDATE $tableName 
            SET ${setClauses.joinToString(", ")} 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        val conn = connection ?: return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params.toMap())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
        return conn.createNamedStatement(sql, params.toMap())
            .execute()
            .awaitSingle()
            .map(rowMapper)
            .awaitFirstOrNull()
    }

    // Search orders
    suspend fun searchOrders(
        query: String,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE (order_number ILIKE :query 
                   OR email ILIKE :query 
                   OR notes ILIKE :query) 
            $statusFilter
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Get orders by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Order> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE placed_at BETWEEN :startDate AND :endDate 
            $statusFilter
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Get order statistics
    suspend fun getOrderStats(
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): OrderStats {
        val dateFilter = if (startDate != null && endDate != null) {
            "WHERE placed_at BETWEEN :startDate AND :endDate"
        } else ""

        val sql = """
            SELECT 
                COUNT(*) as total_orders,
                COALESCE(SUM(total_amount), 0) as total_revenue,
                COALESCE(AVG(total_amount), 0) as average_order_value,
                COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending,
                COUNT(CASE WHEN status = 'confirmed' THEN 1 END) as confirmed,
                COUNT(CASE WHEN status = 'processing' THEN 1 END) as processing,
                COUNT(CASE WHEN status = 'shipped' THEN 1 END) as shipped,
                COUNT(CASE WHEN status = 'delivered' THEN 1 END) as delivered,
                COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled
            FROM $tableName 
            $dateFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("startDate" to startDate, "endDate" to endDate))
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    OrderStats(
                        totalOrders = row.get("total_orders", Long::class.java)!!.toInt(),
                        totalRevenue = row.get("total_revenue", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        averageOrderValue = row.get("average_order_value", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        pending = row.get("pending", Long::class.java)!!.toInt(),
                        confirmed = row.get("confirmed", Long::class.java)!!.toInt(),
                        processing = row.get("processing", Long::class.java)!!.toInt(),
                        shipped = row.get("shipped", Long::class.java)!!.toInt(),
                        delivered = row.get("delivered", Long::class.java)!!.toInt(),
                        cancelled = row.get("cancelled", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: OrderStats(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, 0, 0)
        }
    }

    // Generate order number
    suspend fun generateOrderNumber(): String {
        val prefix = "ORD"
        val date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val sql = """
            SELECT COUNT(*) + 1 as next_sequence 
            FROM $tableName 
            WHERE DATE(placed_at) = CURRENT_DATE
        """.trimIndent()

        val sequence = connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("next_sequence", Long::class.java)!!.toInt() }
                .awaitFirstOrNull() ?: 1
        }

        return "$prefix-$date-${sequence.toString().padStart(4, '0')}"
    }
}

@Serializable
data class OrderStats(
    val totalOrders: Int,
    @Contextual
    val totalRevenue: BigDecimal,
    @Contextual
    val averageOrderValue: BigDecimal,
    val pending: Int,
    val confirmed: Int,
    val processing: Int,
    val shipped: Int,
    val delivered: Int,
    val cancelled: Int
)

/**
 * Response object for order confirmation
 */
@Serializable
data class OrderConfirmation(
    val order: Order,
    val orderItems: List<OrderItem>,
    val payment: Payment,
    val shippingAddress: Address,
    val reservations: List<InventoryReservation>
)

/**
 * Comprehensive order details
 */
@Serializable
data class OrderDetails(
    val order: Order,
    val items: List<OrderItem>,
    val shippingAddress: Address?,
    val billingAddress: Address?,
    val payments: List<Payment>,
    val statusHistory: List<OrderStatusHistory>,
    val shipments: List<Shipment>
)