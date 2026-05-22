package com.andriybobchuk.mooney.mooney.data.feedback

import com.andriybobchuk.mooney.APP_VERSION
import com.andriybobchuk.mooney.core.data.Secrets
import com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackKind
import com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

/**
 * Writes user feedback to the `feedback` collection in Firestore via the
 * REST API. Chose REST over the native SDKs because:
 *
 *  - It works in commonMain without any platform-specific code or Swift bridge
 *  - The existing Ktor HttpClient covers networking on both platforms
 *  - Public, unauthenticated writes are fine for a feedback inbox as long as
 *    Firestore rules limit writes to this single collection with size caps
 *
 * Required Firestore rules (set in Firebase console):
 * ```
 * match /feedback/{doc} {
 *   allow create: if request.resource.data.body.size() < 4000 &&
 *                    request.resource.data.kind in ["GENERAL","BUG","FEATURE","WIDGET"];
 *   allow read, update, delete: if false;
 * }
 * ```
 */
class FirestoreFeedbackRepository(
    private val httpClient: HttpClient
) : FeedbackRepository {

    override suspend fun submit(kind: FeedbackKind, body: String): Boolean {
        if (body.isBlank()) return false
        return try {
            val response: HttpResponse = httpClient.post(endpoint()) {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Accept, ContentType.Application.Json.toString()) }
                setBody(buildPayload(kind, body))
            }
            response.status.isSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            false
        }
    }

    private fun endpoint(): String =
        "https://firestore.googleapis.com/v1/projects/${Secrets.FIREBASE_PROJECT_ID}/databases/(default)/documents/feedback"

    private fun buildPayload(kind: FeedbackKind, body: String): FirestorePayload {
        // Firestore's REST API takes a typed-field map. We trim the body and
        // cap it server-side via rules too; the cap here is defensive.
        val trimmed = body.trim().take(MAX_BODY_LENGTH)
        return FirestorePayload(
            fields = buildJsonObject {
                put("kind", stringField(kind.name))
                put("body", stringField(trimmed))
                put("appVersion", stringField(APP_VERSION))
                put("submittedAt", stringField(Clock.System.now().toString()))
            }
        )
    }

    private fun stringField(value: String): JsonObject = buildJsonObject {
        put("stringValue", value)
    }

    companion object {
        private const val MAX_BODY_LENGTH = 4000
    }
}

@Serializable
private data class FirestorePayload(val fields: JsonObject)
