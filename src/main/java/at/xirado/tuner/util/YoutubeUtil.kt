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

package at.xirado.tuner.util

import at.xirado.tuner.Application
import at.xirado.tuner.interaction.autocomplete.BasicAutocompleteChoice
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private var innertubeRequestBody = "{\"context\":{\"client\":{\"deviceMake\":\"\",\"deviceModel\":\"\",\"userAgent\":\"Mozilla/5.0\",\"clientName\":\"WEB_REMIX\",\"clientVersion\":\"1.20220330.01.00\",\"osName\":\"Windows\",\"osVersion\":\"10.0\",\"originalUrl\":\"https://music.youtube.com/\"}}}"

suspend fun getYoutubeMusicSearchResults(application: Application, query: String) : List<BasicAutocompleteChoice> {
    val httpClient = application.httpClient

    val uri = URIBuilder()
    uri.scheme = "https"
    uri.host = "music.youtube.com"
    uri.path = "/youtubei/v1/music/get_search_suggestions"

    val innertubeBody = DataObject.fromJson(innertubeRequestBody)
    innertubeBody.put("input", query)

    val requestBody = innertubeBody.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(URL(uri.toString()))
        .post(requestBody)
        .addHeader("Referer", "https://music.youtube.com/")
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36")
        .addHeader("Content-Type", "application/json")
        .addHeader("Host", "music.youtube.com")
        .build()

    val response = httpClient.newCall(request).await()
    val responseBody = DataObject.fromJson(response.body!!.string())
    response.close()

    val optContents = responseBody.optArray("contents")
    if (!optContents.isPresent)
        return listOf()

    val renderer = optContents.get().getObject(0).getObject("searchSuggestionsSectionRenderer")
    val contents = renderer.optArray("contents").orElseGet(DataArray::empty)
    val results = mutableListOf<BasicAutocompleteChoice>()
    contents.stream(DataArray::getObject).forEach {
        val result = it.getObject("searchSuggestionRenderer").getObject("navigationEndpoint").getObject("searchEndpoint").getString("query")
        if (result.length <= 100)
            results.add(BasicAutocompleteChoice(result, result))
    }
    return results
}

suspend fun Call.await(recordStack: Boolean = true): Response {
    val callStack = if (recordStack) {
        IOException().apply {
            stackTrace = stackTrace.copyOfRange(1, stackTrace.size)
        }
    } else {
        null
    }

    return suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isCancelled) return
                callStack?.initCause(e)
                cont.resumeWithException(callStack ?: e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })

        cont.invokeOnCancellation {
            try {
                cancel()
            } catch (_: Throwable) {

            }
        }
    }
}