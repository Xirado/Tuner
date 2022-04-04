/*
 * Copyright 2022 Marcel Korzonek and the Tuner contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.xirado.tuner.log

import at.xirado.tuner.Application
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import club.minnced.discord.webhook.WebhookClient
import dev.minn.jda.ktx.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DiscordWebhookAppender : AppenderBase<ILoggingEvent>() {

    companion object {

        const val GREY = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val WHITE = "\u001B[37m"
        const val RESET = "\u001B[0m"

        var client: WebhookClient? = null
        var ready = false
        val lock = ReentrantLock()
        val pendingMessages = mutableListOf<String>()

        fun init(application: Application) {
            if (application.tunerConfig.webhookClient == null) {
                return
            }
            client = application.tunerConfig.webhookClient
            ready = true
            application.coroutineScope.launch {
                var state = 0
                var waitingTime = 0L
                while (true) {
                    val size = lock.withLock { pendingMessages.size }

                    if (size > 0 && state != 1) {
                        state = 1
                        waitingTime = System.currentTimeMillis()
                    }

                    if (state == 1 && System.currentTimeMillis() > waitingTime + 3000) {
                        lock.lock()
                        sendWebhook(pendingMessages)
                        pendingMessages.clear()
                        lock.unlock()
                        state = 0
                    }

                    delay(100)
                }
            }
        }

        fun formatted(event: ILoggingEvent) : String {
            val formatted = event.formattedMessage

            val level = event.level

            val primary = when (level) {
                Level.WARN -> YELLOW
                Level.ERROR -> RED
                Level.INFO -> BLUE
                else -> WHITE
            }

            val builder = StringBuilder("$GREY[$primary${level.levelStr.uppercase()}$GREY] $primary$formatted\n")

            if (event.throwableProxy != null) {
                val proxy = event.throwableProxy
                val result = ThrowableProxyUtil.asString(proxy)

                builder.append(result).append(RESET)
            }
            return builder.toString()
        }

        private suspend fun sendWebhook(logs: List<String>) {
            val builder = StringBuilder()

            logs.forEach(builder::append)

            client!!.send(builder.toString().toByteArray(), "log.ansi").await()
        }
    }

    override fun append(eventObject: ILoggingEvent) {
        if (!ready)
            return

        lock.lock()
        pendingMessages.add(formatted(eventObject))
        lock.unlock()
    }
}