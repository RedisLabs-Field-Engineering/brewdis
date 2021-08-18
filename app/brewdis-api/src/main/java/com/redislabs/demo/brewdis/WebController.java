package com.redislabs.demo.brewdis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.api.search.*;
import com.redislabs.demo.brewdis.Config.StompConfig;
import com.redislabs.demo.brewdis.web.BrewerySuggestion;
import com.redislabs.demo.brewdis.web.Category;
import com.redislabs.demo.brewdis.web.Query;
import com.redislabs.demo.brewdis.web.ResultsPage;
import com.redislabs.demo.brewdis.web.Style;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.redislabs.demo.brewdis.BrewdisField.AVAILABLE_TO_PROMISE;
import static com.redislabs.demo.brewdis.BrewdisField.BREWERY_NAME;
import static com.redislabs.demo.brewdis.BrewdisField.CATEGORY_NAME;
import static com.redislabs.demo.brewdis.BrewdisField.LEVEL;
import static com.redislabs.demo.brewdis.BrewdisField.LOCATION;
import static com.redislabs.demo.brewdis.BrewdisField.PRODUCT_DESCRIPTION;
import static com.redislabs.demo.brewdis.BrewdisField.PRODUCT_ID;
import static com.redislabs.demo.brewdis.BrewdisField.PRODUCT_NAME;
import static com.redislabs.demo.brewdis.BrewdisField.STORE_ID;
import static com.redislabs.demo.brewdis.BrewdisField.STYLE_NAME;

@SuppressWarnings("unused")
@RestController
@RequestMapping(path = "/api")
@CrossOrigin
@Slf4j
class WebController {

    @Autowired
    private Config config;
    @Autowired
    private StatefulRedisModulesConnection<String, String> connection;
    @Autowired
    private DataLoader data;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/config/stomp")
    public StompConfig stompConfig() {
        return config.getStomp();
    }

    @PostMapping("/products")
    public ResultsPage products(@RequestBody Query query,
                                @RequestParam(name = "longitude") Double longitude,
                                @RequestParam(name = "latitude") Double latitude, HttpSession session) {
        log.info("Searching for products around lon={} lat={}", longitude, latitude);
        SearchOptions.SearchOptionsBuilder<String,String> options = SearchOptions.<String,String>builder()
                .highlight(SearchOptions.Highlight.<String,String>builder().field(PRODUCT_NAME).field(PRODUCT_DESCRIPTION)
                        .field(CATEGORY_NAME).field(STYLE_NAME).field(BREWERY_NAME)
                        .tags(SearchOptions.Tags.<String>builder().open("<mark>").close("</mark>").build()).build())
                .limit(SearchOptions.Limit.offset(query.getOffset()).num(query.getPageSize()));
        if (query.getSortByField() != null) {
            options.sortBy(SearchOptions.SortBy.<String,String>field(query.getSortByField()).order(Order.valueOf(query.getSortByDirection())));
        }
        String queryString = query.getQuery() == null || query.getQuery().length() == 0 ? "*" : query.getQuery();
        long startTime = System.currentTimeMillis();
        SearchResults<String, String> searchResults = connection.sync().search(config.getProduct().getIndex(), queryString, options.build());
        long endTime = System.currentTimeMillis();
        ResultsPage results = new ResultsPage();
        results.setCount(searchResults.getCount());
        results.setResults(searchResults);
        results.setPageIndex(query.getPageIndex());
        results.setPageSize(query.getPageSize());
        results.setDuration(((float) (endTime - startTime)) / 1000);
        generateDemand(session, longitude, latitude, searchResults);
        return results;
    }

    private void generateDemand(HttpSession session, Double longitude, Double latitude, SearchResults<String, String> searchResults) {
        List<String> skus = searchResults.stream().limit(config.getInventory().getGenerator().getSkusMax()).map(r -> r.get(PRODUCT_ID)).collect(Collectors.toList());
        if (skus.isEmpty()) {
            log.warn("No SKUs found to generate demand");
            return;
        }
        List<String> stores = connection.sync().search(config.getStore().getIndex(), geoCriteria(longitude, latitude)).stream().limit(config.getInventory().getGenerator().getStoresMax()).map(r -> r.get(STORE_ID)).collect(Collectors.toList());
        if (stores.isEmpty()) {
            log.warn("No store found to generate demand");
            return;
        }
        RedisModulesCommands<String, String> sync = connection.sync();
        sync.sadd("sessions", session.getId());
        String storesKey = "session:stores:" + session.getId();
        sync.sadd(storesKey, stores.toArray(new String[0]));
        sync.expire(storesKey, config.getInventory().getGenerator().getRequestDurationInSeconds());
        String skusKey = "session:skus:" + session.getId();
        sync.sadd(skusKey, skus.toArray(new String[0]));
        sync.expire(skusKey, config.getInventory().getGenerator().getRequestDurationInSeconds());

    }

    @GetMapping("/styles")
    public List<Style> styles(@RequestParam(name = "category", defaultValue = "", required = false) String category) {
        return data.getStyles().get(category);
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return data.getCategories();
    }

    @GetMapping("/inventory")
    public SearchResults<String, String> inventory(@RequestParam(name = "store", required = false) String store) {
        String query = "@" + AVAILABLE_TO_PROMISE + ":[0 inf]";
        if (store != null) {
            query += " " + config.tag(STORE_ID, store);
        }
        return connection.sync().search(config.getInventory().getIndex(), query,
                SearchOptions.<String, String>builder().sortBy(SearchOptions.SortBy.<String, String>field(STORE_ID).order(Order.ASC))
                        .limit(SearchOptions.Limit.offset(0).num(config.getInventory().getSearchLimit())).build());
    }

    @GetMapping("/availability")
    public SearchResults<String, String> availability(@RequestParam(name = "sku", required = false) String sku,
                                                      @RequestParam(name = "longitude") Double longitude,
                                                      @RequestParam(name = "latitude") Double latitude) {
        String query = geoCriteria(longitude, latitude);
        if (sku != null) {
            query += " " + config.tag(PRODUCT_ID, sku);
        }
        log.info("Searching for availability: {}", query);
        SearchResults<String, String> results = connection.sync().search(config.getInventory().getIndex(), query,
                SearchOptions.<String, String>builder().limit(SearchOptions.Limit.offset(0).num(config.getInventory().getSearchLimit()))
                        .build());
        results.forEach(r -> r.put(LEVEL, config.getInventory().level(availableToPromise(r))));
        return results;

    }

    private int availableToPromise(Document<String, String> result) {
        if (result.containsKey(AVAILABLE_TO_PROMISE)) {
            return Integer.parseInt(result.get(AVAILABLE_TO_PROMISE));
        }
        return 0;
    }

    private String geoCriteria(Double longitude, Double latitude) {
        return "@" + LOCATION + ":[" + longitude + " " + latitude + " " + config.getAvailabilityRadius() + "]";
    }

    @GetMapping("/breweries")
    public Stream<BrewerySuggestion> suggestBreweries(
            @RequestParam(name = "prefix", defaultValue = "", required = false) String prefix) {
        List<Suggestion<String>> results = connection.sync().sugget(config.getProduct().getBrewery().getIndex(),
                prefix, SuggetOptions.builder().withPayloads(true).max(20L)
                        .fuzzy(config.getProduct().getBrewery().isFuzzy()).build());
        return results.stream().map(s -> {
            BrewerySuggestion suggestion = new BrewerySuggestion();
            suggestion.setName(s.getString());
            BrewerySuggestion.Payload payload;
            try {
                payload = mapper.readValue(s.getPayload(), BrewerySuggestion.Payload.class);
                suggestion.setId(payload.getId());
                suggestion.setIcon(payload.getIcon());
            } catch (Exception e) {
                log.error("Could not deserialize brewery payload {}", s.getPayload(), e);
            }
            return suggestion;
        });
    }

    @GetMapping("/foods")
    public Stream<String> suggestFoods(@RequestParam(name = "prefix", defaultValue = "", required = false) String prefix) {
        List<Suggestion<String>> results = connection.sync().sugget(config.getProduct().getFoodPairings().getIndex(),
                prefix, SuggetOptions.builder().withPayloads(true).max(config.getProduct().getFoodPairings().getMaxSuggestions())
                        .fuzzy(config.getProduct().getFoodPairings().isFuzzy()).build());
        return results.stream().map(Suggestion::getString);
    }

}
