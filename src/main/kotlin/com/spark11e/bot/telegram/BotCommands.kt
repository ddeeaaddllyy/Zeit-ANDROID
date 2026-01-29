package com.spark11e.bot.telegram

/**
 * Enum class Bot command for providing clearer
 * logic for the Telegram bot's operation and processing of each
 * possible command
 */

enum class BotCommands(
    val command: String,
    val description: String
)
{
    START(command = "/start", description = "bot start"),
    HELP(command = "/help", description = "show all commands"),
    HSR_STATS(command = "/hoyostats", description = "show you hsr account by UID"),
    USER_ACCOUNT(command = "/account", description = "show your account" ),
    SET_PHOTO(command = "/setphoto", description = "here u can set the photo of your profile" )
}