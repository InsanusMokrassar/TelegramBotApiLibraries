package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.message.abstracts.*

suspend inline fun AdminsCacheAPI.isAdmin(message: Message) = when (message) {
    is CommonGroupContentMessage<*> -> isAdmin(message.chat.id, message.user.id)
    is AnonymousGroupContentMessage<*> -> true
    else -> false
}

suspend inline fun AdminsCacheAPI.verifyMessageFromAdmin(message: Message) = isAdmin(message)

suspend inline fun <R : Any> AdminsCacheAPI.doIfAdmin(
    chatId: ChatId,
    userId: UserId,
    block: () -> R
) = if(isAdmin(chatId, userId)) {
    block()
} else {
    null
}

suspend inline fun <R : Any> AdminsCacheAPI.doIfAdmin(
    message: Message,
    block: () -> R
) = if(isAdmin(message)) {
    block()
} else {
    null
}

suspend inline fun <R> ContentMessage<*>.doIfAdmin(adminsCacheAPI: AdminsCacheAPI, block: () -> R): R? {
    val verified = adminsCacheAPI.isAdmin(this)
    return if (verified) {
        block()
    } else {
        null
    }
}

suspend inline fun <R> ContentMessage<*>.doAfterVerification(adminsCacheAPI: AdminsCacheAPI, block: () -> R) = doIfAdmin(adminsCacheAPI, block)
