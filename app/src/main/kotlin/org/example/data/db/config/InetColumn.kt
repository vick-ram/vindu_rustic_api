package org.example.data.db.config

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject
import java.net.InetAddress

class InetColumn : ColumnType<InetAddress>() {
    override fun sqlType(): String = "INET"

    override fun valueFromDB(value: Any): InetAddress? {
        return when (value) {
            is InetAddress -> value
            is PGobject -> InetAddress.getByName(value.value)
            is String -> InetAddress.getByName(value)
            else -> null
        }
    }

    override fun notNullValueToDB(value: InetAddress): Any {
        return PGobject().apply {
            type = "inet"
            this.value = value.hostAddress
        }
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value is InetAddress) {
            val pgObject = PGobject().apply {
                type = "inet"
                this.value = value.hostAddress
            }
            stmt.set(index, pgObject, this)
        } else {
            super.setParameter(stmt, index, value)
        }
    }
}

fun Table.inet(name: String): Column<InetAddress> = registerColumn(name, InetColumn())