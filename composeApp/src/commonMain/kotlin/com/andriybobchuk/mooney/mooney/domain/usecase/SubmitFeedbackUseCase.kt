package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackKind
import com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackRepository

class SubmitFeedbackUseCase(
    private val repository: FeedbackRepository
) {
    suspend operator fun invoke(kind: FeedbackKind, body: String): Boolean =
        repository.submit(kind, body)
}
