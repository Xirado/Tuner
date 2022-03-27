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

package at.xirado.tuner.youtube;

import at.xirado.tuner.Application;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import okhttp3.*;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class YoutubeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(YoutubeUtil.class);

    private static volatile boolean firstTry = true;
    private static volatile boolean ready = false;
    private static AtomicReference<DataObject> innertubeRequestBody;

    public static List<String> getYoutubeMusicSearchResults(String query) throws URISyntaxException, IOException {
        var application = Application.getApplication();
        var config = application.getTunerConfiguration();

        if (!firstTry && !ready) {
            return Collections.emptyList();
        }

        if (firstTry) {
            firstTry = false;
            if (config.getInnertubeRequestBody() == null || config.getInnertubeApiKey() == null) {
                if (config.getInnertubeApiKey() == null) {
                    LOG.warn("Failed to send Youtube Music search suggestion results! Missing \"youtube.innertube_api_key\" config property!");
                }
                if (config.getInnertubeRequestBody() == null) {
                    if (config.getInnertubeRequestBodyLocation() != null) {
                        LOG.warn("Failed to send Youtube Music search suggestion results! File {} does not exist!", config.getInnertubeRequestBodyLocation());
                    } else {
                        LOG.warn("Failed to send Youtube Music search suggestion results! Missing \"youtube.innertube_request_body_location\" property!");
                    }
                }
                return Collections.emptyList();
            }
            innertubeRequestBody = new AtomicReference<>(DataObject.fromJson(config.getInnertubeRequestBody()));
            ready = true;
        }
        var innertubeApiKey = config.getInnertubeApiKey();

        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost("music.youtube.com")
                .setPath("/youtubei/v1/music/get_search_suggestions")
                .addParameter("key", innertubeApiKey)
                .build();

        var innertubeBody = innertubeRequestBody.get();
        innertubeBody.put("input", query);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), innertubeBody.toString());

        Request request = new Request.Builder()
                .url(uri.toURL())
                .post(requestBody)
                .addHeader("referer", "https://music.youtube.com/")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36")
                .addHeader("content-type", "application/json")
                .build();

        Call call = application.getHttpClient().newCall(request);

        Response response = call.execute();

        DataObject responseBody = DataObject.fromJson(response.body().string());
        response.close();
        Optional<DataArray> optContents = responseBody.optArray("contents");
        if (!optContents.isPresent())
            return Collections.emptyList();

        DataObject renderer = optContents.get().getObject(0).getObject("searchSuggestionsSectionRenderer");
        DataArray contents = renderer.optArray("contents").orElse(DataArray.empty());
        List<String> results = new ArrayList<>();
        contents.stream(DataArray::getObject).forEach(content ->
        {
            String result = content.getObject("searchSuggestionRenderer").getObject("navigationEndpoint").getObject("searchEndpoint").getString("query");
            if (result.length() <= 100)
                results.add(result);
        });
        return results;
    }
}
