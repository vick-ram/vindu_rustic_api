package org.example.data.mappers

import org.example.domain.models.catalog.Category
import org.example.domain.models.catalog.Product
import org.example.domain.models.catalog.ProductMedia
import org.example.domain.models.catalog.ProductReview
import org.example.domain.models.catalog.ProductTag
import org.example.domain.models.catalog.ProductVariant
import org.example.domain.models.catalog.Tag
import org.example.domain.models.content.Page
import org.example.domain.models.content.Setting
import org.example.domain.models.customization.CustomProductAttachment
import org.example.domain.models.customization.CustomProductQuote
import org.example.domain.models.customization.CustomProductRequest
import org.example.domain.models.identity.Address
import org.example.domain.models.identity.Role
import org.example.domain.models.identity.Session
import org.example.domain.models.identity.User
import org.example.domain.models.identity.UserRole
import org.example.domain.models.inventory.Inventory
import org.example.domain.models.inventory.InventoryMovement
import org.example.domain.models.inventory.InventoryReservation
import org.example.domain.models.inventory.Warehouse
import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import org.example.domain.models.payments.Payment
import org.example.domain.models.payments.PaymentTransaction
import org.example.domain.models.payments.Refund
import org.example.domain.models.production.ProductionJob
import org.example.domain.models.production.ProductionUpdate
import org.example.domain.models.sales.CartItem
import org.example.domain.models.sales.Order
import org.example.domain.models.sales.OrderItem
import org.example.domain.models.sales.OrderStatusHistory
import org.example.domain.models.sales.ShoppingCart
import org.example.domain.models.sales.Wishlist
import org.example.domain.models.sales.WishlistItem
import org.example.domain.models.shipping.Shipment
import org.example.domain.models.shipping.ShipmentEvent
import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.example.domain.models.support.Participant
import org.example.domain.models.support.SupportTicket
import org.example.domain.models.support.TicketMessage
import org.example.domain.models.system.AuditLogs
import org.example.domain.models.system.DeviceToken
import org.example.domain.models.system.Notification
import org.example.domain.models.system.OutboxEvent
import org.koin.core.annotation.Single

@Single
class AddressMapper : BaseRowMapper<Address>(Address.serializer(), { it.id })

@Single
class AuditLogsMapper : BaseRowMapper<AuditLogs>(AuditLogs.serializer(), { it.id })

@Single
class CategoryMapper : BaseRowMapper<Category>(Category.serializer(), { it.id })

@Single
class ConversationMapper : BaseRowMapper<Conversation>(Conversation.serializer(), { it.id })

@Single
class ParticipantMapper : BaseRowMapper<Participant>(Participant.serializer(), { it.id })

@Single
class MessageMapper : BaseRowMapper<Message>(Message.serializer(), { it.id })

@Single
class CouponMapper : BaseRowMapper<Coupon>(Coupon.serializer(), { it.id })

@Single
class CouponUsageMapper : BaseRowMapper<CouponUsage>(CouponUsage.serializer(), { it.id })

@Single
class CustomProductAttachmentMapper : BaseRowMapper<CustomProductAttachment>(CustomProductAttachment.serializer(), { it.id })

@Single
class CustomProductQuoteMapper : BaseRowMapper<CustomProductQuote>(CustomProductQuote.serializer(), { it.id })

@Single
class CustomProductRequestMapper : BaseRowMapper<CustomProductRequest>(CustomProductRequest.serializer(), { it.id })

@Single
class InventoryMapper : BaseRowMapper<Inventory>(Inventory.serializer(), { it.id })

@Single
class InventoryMovementMapper : BaseRowMapper<InventoryMovement>(InventoryMovement.serializer(), { it.id })

@Single
class InventoryReservationMapper : BaseRowMapper<InventoryReservation>(InventoryReservation.serializer(), { it.id })

@Single
class NotificationMapper : BaseRowMapper<Notification>(Notification.serializer(), { it.id })

@Single
class DeviceTokenMapper : BaseRowMapper<DeviceToken>(DeviceToken.serializer(), { it.id })

@Single
class OrderMapper : BaseRowMapper<Order>(Order.serializer(), { it.id })

@Single
class OrderItemMapper : BaseRowMapper<OrderItem>(OrderItem.serializer(), { it.id })

@Single
class OrderStatusHistoryMapper : BaseRowMapper<OrderStatusHistory>(OrderStatusHistory.serializer(), { it.id })

@Single
class OutboxEventMapper : BaseRowMapper<OutboxEvent>(OutboxEvent.serializer(), { it.id })

@Single
class PageMapper : BaseRowMapper<Page>(Page.serializer(), { it.id })

@Single
class PaymentMapper : BaseRowMapper<Payment>(Payment.serializer(), { it.id })

@Single
class PaymentTransactionMapper : BaseRowMapper<PaymentTransaction>(PaymentTransaction.serializer(), { it.id })

@Single
class ProductionJobMapper : BaseRowMapper<ProductionJob>(ProductionJob.serializer(), { it.id })

@Single
class ProductionUpdateMapper : BaseRowMapper<ProductionUpdate>(ProductionUpdate.serializer(), { it.id })

@Single
class ProductMapper : BaseRowMapper<Product>(Product.serializer(), { it.id })

@Single
class ProductMediaMapper : BaseRowMapper<ProductMedia>(ProductMedia.serializer(), { it.id })

@Single
class ProductReviewMapper : BaseRowMapper<ProductReview>(ProductReview.serializer(), { it.id })

@Single
class ProductTagMapper : BaseRowMapper<ProductTag>(ProductTag.serializer(), { model -> model.productId to model.tagId })

@Single
class ProductVariantMapper : BaseRowMapper<ProductVariant>(ProductVariant.serializer(), { it.id })

@Single
class RefundMapper : BaseRowMapper<Refund>(Refund.serializer(), { it.id })

@Single
class RoleMapper : BaseRowMapper<Role>(Role.serializer(), { it.id })

@Single
class UserRoleMapper : BaseRowMapper<UserRole>(UserRole.serializer(), { it.userId to it.roleId })

@Single
class SessionMapper : BaseRowMapper<Session>(Session.serializer(), { it.id })

@Single
class SettingsMapper : BaseRowMapper<Setting>(Setting.serializer(), { it.id })

@Single
class ShipmentEventMapper : BaseRowMapper<ShipmentEvent>(ShipmentEvent.serializer(), { it.id })

@Single
class ShipmentMapper : BaseRowMapper<Shipment>(Shipment.serializer(), { it.id })

@Single
class ShoppingCartMapper : BaseRowMapper<ShoppingCart>(ShoppingCart.serializer(), { it.id })

@Single
class SupportTicketMapper : BaseRowMapper<SupportTicket>(SupportTicket.serializer(), { it.id })

@Single
class TagMapper : BaseRowMapper<Tag>(Tag.serializer(), { it.id })

@Single
class TicketMessageMapper : BaseRowMapper<TicketMessage>(TicketMessage.serializer(), { it.id })

@Single
class UserMapper : BaseRowMapper<User>(User.serializer(), { it.id })

@Single
class WarehouseMapper : BaseRowMapper<Warehouse>(Warehouse.serializer(), { it.id })

@Single
class CartItemMapper : BaseRowMapper<CartItem>(CartItem.serializer(), {it.id})

@Single
class WishlistMapper : BaseRowMapper<Wishlist>(Wishlist.serializer(), { it.id })

@Single
class WishlistItemMapper : BaseRowMapper<WishlistItem>(WishlistItem.serializer(), { it.id })