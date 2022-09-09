package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.ChatMemberUpdatedFilterByChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.SimpleFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.marker_factories.ByChatChatMemberUpdatedMarkerFactory
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.marker_factories.MarkerFactory
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.member.ChatMemberUpdated
import dev.inmo.tgbotapi.types.update.abstracts.Update

suspend fun BehaviourContext.activateAdminsChangesListening(
    repo: DefaultAdminsCacheAPIRepo,
    initialFilter: SimpleFilter<ChatMemberUpdated>? = null,
    markerFactory: MarkerFactory<ChatMemberUpdated, Any> = ByChatChatMemberUpdatedMarkerFactory
) {
    val me = getMe()
    onChatMemberUpdated(initialFilter, markerFactory = markerFactory) {
        when {
            it.oldChatMemberState is AdministratorChatMember && it.newChatMemberState !is AdministratorChatMember ||
            it.newChatMemberState is AdministratorChatMember && it.oldChatMemberState !is AdministratorChatMember -> {
                updateAdmins(
                    it.chat.id,
                    repo,
                    me
                )
            }
        }
    }
}
