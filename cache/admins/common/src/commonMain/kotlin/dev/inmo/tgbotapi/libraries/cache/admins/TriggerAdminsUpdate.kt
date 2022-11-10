package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.chat.ExtendedBot
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember

internal suspend fun TelegramBot.updateAdmins(
    chatId: IdChatIdentifier,
    repo: DefaultAdminsCacheAPIRepo,
    botInfo: ExtendedBot? = null
): List<AdministratorChatMember> {
    val botInfo = botInfo ?: getMe()
    val admins = getChatAdministrators(chatId).filter {
        botInfo.id != it.user.id
    }
    repo.setChatAdmins(chatId, admins)
    return admins
}
