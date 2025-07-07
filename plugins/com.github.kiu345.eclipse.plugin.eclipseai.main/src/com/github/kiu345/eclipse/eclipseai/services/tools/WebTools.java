package com.github.kiu345.eclipse.eclipseai.services.tools;

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

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Creatable
@Singleton
public class WebTools {
    @Inject
    private ILog log;

    public record WeatherReport(String location, int degrees, String unit, String[] forecast) {};

    @Tool("Performs a search using a Duck Duck Go search engine and returns the search result json.")
    public String webSearch(
            @P(value = "A search query", required = true) String query
    ) {
        return search(query);
    }

    @Tool("Reads the content of the given web site and returns its content as a markdown text.")
    public String readWebPage(
            @P(value = "A web site URL", required = true) String url
    ) {
        return readWebPage(url);
    }

    public String search(String query) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode resultsArray = mapper.createArrayNode();

            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            log.info("Performing web search: " + url);

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
            log.info("Search results for query \"" + query + "\":\n" + jsonResults);
            return jsonResults;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
