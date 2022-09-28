package com.redis.brewdis;

import static com.redis.brewdis.BrewdisField.BREWERY_ICON;
import static com.redis.brewdis.BrewdisField.BREWERY_ID;
import static com.redis.brewdis.BrewdisField.BREWERY_NAME;
import static com.redis.brewdis.BrewdisField.CATEGORY_ID;
import static com.redis.brewdis.BrewdisField.CATEGORY_NAME;
import static com.redis.brewdis.BrewdisField.COUNT;
import static com.redis.brewdis.BrewdisField.FOOD_PAIRINGS;
import static com.redis.brewdis.BrewdisField.LOCATION;
import static com.redis.brewdis.BrewdisField.PRODUCT_DESCRIPTION;
import static com.redis.brewdis.BrewdisField.PRODUCT_ID;
import static com.redis.brewdis.BrewdisField.PRODUCT_LABEL;
import static com.redis.brewdis.BrewdisField.PRODUCT_NAME;
import static com.redis.brewdis.BrewdisField.STORE_ID;
import static com.redis.brewdis.BrewdisField.STYLE_ID;
import static com.redis.brewdis.BrewdisField.STYLE_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.brewdis.web.BrewerySuggestion;
import com.redis.brewdis.web.Category;
import com.redis.brewdis.web.Style;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.async.RedisModulesAsyncCommands;
import com.redis.lettucemod.api.sync.RediSearchCommands;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.search.AggregateOptions;
import com.redis.lettucemod.search.AggregateResults;
import com.redis.lettucemod.search.CreateOptions;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.Group;
import com.redis.lettucemod.search.IndexInfo;
import com.redis.lettucemod.search.Limit;
import com.redis.lettucemod.search.Reducers.CountDistinct;
import com.redis.lettucemod.search.Sort;
import com.redis.lettucemod.search.Suggestion;
import com.redis.lettucemod.search.TextField.PhoneticMatcher;
import com.redis.lettucemod.util.RedisModulesUtils;
import com.redis.riot.MapProcessorOptions;
import com.redis.riot.RedisOptions;
import com.redis.riot.file.FileImportCommand;
import com.redis.riot.file.RiotFile;
import com.redis.riot.redis.HsetCommand;
import com.redis.riot.redis.KeyOptions;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;

@Component
public class DataLoader implements InitializingBean {

	private final Logger log = LoggerFactory.getLogger(DataLoader.class);

	@Value("classpath:english_stopwords.txt")
	private Resource stopwordsResource;
	@Autowired
	private StatefulRedisModulesConnection<String, String> connection;
	@Autowired
	private GenericObjectPool<StatefulRedisModulesConnection<String, String>> pool;
	@Autowired
	private Config config;
	@Autowired
	private RedisProperties redisProperties;
	private List<Category> categories;
	private Map<String, List<Style>> styles = new HashMap<>();
	private List<String> stopwords;

	public List<Category> getCategories() {
		return categories;
	}

	public Map<String, List<Style>> getStyles() {
		return styles;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stopwords = new BufferedReader(
				new InputStreamReader(stopwordsResource.getInputStream(), StandardCharsets.UTF_8)).lines()
				.collect(Collectors.toList());
	}

	public void execute() throws Exception {
		loadStores();
		loadProducts();
		loadBreweries();
		loadCategoriesAndStyles();
		loadFoodPairings();
	}

	@SuppressWarnings("unchecked")
	private void loadStores() throws Exception {
		RediSearchCommands<String, String> commands = connection.sync();
		String index = config.getStore().getIndex();
		try {
			IndexInfo info = RedisModulesUtils.indexInfo(commands.ftInfo(index));
			if (info.getNumDocs() >= config.getStore().getCount()) {
				log.info("Found {} stores - skipping load", Math.round(info.getNumDocs()));
				return;
			}
			commands.ftDropindex(index);
		} catch (RedisCommandExecutionException e) {
			if (!e.getMessage().equals("Unknown Index name")) {
				throw e;
			}
		}
		commands.ftCreate(index,
				CreateOptions.<String, String>builder()
						.prefix(config.getStore().getKeyspace() + config.getKeySeparator()).build(),
				Field.tag(STORE_ID).sortable().build(), Field.text("description").build(),
				Field.tag("market").sortable().build(), Field.tag("parent").sortable().build(),
				Field.text("address").build(), Field.text("city").sortable().build(),
				Field.tag("country").sortable().build(), Field.tag("inventoryAvailableToSell").sortable().build(),
				Field.tag("isDefault").sortable().build(), Field.tag("preferred").sortable().build(),
				Field.numeric("latitude").sortable().build(), Field.geo(LOCATION).build(),
				Field.numeric("longitude").sortable().build(), Field.tag("rollupInventory").sortable().build(),
				Field.tag("state").sortable().build(), Field.tag("type").sortable().build(),
				Field.tag("postalCode").sortable().build());
		RiotFile file = new RiotFile();
		configure(file.getRedisOptions());
		FileImportCommand command = new FileImportCommand();
		command.setApp(file);
		command.setFiles(Collections.singletonList(config.getStore().getUrl()));
		command.getOptions().setHeader(true);
		MapProcessorOptions processorOptions = new MapProcessorOptions();
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> fields = new LinkedHashMap<>();
		fields.put(LOCATION, parser.parseExpression("#geo(longitude,latitude)"));
		processorOptions.setSpelFields(fields);
		command.setProcessorOptions(processorOptions);
		HsetCommand hset = new HsetCommand();
		KeyOptions keyOptions = new KeyOptions();
		keyOptions.setKeyspace(config.getStore().getKeyspace());
		keyOptions.setKeys(new String[] { STORE_ID });
		hset.setKeyOptions(keyOptions);
		command.setRedisCommands(Collections.singletonList(hset));
		command.call();
	}

	@SuppressWarnings("unchecked")
	private void loadProducts() throws Exception {
		RediSearchCommands<String, String> commands = connection.sync();
		String index = config.getProduct().getIndex();
		try {
			IndexInfo info = RedisModulesUtils.indexInfo(commands.ftInfo(index));
			if (info.getNumDocs() >= config.getProduct().getLoad().getCount()) {
				log.info("Found {} products - skipping load", Math.round(info.getNumDocs()));
				return;
			}
			commands.ftDropindex(index);
		} catch (RedisCommandExecutionException e) {
			if (!e.getMessage().equals("Unknown Index name")) {
				throw e;
			}
		}
		commands.ftCreate(index,
				CreateOptions.<String, String>builder()
						.prefix(config.getProduct().getKeyspace() + config.getKeySeparator()).build(),
				Field.tag(PRODUCT_ID).sortable().build(), Field.text(PRODUCT_NAME).sortable().build(),
				Field.text(PRODUCT_DESCRIPTION).matcher(PhoneticMatcher.ENGLISH).build(),
				Field.tag(PRODUCT_LABEL).build(), Field.tag(CATEGORY_ID).sortable().build(),
				Field.text(CATEGORY_NAME).build(), Field.tag(STYLE_ID).sortable().build(),
				Field.text(STYLE_NAME).build(), Field.tag(BREWERY_ID).sortable().build(),
				Field.text(BREWERY_NAME).build(), Field.text(FOOD_PAIRINGS).sortable().build(),
				Field.tag("isOrganic").sortable().build(), Field.numeric("abv").sortable().build(),
				Field.numeric("ibu").sortable().build());
		RiotFile file = new RiotFile();
		configure(file.getRedisOptions());
		FileImportCommand command = new FileImportCommand();
		command.setApp(file);
		command.setFiles(Collections.singletonList(config.getProduct().getUrl()));
		MapProcessorOptions processorOptions = new MapProcessorOptions();
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> fields = new LinkedHashMap<>();
		fields.put(PRODUCT_ID, parser.parseExpression("id"));
		fields.put(PRODUCT_LABEL, parser.parseExpression("containsKey('labels')"));
		fields.put(CATEGORY_ID, parser.parseExpression("style.category.id"));
		fields.put(CATEGORY_NAME, parser.parseExpression("style.category.name"));
		fields.put(STYLE_NAME, parser.parseExpression("style.shortName"));
		fields.put(STYLE_ID, parser.parseExpression("style.id"));
		fields.put(BREWERY_ID, parser.parseExpression("containsKey('breweries')?breweries[0].id:null"));
		fields.put(BREWERY_NAME, parser.parseExpression("containsKey('breweries')?breweries[0].nameShortDisplay:null"));
		fields.put(BREWERY_ICON, parser.parseExpression(
				"containsKey('breweries')?breweries[0].containsKey('images')?breweries[0].get('images').get('icon'):null:null"));
		processorOptions.setSpelFields(fields);
		command.setProcessorOptions(processorOptions);
		HsetCommand hset = new HsetCommand();
		KeyOptions keyOptions = new KeyOptions();
		keyOptions.setKeyspace(config.getProduct().getKeyspace());
		keyOptions.setKeys(new String[] { PRODUCT_ID });
		hset.setKeyOptions(keyOptions);
		command.setRedisCommands(Collections.singletonList(hset));
		command.call();
	}

	private void configure(RedisOptions redisOptions) {
		redisOptions.setHost(redisProperties.getHost());
		redisOptions.setPort(redisProperties.getPort());
		if (redisProperties.getClientName() != null) {
			redisOptions.setClientName(redisProperties.getClientName());
		}
		redisOptions.setDatabase(redisProperties.getDatabase());
		if (redisProperties.getPassword() != null) {
			redisOptions.setPassword(redisProperties.getPassword().toCharArray());
		}
		redisOptions.setTls(redisProperties.isSsl());
	}

	private void loadCategoriesAndStyles() {
		log.info("Loading categories");
		RediSearchCommands<String, String> commands = connection.sync();
		String index = config.getProduct().getIndex();
		AggregateResults<String> results = commands
				.ftAggregate(index, "*",
						AggregateOptions.<String, String>builder().load(CATEGORY_NAME)
								.operation(Group.by(CATEGORY_ID, CATEGORY_NAME)
										.reducer(CountDistinct.property(PRODUCT_ID).as(COUNT).build()).build())
								.build());
		this.categories = results.stream().map(
				r -> Category.builder().id((String) r.get(CATEGORY_ID)).name((String) r.get(CATEGORY_NAME)).build())
				.sorted(Comparator.comparing(Category::getName, Comparator.nullsLast(Comparator.naturalOrder())))
				.collect(Collectors.toList());
		log.info("Loading styles");
		this.categories.forEach(category -> {
			AggregateResults<String> styleResults = commands
					.ftAggregate(index, config.tag(CATEGORY_ID, category.getId()),
							AggregateOptions.<String, String>builder().load(STYLE_NAME)
									.operation(Group.by(STYLE_ID, STYLE_NAME)
											.reducer(CountDistinct.property(PRODUCT_ID).as(COUNT).build()).build())
									.build());
			List<Style> styleList = styleResults.stream()
					.map(r -> Style.builder().id((String) r.get(STYLE_ID)).name((String) r.get(STYLE_NAME)).build())
					.sorted(Comparator.comparing(Style::getName, Comparator.nullsLast(Comparator.naturalOrder())))
					.collect(Collectors.toList());
			this.styles.put(category.getId(), styleList);
		});
	}

	private void loadBreweries() {
		RediSearchCommands<String, String> commands = connection.sync();
		try {
			Long length = commands.ftSuglen(config.getProduct().getBrewery().getIndex());
			if (length != null && length > 0) {
				log.info("Found {} breweries - skipping load", length);
				return;
			}
		} catch (RedisCommandExecutionException e) {
			// ignore
		}
		log.info("Loading breweries");
		AggregateResults<String> results = commands
				.ftAggregate(config.getProduct().getIndex(), "*",
						AggregateOptions.<String, String>builder().load(BREWERY_NAME).load(BREWERY_ICON)
								.operation(Group.by(BREWERY_ID, BREWERY_NAME, BREWERY_ICON)
										.reducer(CountDistinct.property(PRODUCT_ID).as(COUNT).build()).build())
								.build());
		ObjectMapper mapper = new ObjectMapper();
		results.forEach(r -> {
			BrewerySuggestion.Payload payloadObject = new BrewerySuggestion.Payload();
			payloadObject.setId((String) r.get(BREWERY_ID));
			payloadObject.setIcon((String) r.get(BREWERY_ICON));
			String payload = null;
			try {
				payload = mapper.writeValueAsString(payloadObject);
			} catch (JsonProcessingException e) {
				log.error("Could not serialize brewery payload {}", payloadObject, e);
			}
			String breweryName = (String) r.get(BREWERY_NAME);
			if (breweryName == null) {
				return;
			}
			double count = Double.parseDouble((String) r.get(COUNT));
			commands.ftSugadd(config.getProduct().getBrewery().getIndex(),
					Suggestion.string(breweryName).score(count).payload(payload).build());
		});
		log.info("Loaded {} breweries", results.size());
	}

	private void loadFoodPairings() throws Exception {
		RedisModulesCommands<String, String> sync = connection.sync();
		sync.del(config.getProduct().getFoodPairings().getIndex());
		log.info("Loading food pairings");
		String index = config.getProduct().getIndex();
		AggregateResults<String> results = sync.ftAggregate(index, "*", AggregateOptions.<String, String>builder()
				.operation(
						Group.by(FOOD_PAIRINGS).reducer(CountDistinct.property(PRODUCT_ID).as(COUNT).build()).build())
				.operation(Sort.by(Sort.Property.desc(COUNT)).build())
				.operation(Limit.offset(0).num(config.getProduct().getFoodPairings().getLimit())).build());
		try (StatefulRedisModulesConnection<String, String> connection = pool.borrowObject()) {
			RedisModulesAsyncCommands<String, String> async = connection.async();
			connection.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = new ArrayList<>();
			results.forEach(r -> {
				String foodPairings = (String) r.get(FOOD_PAIRINGS);
				if (foodPairings == null || foodPairings.trim().isEmpty()) {
					return;
				}
				Arrays.stream(foodPairings.split("[,\\n]")).map(this::clean).filter(s -> s.split(" ").length <= 2)
						.forEach(
								food -> futures.add(async.ftSugaddIncr(config.getProduct().getFoodPairings().getIndex(),
										Suggestion.of(food, 1.0))));
			});
			connection.flushCommands();
			LettuceFutures.awaitAll(connection.getTimeout(), futures.toArray(new RedisFuture[0]));
			connection.setAutoFlushCommands(true);
			log.info("Loaded {} food pairings", results.size());
		}
	}

	private String clean(String food) {
		List<String> allWords = Stream.of(food.toLowerCase().split(" "))
				.collect(Collectors.toCollection(ArrayList<String>::new));
		allWords.removeAll(stopwords);
		String result = String.join(" ", allWords).trim();
		if (result.endsWith(".")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

}
