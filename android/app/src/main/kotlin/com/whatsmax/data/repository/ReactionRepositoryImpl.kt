package com.whatsmax.data.repository

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.dto.ReactionRequest
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.ReactionRepository
import com.whatsmax.domain.repository.ReactionSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ReactionRepository {

    override suspend fun getReactions(entityType: String, entityId: String): Result<ReactionSummary> =
        safeApiCall { apiService.getReactions(entityType, entityId).bodyOrThrow().toModel() }

    override suspend fun setReaction(entityType: String, entityId: String, emoji: String): Result<ReactionSummary> =
        safeApiCall { apiService.setReaction(entityType, entityId, ReactionRequest(emoji)).bodyOrThrow().toModel() }

    override suspend fun removeReaction(entityType: String, entityId: String): Result<ReactionSummary> =
        safeApiCall { apiService.removeReaction(entityType, entityId).bodyOrThrow().toModel() }
}

private fun com.whatsmax.data.remote.dto.ReactionSummaryDto.toModel() =
    ReactionSummary(reactions = reactions, myReaction = myReaction)
