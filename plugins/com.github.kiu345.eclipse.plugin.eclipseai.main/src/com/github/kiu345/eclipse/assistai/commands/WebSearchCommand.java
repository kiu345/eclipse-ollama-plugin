
package com.github.kiu345.eclipse.assistai.commands;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.inject.Inject;

@Creatable
public class WebSearchCommand {
    @Inject
    private ILog logger;

    public String search(String query) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode resultsArray = mapper.createArrayNode();

            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            logger.info("Performing web search: " + url);

            Document document = Jsoup.parse(URI.create(url).toURL(), 0);
            Elements results = document.select(".results_links");

            for (Element result : results) {
                Element titleElement = result.select(".result__title").select("a").first();
                Element snippetElement = result.select(".result__snippet").first();
                if (titleElement != null && snippetElement != null) {
                    String title = titleElement.text();
                    String resultUrl = titleElement.attr("href");
                    if (resultUrl.startsWith("//")) {
                        resultUrl = "https:" + resultUrl;
                    }
                    String snippetText = snippetElement.text();

                    ObjectNode resultNode = mapper.createObjectNode();
                    resultNode.put("title", title);
                    resultNode.put("url", resultUrl);
                    resultNode.put("snippet", snippetText);

                    resultsArray.add(resultNode);
                }
            }

            String jsonResults = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultsArray);
            logger.info("Search results for query \"" + query + "\":\n" + jsonResults);
            return jsonResults;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
