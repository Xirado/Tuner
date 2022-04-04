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

package at.xirado.tuner.interaction.autocomplete

import net.dv8tion.jda.api.interactions.commands.Command

class SearchHistoryAutocompleteChoice(override val name: String, override val value: String, val searchedOn: Long, val playlist: Boolean) : IAutocompleteChoice {

    companion object {
        const val SEARCH_INDICATOR = "\uD83D\uDD52"
        const val PLAYLIST_INDICATOR = "\uD83D\uDCDC" // indicates that this is a playlist
    }

    override fun toJDAChoice(): Command.Choice {
        return Command.Choice(getFormattedName(), value)
    }

    fun getFormattedName(): String {
        var str = "$SEARCH_INDICATOR${if (playlist) PLAYLIST_INDICATOR else ""} $name"
        if (str.length > 100) {
            val replaceString = "..."
            val substr = str.substring(0, 100 - replaceString.length)
            str = substr + replaceString
        }
        return str
    }
}