package com.spark11e.bot.service

import com.spark11e.bot.model.User
import com.spark11e.bot.telegram.BotCommand
import com.spark11e.bot.telegram.BotCommands
import com.spark11e.bot.config.BotProperty
import com.spark11e.bot.service.api.HoyoverseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

private val botScope = CoroutineScope(Dispatchers.IO)
@Component
open class TelegramBotService(
    private val botProperty: BotProperty,
    private val hoyoverseService: HoyoverseService,
    private val userService: UserService,
    private val resourceLoader: ResourceLoader
) : TelegramLongPollingBot() {
    private object ProfilePhotos {
        val map = mapOf(
            "art1" to "red.jpg",
            "art2" to "yellow.jpg",
            "art3" to "green.jpg"
        )
    }

    /** Standard child class setup
        to launch a Telegram bot
     */
    private final val log = LoggerFactory.getLogger(TelegramBotService::class.java)

    override fun getBotToken() = botProperty.token

    override fun getBotUsername() = botProperty.username

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId.toString()
            val messageText = message.text

            val userId: Long = message.from.id
            val userNickname: String = message.from.userName ?: "Анонимный пользователь $userId"
            val currentUser = userService.createOrLoadUser(userId, userNickname)

            if (messageText.startsWith(prefix = "/")) {
                botScope.launch {
                    handleCommand(chatId = chatId, commandText = messageText, user = currentUser)
                }
            } else {
                sendMessage(chatId, "${currentUser.nickname}, Вы сказали: \"$messageText\". Я пока умею только обрабатывать команды.")
            }
        }
        else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.callbackQuery)
        }
    }

    private final fun handleCallbackQuery(callbackQuery: CallbackQuery) {
        val data = callbackQuery.data
        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId.toString()
        val messageId = callbackQuery.message.messageId

        try {
            if (data.startsWith("SET_PHOTO:")) {
                val photoName = data.substringAfter("SET_PHOTO:")

                userService.updateProfilePhoto(userId, photoName)

                val displayName = ProfilePhotos.map.entries.find { it.value == photoName }?.key ?: "Выбранное"
                val answerText = "✅ Фото профиля обновлено на '$displayName'!"

                val editMessage = EditMessageText()
                editMessage.chatId = chatId
                editMessage.messageId = messageId
                editMessage.text = answerText
                execute(editMessage)
            }
        } catch (e: Exception) {
            log.error("Ошибка обработки CallbackQuery: ${e.message}")
            sendMessage(chatId, "Произошла ошибка при выборе фото.")
        } finally {
            try {
                execute(AnswerCallbackQuery(callbackQuery.id))
            } catch (e: TelegramApiException) {
                log.error("Ошибка при отправке AnswerCallbackQuery: ${e.message}")
            }
        }
    }

    private final fun escapeMarkdownV1(text: String): String {
        return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("`", "\\`")
            .replace("[", "\\[")
    }

    private final fun sendMessage(chatId: String, text: String) {
        val message = SendMessage(chatId, text)
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            log.error("Ошибка при отправке сообщения в чат $chatId: ${e.message}")
        }
    }

    private final fun sendAccountPhoto(chatId: String, text: String, photoName: String?) {
        if (photoName == null) {
            sendMessage(chatId, "$text\n\n(Вы можете выбрать фото профиля командой /setphoto)")
            return
        }

        try {
            val resource = resourceLoader.getResource("classpath:photo/$photoName")

            if (!resource.exists()) {
                log.warn("Фото $photoName не найдено в resources, отправляем только текст.")
                sendMessage(chatId, text)
                return
            }

            val sendPhoto = SendPhoto()
            sendPhoto.chatId = chatId
            sendPhoto.caption = text

            sendPhoto.photo = InputFile(resource.inputStream, photoName)

            execute(sendPhoto)

        } catch (e: Exception) {
            log.error("Не удалось загрузить или отправить фото $photoName: ${e.message}")
            sendMessage(chatId, text)
        }
    }

    protected final fun sendPhotoSelectionMenu(chatId: String, text: String) {
        val message = SendMessage(chatId, text)

        val keyboard = InlineKeyboardMarkup()
        val rows = mutableListOf<List<InlineKeyboardButton>>()

        ProfilePhotos.map.forEach { (displayName, fileName) ->
            val button = InlineKeyboardButton()
            button.text = displayName
            button.callbackData = "SET_PHOTO:$fileName"
            rows.add(listOf(button))
        }

        keyboard.keyboard = rows
        message.replyMarkup = keyboard

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            log.error("Ошибка при отправке меню выбора фото: ${e.message}")
        }
    }

    private final suspend fun handleCommand(chatId: String, commandText: String, user: User) {
        val parts = commandText.split("\\s+".toRegex(), 2)
        val command = parts[0].lowercase()
        val arguments = parts.getOrNull(1)

        val botCommand = BotCommands.entries.find { command.startsWith(it.command.lowercase()) }

        when (botCommand) {
            BotCommands.START -> sendMessage(chatId, "Привет! Я Sparkle=). Используйте /help для списка команд.")
            BotCommands.HELP -> sendMessage(chatId, getHelpMessage())
            BotCommands.HSR_STATS -> {
                val responseText = arguments?.let {
                    val targetUid = it.trim()
                    if (targetUid.length == 9 && targetUid.all { char -> char.isDigit() }) {
                        getHsrStatsResponse(targetUid)
                    } else run {
                        "Пж введи корректный UID (9 Цифр)"
                    }
                } ?: "Введите UID после /hoyostats"

                sendMessage(chatId, responseText)
            }

            BotCommands.USER_ACCOUNT ->  {
                val statsText = getUserAccount(user)
                sendAccountPhoto(chatId, statsText, user.profilePhotoName)
            }

            BotCommands.SET_PHOTO -> {
                sendPhotoSelectionMenu(chatId, "Выберите фото для вашего профиля:")
            }

            null -> "Неизвестная команда. Используйте /help."
        }
    }



    @BotCommand("/help")
    public open fun getHelpMessage() : String {
        return "Доступные команды:\n" +
                BotCommands.entries.joinToString("\n") { "${it.command} <- ${it.description}" }
    }


    @BotCommand("/hoyostats")
    public open suspend fun getHsrStatsResponse(uid: String) : String {
        val result = hoyoverseService.fetchHsrStats(uid)

        return result.fold(
            onSuccess = { data ->
                "📊 Статистика игрока ${data.detailInfo.nickname}:\n" +
                        "UID: ${data.uid}\n" +
                        "Уровень (Level): ${data.detailInfo.level}\n" +
                        "Достижений: ${data.detailInfo.recordInfo.achievementCount}"
            },
            onFailure = { error ->
                "❌ Не удалось получить статистику для UID $uid.\n" +
                        "Причина: ${error.message ?: "Неизвестная ошибка API/сети"}"
            }
        )
    }


    @BotCommand("/account")
    public open fun getUserAccount(user: User) : String {
        val dateJoined = user.joinedAt.toLocalDate()
        val safeNickname = escapeMarkdownV1(user.nickname)

        return """
        *Ваша статистика аккаунта:*

        👤 * Никнейм: $safeNickname
        🆔 * ID в Telegram: ${user.id}

        📅 * Зарегистрирован: $dateJoined
    """.trimIndent()

    }

}