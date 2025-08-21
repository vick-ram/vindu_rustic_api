package org.example.data.mappers

import org.example.data.db.entities.AddressEntity
import org.example.data.db.entities.DiscountEntity
import org.example.data.db.entities.OrderEntity
import org.example.data.db.entities.OrderItemEntity
import org.example.domain.models.Address
import org.example.domain.models.Discount
import org.example.domain.models.Order
import org.example.domain.models.OrderItem
import org.example.domain.repo.EntityMapper

object OrderItemMapper: EntityMapper<OrderItemEntity, OrderItem, String> {
    override fun toModel(entity: OrderItemEntity): OrderItem {
        return OrderItem(
            id = entity.id.value,
            orderId = entity.order.id.value,
            productId = entity.product.id.value,
            quantity = entity.quantity,
            unitPrice = entity.unitPrice,
            totalPrice = entity.totalPrice
        )
    }

    override fun toEntity(
        model: OrderItem,
        entity: OrderItemEntity
    ): OrderItemEntity {
        val orderEntity = entity.order
        val orderModel = OrderMapper.toModel(orderEntity)

        val productEntity = entity.product
        val productModel = ProductMapper.toModel(productEntity)

        entity.order = OrderMapper.toEntity(orderModel, orderEntity)
        entity.product = ProductMapper.toEntity(productModel, productEntity)
        entity.quantity = model.quantity
        entity.unitPrice = model.unitPrice
        entity.totalPrice = model.totalPrice

        return entity
    }
}

object AddressMapper: EntityMapper<AddressEntity, Address, String> {
    override fun toModel(entity: AddressEntity): Address {
        return Address(
            id = entity.id.value,
            fullName = entity.fullName,
            phone = entity.phone,
            email = entity.email,
            street = entity.street,
            county = entity.county,
            postalCode = entity.postalCode
        )
    }

    override fun toEntity(
        model: Address,
        entity: AddressEntity
    ): AddressEntity {
        entity.fullName = model.fullName
        entity.phone = model.phone
        entity.email = model.email
        entity.street = model.street
        entity.county = model.county
        entity.postalCode = model.postalCode

        return entity
    }
}

object DiscountMapper: EntityMapper<DiscountEntity, Discount, String> {
    override fun toModel(entity: DiscountEntity): Discount {
        return Discount(
            id = entity.id.value,
            name = entity.name,
            description = entity.description,
            type = entity.type,
            value = entity.value,
            code = entity.code,
            appliedTo = entity.appliedTo,
            minimumOrderAmount = entity.minimumOrderAmount,
            startDate = entity.startDate,
            endDate = entity.endDate,
            maxUses = entity.maxUses,
            currentUses = entity.currentUses,
            isActive = entity.isActive,
        )
    }

    override fun toEntity(
        model: Discount,
        entity: DiscountEntity
    ): DiscountEntity {
        entity.name = model.name
        entity.description = model.description
        entity.type = model.type
        entity.value = model.value
        entity.code = model.code
        entity.appliedTo = model.appliedTo
        entity.minimumOrderAmount = model.minimumOrderAmount
        entity.startDate = model.startDate
        entity.endDate = model.endDate
        entity.maxUses = model.maxUses
        entity.currentUses = model.currentUses
        entity.isActive = model.isActive

        return entity
    }
}

object OrderMapper: EntityMapper<OrderEntity, Order, String> {
    override fun toModel(entity: OrderEntity): Order {
        return Order(
            id = entity.id.value,
            userId = entity.user.id.value,
            orderNumber = entity.orderNumber,
            items = entity.orderItems.map { OrderItemMapper.toModel(it) },
            status = entity.status,
            paymentStatus = entity.paymentStatus,
            shippingAddress = AddressMapper.toModel(entity.shippingAddress),
            discount = entity.discount?.let { DiscountMapper.toModel(it) },
            shippingFee = entity.shippingFee,
            totalAmount = entity.totalAmount,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(
        model: Order,
        entity: OrderEntity
    ): OrderEntity {
//        entity.user = model.userId
        entity.orderNumber = model.orderNumber
        entity.status = model.status
        entity.paymentStatus = model.paymentStatus
        entity.shippingAddress = AddressMapper.toEntity(model.shippingAddress, entity.shippingAddress)
        entity.discount = model.discount?.let { DiscountMapper.toEntity(it, entity.discount ?: DiscountEntity.new { }) }
        entity.shippingFee = model.shippingFee
        entity.totalAmount = model.totalAmount
        entity.notes = model.notes
        entity.createdAt = model.createdAt
        entity.updatedAt = model.updatedAt

        return entity
    }
}