package com.whatsmax.domain.usecase.reaction

import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.ReactionRepository
import com.whatsmax.domain.repository.ReactionSummary
import javax.inject.Inject

class GetReactionsUseCase @Inject constructor(private val repo: ReactionRepository) {
    suspend operator fun invoke(entityType: String, entityId: String): Result<ReactionSummary> =
        repo.getReactions(entityType, entityId)
}

class SetReactionUseCase @Inject constructor(private val repo: ReactionRepository) {
    suspend operator fun invoke(entityType: String, entityId: String, emoji: String): Result<ReactionSummary> =
        repo.setReaction(entityType, entityId, emoji)
}

class RemoveReactionUseCase @Inject constructor(private val repo: ReactionRepository) {
    suspend operator fun invoke(entityType: String, entityId: String): Result<ReactionSummary> =
        repo.removeReaction(entityType, entityId)
}
