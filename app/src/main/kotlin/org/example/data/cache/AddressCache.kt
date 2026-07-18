package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.mappers.AddressMapper
import org.example.data.repo.AddressRepository
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.identity.Address

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Injectable
class AddressCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    addressMapper: AddressMapper,
    private val addressRepository: AddressRepository
) : CrudCache<Address, String>(
    redis = redis,
    delegate = addressRepository,
    getId = { address -> addressMapper.getId(address) as String },
    serializer = Address.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "addressCache"
        override val ttl: Long = 300
    }
) {
    suspend fun findByUserId(userId: String): List<Address> = addressRepository.findByUserId(userId)
    suspend fun findDefaultByUserId(userId: String): Address? = addressRepository.findDefaultByUserId(userId)
    suspend fun searchByUser(userId: String, query: String): List<Address> =
        addressRepository.searchByUser(userId, query)
    suspend fun setDefault(userId: String, addressId: String): Address? {
        val result = addressRepository.setDefault(userId, addressId)
        if (result != null) {
            putInCache(result.id, result)
            invalidateCollectionCaches()
        }
        return result
    }
    suspend fun countByUser(userId: String): Long {
        return addressRepository.countByUser(userId)
    }
    suspend fun isOwnedBy(userId: String, addressId: String): Boolean {
        return addressRepository.isOwnedBy(userId, addressId)
    }

    suspend fun bulkCreate(addresses: List<Address>): List<Address> {
        val created = addressRepository.bulkCreate(addresses)
        created.forEach { putInCache(it.id, it) }
        if (created.isNotEmpty()) invalidateCollectionCaches()
        return created
    }
}