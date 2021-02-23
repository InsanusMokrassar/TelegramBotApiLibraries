package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.message.abstracts.*

suspend fun AdminsCacheAPI.userIsAdmin(chatId: ChatId, userId: UserId) = this.isAdmin()

suspend fun AdminsCacheAPI.verifyMessageFromAdmin(message: ContentMessage<*>) = when (message) {
    is CommonGroupContentMessage<*> -> isAdmin(message.chat.id, message.user.id)
    is AnonymousGroupContentMessage<*> -> true
    else -> false
}

suspend fun <R> ContentMessage<*>.doAfterVerification(adminsCacheAPI: AdminsCacheAPI, block: suspend () -> R): R? {
    val verified = adminsCacheAPI.verifyMessageFromAdmin(this)
    return if (verified) {
        block()
    } else {
        null
    }
}
