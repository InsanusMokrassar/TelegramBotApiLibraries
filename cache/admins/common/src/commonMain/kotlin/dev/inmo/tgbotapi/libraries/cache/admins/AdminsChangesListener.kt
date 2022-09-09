package dev.inmo.tgbotapi.libraries.cache.admins

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember

suspend fun BehaviourContext.activateAdminsChangesListening(
    repo: DefaultAdminsCacheAPIRepo
) {
    val me = getMe()
    onChatMemberUpdated {
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
