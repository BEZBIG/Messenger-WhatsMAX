/** PostgreSQL + HikariCP + Exposed: создание схемы и suspend-транзакции. */
package com.whatsmax.data.database

import com.whatsmax.data.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {

    fun init(url: String, driver: String, user: String, password: String, maxPool: Int) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl         = url
            driverClassName = driver
            username        = user
            this.password   = password
            maximumPoolSize = maxPool
            isAutoCommit    = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                ChatsTable,
                ChatMembersTable,
                MessagesTable,
                MessageReadStatusTable,
                ChannelsTable,
                ChannelMembersTable,
                ChannelMessagesTable,
                ChannelCommentsTable,
                ChannelMessageViewsTable,
                FilesTable,
                ReactionsTable
            )
            UsersTable.update({ UsersTable.isOnline eq true }) {
                it[UsersTable.isOnline] = false
            }
            createTrigramIndexes()
        }
    }

    private fun createTrigramIndexes() {
        val log = LoggerFactory.getLogger(DatabaseFactory::class.java)
        val conn = TransactionManager.current().connection
        try {
            conn.prepareStatement("CREATE EXTENSION IF NOT EXISTS pg_trgm", false).executeUpdate()
            val statements = listOf(
                "CREATE INDEX IF NOT EXISTS idx_users_username_trgm ON users USING gin (username gin_trgm_ops)",
                "CREATE INDEX IF NOT EXISTS idx_users_displayname_trgm ON users USING gin (display_name gin_trgm_ops)",
                "CREATE INDEX IF NOT EXISTS idx_channels_handle_trgm ON channels USING gin (handle gin_trgm_ops)",
                "CREATE INDEX IF NOT EXISTS idx_channels_name_trgm ON channels USING gin (name gin_trgm_ops)"
            )
            statements.forEach { conn.prepareStatement(it, false).executeUpdate() }
            log.info("pg_trgm indexes ensured")
        } catch (e: Exception) {
            log.warn("Could not create pg_trgm indexes: ${e.message}. Search will use seq scan.")
        }
    }

    /** Выполняет блок в suspend-транзакции на IO-диспетчере. */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
