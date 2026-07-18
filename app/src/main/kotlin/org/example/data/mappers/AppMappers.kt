package org.example.data.mappers

import org.example.di.Injectable
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

@Injectable
class AddressMapper : BaseRowMapper<Address>(Address.serializer(), { it.id })

@Injectable
class AuditLogsMapper : BaseRowMapper<AuditLogs>(AuditLogs.serializer(), { it.id })

@Injectable
class CategoryMapper : BaseRowMapper<Category>(Category.serializer(), { it.id })

@Injectable
class ConversationMapper : BaseRowMapper<Conversation>(Conversation.serializer(), { it.id })

@Injectable
class ParticipantMapper : BaseRowMapper<Participant>(Participant.serializer(), { it.id })

@Injectable
class MessageMapper : BaseRowMapper<Message>(Message.serializer(), { it.id })

@Injectable
class CouponMapper : BaseRowMapper<Coupon>(Coupon.serializer(), { it.id })

@Injectable
class CouponUsageMapper : BaseRowMapper<CouponUsage>(CouponUsage.serializer(), { it.id })

@Injectable
class CustomProductAttachmentMapper : BaseRowMapper<CustomProductAttachment>(CustomProductAttachment.serializer(), { it.id })

@Injectable
class CustomProductQuoteMapper : BaseRowMapper<CustomProductQuote>(CustomProductQuote.serializer(), { it.id })

@Injectable
class CustomProductRequestMapper : BaseRowMapper<CustomProductRequest>(CustomProductRequest.serializer(), { it.id })

@Injectable
class InventoryMapper : BaseRowMapper<Inventory>(Inventory.serializer(), { it.id })

@Injectable
class InventoryMovementMapper : BaseRowMapper<InventoryMovement>(InventoryMovement.serializer(), { it.id })

@Injectable
class InventoryReservationMapper : BaseRowMapper<InventoryReservation>(InventoryReservation.serializer(), { it.id })

@Injectable
class NotificationMapper : BaseRowMapper<Notification>(Notification.serializer(), { it.id })

@Injectable
class DeviceTokenMapper : BaseRowMapper<DeviceToken>(DeviceToken.serializer(), { it.id })

@Injectable
class OrderMapper : BaseRowMapper<Order>(Order.serializer(), { it.id })

@Injectable
class OrderItemMapper : BaseRowMapper<OrderItem>(OrderItem.serializer(), { it.id })

@Injectable
class OrderStatusHistoryMapper : BaseRowMapper<OrderStatusHistory>(OrderStatusHistory.serializer(), { it.id })

@Injectable
class OutboxEventMapper : BaseRowMapper<OutboxEvent>(OutboxEvent.serializer(), { it.id })

@Injectable
class PageMapper : BaseRowMapper<Page>(Page.serializer(), { it.id })

@Injectable
class PaymentMapper : BaseRowMapper<Payment>(Payment.serializer(), { it.id })

@Injectable
class PaymentTransactionMapper : BaseRowMapper<PaymentTransaction>(PaymentTransaction.serializer(), { it.id })

@Injectable
class ProductionJobMapper : BaseRowMapper<ProductionJob>(ProductionJob.serializer(), { it.id })

@Injectable
class ProductionUpdateMapper : BaseRowMapper<ProductionUpdate>(ProductionUpdate.serializer(), { it.id })

@Injectable
class ProductMapper : BaseRowMapper<Product>(Product.serializer(), { it.id })

@Injectable
class ProductMediaMapper : BaseRowMapper<ProductMedia>(ProductMedia.serializer(), { it.id })

@Injectable
class ProductReviewMapper : BaseRowMapper<ProductReview>(ProductReview.serializer(), { it.id })

@Injectable
class ProductTagMapper : BaseRowMapper<ProductTag>(ProductTag.serializer(), { model -> model.productId to model.tagId })

@Injectable
class ProductVariantMapper : BaseRowMapper<ProductVariant>(ProductVariant.serializer(), { it.id })

@Injectable
class RefundMapper : BaseRowMapper<Refund>(Refund.serializer(), { it.id })

@Injectable
class RoleMapper : BaseRowMapper<Role>(Role.serializer(), { it.id })

@Injectable
class UserRoleMapper : BaseRowMapper<UserRole>(UserRole.serializer(), { it.userId to it.roleId })

@Injectable
class SessionMapper : BaseRowMapper<Session>(Session.serializer(), { it.id })

@Injectable
class SettingsMapper : BaseRowMapper<Setting>(Setting.serializer(), { it.id })

@Injectable
class ShipmentEventMapper : BaseRowMapper<ShipmentEvent>(ShipmentEvent.serializer(), { it.id })

@Injectable
class ShipmentMapper : BaseRowMapper<Shipment>(Shipment.serializer(), { it.id })

@Injectable
class ShoppingCartMapper : BaseRowMapper<ShoppingCart>(ShoppingCart.serializer(), { it.id })

@Injectable
class SupportTicketMapper : BaseRowMapper<SupportTicket>(SupportTicket.serializer(), { it.id })

@Injectable
class TagMapper : BaseRowMapper<Tag>(Tag.serializer(), { it.id })

@Injectable
class TicketMessageMapper : BaseRowMapper<TicketMessage>(TicketMessage.serializer(), { it.id })

@Injectable
class UserMapper : BaseRowMapper<User>(User.serializer(), { it.id })

@Injectable
class WarehouseMapper : BaseRowMapper<Warehouse>(Warehouse.serializer(), { it.id })

@Injectable
class CartItemMapper : BaseRowMapper<CartItem>(CartItem.serializer(), {it.id})

@Injectable
class WishlistMapper : BaseRowMapper<Wishlist>(Wishlist.serializer(), { it.id })

@Injectable
class WishlistItemMapper : BaseRowMapper<WishlistItem>(WishlistItem.serializer(), { it.id })