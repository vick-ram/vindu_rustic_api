package org.example.domain.repo

import org.example.domain.models.identity.Address

interface AddressRepository: CrudRepository<Address, String> {
}