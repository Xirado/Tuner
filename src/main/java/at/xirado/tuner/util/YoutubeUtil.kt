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
import okhttp3.*
import org.apache.http.client.utils.URIBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val log = LoggerFactory.getLogger(Application::class.java) as Logger
private var firstTry = true
private var ready = false
private var innertubeRequestBody: AtomicReference<DataObject>? = null

suspend fun getYoutubeMusicSearchResults(application: Application, query: String) : List<BasicAutocompleteChoice> {
    val tunerConfig = application.tunerConfig
    val httpClient = application.httpClient
    if (!firstTry && !ready)
        return listOf()

    if (firstTry) {
        firstTry = false
        if (tunerConfig.innertubeRequestBody == null || tunerConfig.innertubeApiKey == null) {
            if (tunerConfig.innertubeApiKey == null)
                log.warn("Failed to fetch Youtube Music search results! Missing \"youtube.innertube_api_key\" config property!")

            if (tunerConfig.innertubeRequestBody == null) {
                  if (tunerConfig.innertubeRequestBodyLocation != null)
                      log.warn("Failed to fetch Youtube Music search results! File ${tunerConfig.innertubeRequestBodyLocation} does not exist!")
                  else
                      log.warn("Failed to fetch Youtube Music search results! Missing \"youtube.innertube_request_body_location\" property!")
            }
            return listOf()
        }
        innertubeRequestBody = AtomicReference(DataObject.fromJson(tunerConfig.innertubeRequestBody))
        ready = true
    }
    val innertubeApiKey = tunerConfig.innertubeApiKey

    val uri = URIBuilder()
    uri.scheme = "https"
    uri.host = "music.youtube.com"
    uri.path = "/youtubei/v1/music/get_search_suggestions"
    uri.addParameter("key", innertubeApiKey)

    val innertubeBody = innertubeRequestBody!!.get()
    innertubeBody.put("input", query)

    val requestBody = RequestBody.create(MediaType.get("application/json"), innertubeBody.toString())

    val request = Request.Builder()
        .url(URL(uri.toString()))
        .post(requestBody)
        .addHeader("referer", "https://music.youtube.com/")
        .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36")
        .addHeader("content-type", "application/json")
        .build()

    val response = httpClient.newCall(request).await()
    val responseBody = DataObject.fromJson(response.body()!!.string())
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