package org.example.services

import org.example.data.cache.AddressCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Address

@Component
class AddressService @Inject constructor(
    private val addressCache: AddressCache
) {
    companion object {
        private const val MAX_ADDRESSES_PER_USER = 20
    }

    suspend fun listForUser(userId: String): List<Address> = addressCache.findByUserId(userId)

    suspend fun getDefault(userId: String): Address? = addressCache.findDefaultByUserId(userId)

    suspend fun get(id: String): Address? = addressCache.read(id)

    suspend fun create(userId: String, address: Address): Address {
        val currentCount = addressCache.countByUser(userId)
        require(currentCount <= MAX_ADDRESSES_PER_USER) {
            "User $userId already has the maximum of $MAX_ADDRESSES_PER_USER addresses"
        }
        return addressCache.create(address)
    }

    suspend fun update(userId: String, addressId: String, address: Address): Address? {
        requireOwnership(addressId, userId)
        return addressCache.update(addressId, address)
    }

    suspend fun delete(userId: String, addressId: String): Boolean {
        requireOwnership(addressId, userId)
        return addressCache.delete(addressId)
    }

    suspend fun setAsDefault(userId: String, addressId: String): Address? {
        requireOwnership(addressId, userId)
        return addressCache.setDefault(userId, addressId)
    }

    suspend fun search(userId: String, query: String): List<Address> =
        addressCache.searchByUser(userId, query)

    suspend fun bulkCreate(userId: String, addresses: List<Address>): List<Address> {
        val currentCount = addressCache.countByUser(userId)
        require(currentCount + addresses.size <= MAX_ADDRESSES_PER_USER) {
            "User $userId already has the maximum of $MAX_ADDRESSES_PER_USER addresses"
        }
        return addressCache.bulkCreate(addresses)
    }

    private suspend fun requireOwnership(addressId: String, userId: String) {
        require(addressCache.isOwnedBy(addressId, userId)){
            "Address $addressId does not belong to the requesting user"
        }
    }
}