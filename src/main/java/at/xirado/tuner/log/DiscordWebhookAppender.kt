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
import java.lang.StringBuilder
import java.util.concurrent.locks.ReentrantLock
import kotlin.properties.Delegates

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
        val webhookLock = ReentrantLock()
        val pendingMessages = mutableListOf<String>()
        val emptyLength = getWebhookMessageLength(listOf())

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
                    webhookLock.lock()
                    val size = pendingMessages.size
                    webhookLock.unlock()
                    if (size > 0 && state != 1) {
                        state = 1
                        waitingTime = System.currentTimeMillis()
                    }

                    if (state == 1 && System.currentTimeMillis() > waitingTime + 3000) {
                        webhookLock.lock()
                        val pack = split(pendingMessages)

                        pack.forEach {
                            sendWebhook(it)
                        }
                        pendingMessages.clear()
                        webhookLock.unlock()
                        state = 0
                    }

                    delay(100)
                }
            }
        }
        fun formatted(event: ILoggingEvent) : String {
            var formatted = event.formattedMessage
            if (formatted.length > 1800)
                formatted = "[!] Message too long"

            val level = event.level

            var primary = when (level) {
                Level.WARN -> YELLOW
                Level.ERROR -> RED
                Level.INFO -> BLUE
                else -> WHITE
            }

            val builder = StringBuilder("$GREY[$primary${level.levelStr.uppercase()}$GREY] $primary$formatted\n")

            if (event.throwableProxy != null) {
                val proxy = event.throwableProxy
                var result = ThrowableProxyUtil.asString(proxy)
                if (result.length > 1500)
                   result = "   Too long!\n"

                builder.append(result).append(RESET)
            }
            return builder.toString()
        }

        private suspend fun sendWebhook(logs: List<String>) {
            val builder = StringBuilder("```ansi\n")

            logs.forEach(builder::append)

            builder.append("```")

            client!!.send(builder.toString().trim()).await()
        }

        private fun getWebhookMessageLength(logs: List<String>) : Int {
            val builder = StringBuilder("```ansi\n")
            logs.forEach(builder::append)
            builder.append("```")
            return builder.length
        }

        private fun split(input: List<String>) : List<List<String>> {
            val output = mutableListOf<List<String>>()
            val current = mutableListOf<String>()

            var currentSize = emptyLength
            var index = 0
            for (i in input.indices) {
                if (currentSize + input[i].length <= 2000) {
                    currentSize += input[i].length
                    current.add(input[i])
                    continue
                }
                val list = mutableListOf<String>()
                list.addAll(current)
                output.add(list)
                current.clear()
                current.add(input[i])
                currentSize = input[i].length
                index = i
            }
            val lastIter = mutableListOf<String>()
            for (i in index until input.size) {
                lastIter.add(input[i])
            }
            output.add(lastIter)
            return output
        }
    }

    override fun append(eventObject: ILoggingEvent) {
        if (!ready)
            return

        webhookLock.lock()
        pendingMessages.add(formatted(eventObject))
        webhookLock.unlock()
    }
}