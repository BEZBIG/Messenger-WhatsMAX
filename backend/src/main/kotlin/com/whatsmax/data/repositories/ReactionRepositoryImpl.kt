/** Реализация ReactionRepository: upsert реакций через delete+insert. */
package com.whatsmax.data.repositories

import com.whatsmax.data.database.DatabaseFactory.dbQuery
import com.whatsmax.data.database.tables.ReactionsTable
import com.whatsmax.domain.models.ReactionSummary
import com.whatsmax.domain.repositories.ReactionRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class ReactionRepositoryImpl : ReactionRepository {

    override suspend fun setReaction(
        entityType: String, entityId: UUID, userId: String, emoji: String
    ): ReactionSummary = dbQuery {
        ReactionsTable.deleteWhere {
            (ReactionsTable.entityType eq entityType) and
            (ReactionsTable.entityId   eq entityId)  and
            (ReactionsTable.userId     eq userId)
        }
        ReactionsTable.insert {
            it[ReactionsTable.entityType] = entityType
            it[ReactionsTable.entityId]   = entityId
            it[ReactionsTable.userId]     = userId
            it[ReactionsTable.emoji]      = emoji
        }
        buildSummary(entityType, entityId, userId)
    }

    override suspend fun removeReaction(
        entityType: String, entityId: UUID, userId: String
    ): ReactionSummary = dbQuery {
        ReactionsTable.deleteWhere {
            (ReactionsTable.entityType eq entityType) and
            (ReactionsTable.entityId   eq entityId)  and
            (ReactionsTable.userId     eq userId)
        }
        buildSummary(entityType, entityId, userId)
    }

    override suspend fun getReactions(
        entityType: String, entityId: UUID, userId: String?
    ): ReactionSummary = dbQuery {
        buildSummary(entityType, entityId, userId)
    }

    private fun buildSummary(entityType: String, entityId: UUID, userId: String?): ReactionSummary {
        val rows = ReactionsTable.select {
            (ReactionsTable.entityType eq entityType) and (ReactionsTable.entityId eq entityId)
        }.toList()

        val counts = rows.groupingBy { it[ReactionsTable.emoji] }.eachCount()
        val myReaction = userId?.let { uid ->
            rows.firstOrNull { it[ReactionsTable.userId] == uid }?.get(ReactionsTable.emoji)
        }
        return ReactionSummary(reactions = counts, myReaction = myReaction)
    }
}
