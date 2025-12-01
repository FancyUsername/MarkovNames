package de.sommer.test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class KleinanzeigenClient {
        private static final String BASE_URL = "https://www.kleinanzeigen.de";

        public List<SearchResult> search(String query, int page) throws IOException {
                String keyword = Optional.ofNullable(query).orElse("").trim();
                String sanitized = keyword.isEmpty() ? "" : URLEncoder.encode(keyword, StandardCharsets.UTF_8.name()).replace("+", "-");
                StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                                .append("/s-")
                                .append(sanitized.isEmpty() ? "" : sanitized + "/")
                                .append("k0");
                if (page > 1) {
                        urlBuilder.append("?page=").append(page);
                }

                Document document = Jsoup.connect(urlBuilder.toString())
                                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Safari/537.36")
                                .get();

                Elements items = document.select("article.aditem");
                if (items.isEmpty()) {
                        items = document.select("li.ad-listitem");
                }

                List<SearchResult> results = new ArrayList<>();
                for (Element item : items) {
                        String id = item.attr("data-adid");
                        Element linkElement = item.selectFirst("a[href]");
                        String title = linkElement != null ? linkElement.text().trim() : "";
                        String url = linkElement != null ? linkElement.absUrl("href") : "";
                        if (url.isEmpty() && linkElement != null) {
                                url = BASE_URL + linkElement.attr("href");
                        }

                        String price = firstNonEmptyText(item,
                                        ".aditem-main--middle--price-shipping--price",
                                        ".aditem-main--middle--price",
                                        ".aditem-details .text-bold");

                        String location = firstNonEmptyText(item,
                                        ".aditem-main--top--left",
                                        ".aditem-main--top",
                                        "[class*='location']");

                        results.add(new SearchResult(id, title, price, location, url));
                }
                return results;
        }

        private String firstNonEmptyText(Element element, String... selectors) {
                for (String selector : selectors) {
                        Element found = element.selectFirst(selector);
                        if (found != null) {
                                String text = found.text().trim();
                                if (!text.isEmpty()) {
                                        return text;
                                }
                        }
                }
                return "";
        }
}
