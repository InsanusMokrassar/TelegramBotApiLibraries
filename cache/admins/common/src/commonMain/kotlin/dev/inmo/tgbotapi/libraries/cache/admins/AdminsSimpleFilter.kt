package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.abstracts.FromUser
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.SimpleFilter
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.message.abstracts.Message

fun AdminsChecker(
    adminsCacheAPI: AdminsCacheAPI
): SimpleFilter<Pair<IdChatIdentifier, UserId>> = SimpleFilter {
    adminsCacheAPI.isAdmin(it.first, it.second)
}

fun <T> AdminsChecker(
    adminsCacheAPI: AdminsCacheAPI,
    mapper: (T) -> Pair<IdChatIdentifier, UserId>
): SimpleFilter<T> {
    val baseChecker = AdminsChecker(adminsCacheAPI)

    return SimpleFilter<T> {
        baseChecker(mapper(it))
    }
}

fun MessageAdminsChecker(
    adminsCacheAPI: AdminsCacheAPI
) = SimpleFilter<Message> {
    adminsCacheAPI.isAdmin(it)
}

fun AdminsChecker(
    adminsCacheAPI: AdminsCacheAPI,
    chatId: IdChatIdentifier
) = SimpleFilter<FromUser> {
    adminsCacheAPI.isAdmin(chatId, it.from.id)
}
