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

public class YoutubeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(YoutubeUtil.class);

    public static List<String> getYoutubeMusicSearchResults(String query) throws URISyntaxException, IOException {
        var application = Application.getApplication();
        var config = application.getTunerConfiguration();
        if (config.getInnertubeRequestBody() == null || config.getInnertubeApiKey() == null) {
            if (config.getInnertubeApiKey() == null) {
                LOG.warn("Missing Youtube Innertube API Key!");
            }
            if (config.getInnertubeRequestBody() == null) {
                LOG.warn("Missing Youtube Innertube Request Body!");
            }
            return Collections.emptyList();
        }
        var innertubeApiKey = config.getInnertubeApiKey();

        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost("music.youtube.com")
                .setPath("/youtubei/v1/music/get_search_suggestions")
                .addParameter("key", innertubeApiKey)
                .build();

        var innertubeBody = DataObject.fromJson(config.getInnertubeRequestBody());
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
