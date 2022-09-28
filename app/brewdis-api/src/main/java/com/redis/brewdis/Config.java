package com.redis.brewdis;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@EnableAutoConfiguration
public class Config {

	private String keySeparator;
	private long streamPollTimeout = 100;
	private StompConfig stomp = new StompConfig();
	private String availabilityRadius;
	private ProductConfig product = new ProductConfig();
	private StoreConfig store = new StoreConfig();
	private InventoryConfig inventory = new InventoryConfig();
	private SessionConfig session = new SessionConfig();

	public String concat(String... keys) {
		return String.join(keySeparator, keys);
	}

	public String tag(String field, String value) {
		return "@" + field + ":{" + value + "}";
	}

	public String getKeySeparator() {
		return keySeparator;
	}

	public void setKeySeparator(String keySeparator) {
		this.keySeparator = keySeparator;
	}

	public long getStreamPollTimeout() {
		return streamPollTimeout;
	}

	public void setStreamPollTimeout(long streamPollTimeout) {
		this.streamPollTimeout = streamPollTimeout;
	}

	public StompConfig getStomp() {
		return stomp;
	}

	public void setStomp(StompConfig stomp) {
		this.stomp = stomp;
	}

	public String getAvailabilityRadius() {
		return availabilityRadius;
	}

	public void setAvailabilityRadius(String availabilityRadius) {
		this.availabilityRadius = availabilityRadius;
	}

	public ProductConfig getProduct() {
		return product;
	}

	public void setProduct(ProductConfig product) {
		this.product = product;
	}

	public StoreConfig getStore() {
		return store;
	}

	public void setStore(StoreConfig store) {
		this.store = store;
	}

	public InventoryConfig getInventory() {
		return inventory;
	}

	public void setInventory(InventoryConfig inventory) {
		this.inventory = inventory;
	}

	public SessionConfig getSession() {
		return session;
	}

	public void setSession(SessionConfig session) {
		this.session = session;
	}

	public static class SessionConfig {
		private String cartAttribute = "cart";
		private String coordsAttribute = "coords";

		public String getCartAttribute() {
			return cartAttribute;
		}

		public void setCartAttribute(String cartAttribute) {
			this.cartAttribute = cartAttribute;
		}

		public String getCoordsAttribute() {
			return coordsAttribute;
		}

		public void setCoordsAttribute(String coordsAttribute) {
			this.coordsAttribute = coordsAttribute;
		}

	}

	public static class StoreConfig {
		private String index;
		private String keyspace;
		private String url;
		private long count;
		private Map<String, String> inventoryMapping = new HashMap<>();

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}

		public String getKeyspace() {
			return keyspace;
		}

		public void setKeyspace(String keyspace) {
			this.keyspace = keyspace;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		public Map<String, String> getInventoryMapping() {
			return inventoryMapping;
		}

		public void setInventoryMapping(Map<String, String> inventoryMapping) {
			this.inventoryMapping = inventoryMapping;
		}

	}

	public static class ProductConfig {
		private String index;
		private String keyspace;
		private String url;
		private Map<String, String> inventoryMapping = new HashMap<>();
		private ProductLoadConfig load = new ProductLoadConfig();
		private FoodPairingsConfig foodPairings = new FoodPairingsConfig();
		private BreweryConfig brewery = new BreweryConfig();

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}

		public String getKeyspace() {
			return keyspace;
		}

		public void setKeyspace(String keyspace) {
			this.keyspace = keyspace;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public Map<String, String> getInventoryMapping() {
			return inventoryMapping;
		}

		public void setInventoryMapping(Map<String, String> inventoryMapping) {
			this.inventoryMapping = inventoryMapping;
		}

		public ProductLoadConfig getLoad() {
			return load;
		}

		public void setLoad(ProductLoadConfig load) {
			this.load = load;
		}

		public FoodPairingsConfig getFoodPairings() {
			return foodPairings;
		}

		public void setFoodPairings(FoodPairingsConfig foodPairings) {
			this.foodPairings = foodPairings;
		}

		public BreweryConfig getBrewery() {
			return brewery;
		}

		public void setBrewery(BreweryConfig brewery) {
			this.brewery = brewery;
		}

	}

	public static class BreweryConfig {
		private String index;
		private boolean fuzzy;

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}

		public boolean isFuzzy() {
			return fuzzy;
		}

		public void setFuzzy(boolean fuzzy) {
			this.fuzzy = fuzzy;
		}

	}

	public static class FoodPairingsConfig {
		private long limit;
		private String index;
		private boolean fuzzy;
		private long maxSuggestions;

		public long getLimit() {
			return limit;
		}

		public void setLimit(long limit) {
			this.limit = limit;
		}

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}

		public boolean isFuzzy() {
			return fuzzy;
		}

		public void setFuzzy(boolean fuzzy) {
			this.fuzzy = fuzzy;
		}

		public long getMaxSuggestions() {
			return maxSuggestions;
		}

		public void setMaxSuggestions(long maxSuggestions) {
			this.maxSuggestions = maxSuggestions;
		}

	}

	public static class ProductLoadConfig {
		private long count;

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

	}

	public static class InventoryConfig {
		private String updateTopic;
		private String updateStream;
		private String stream;
		private String index;
		private String keyspace;
		private int searchLimit;
		private int levelLow;
		private int levelMedium;
		private InventoryGeneratorConfig generator = new InventoryGeneratorConfig();
		private InventoryRestockConfig restock = new InventoryRestockConfig();
		private InventoryCleanupConfig cleanup = new InventoryCleanupConfig();

		public String getUpdateTopic() {
			return updateTopic;
		}

		public void setUpdateTopic(String updateTopic) {
			this.updateTopic = updateTopic;
		}

		public String getUpdateStream() {
			return updateStream;
		}

		public void setUpdateStream(String updateStream) {
			this.updateStream = updateStream;
		}

		public String getStream() {
			return stream;
		}

		public void setStream(String stream) {
			this.stream = stream;
		}

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}

		public String getKeyspace() {
			return keyspace;
		}

		public void setKeyspace(String keyspace) {
			this.keyspace = keyspace;
		}

		public int getSearchLimit() {
			return searchLimit;
		}

		public void setSearchLimit(int searchLimit) {
			this.searchLimit = searchLimit;
		}

		public int getLevelLow() {
			return levelLow;
		}

		public void setLevelLow(int levelLow) {
			this.levelLow = levelLow;
		}

		public int getLevelMedium() {
			return levelMedium;
		}

		public void setLevelMedium(int levelMedium) {
			this.levelMedium = levelMedium;
		}

		public InventoryGeneratorConfig getGenerator() {
			return generator;
		}

		public void setGenerator(InventoryGeneratorConfig generator) {
			this.generator = generator;
		}

		public InventoryRestockConfig getRestock() {
			return restock;
		}

		public void setRestock(InventoryRestockConfig restock) {
			this.restock = restock;
		}

		public InventoryCleanupConfig getCleanup() {
			return cleanup;
		}

		public void setCleanup(InventoryCleanupConfig cleanup) {
			this.cleanup = cleanup;
		}

		public String level(int quantity) {
			if (quantity <= levelLow) {
				return "low";
			}
			if (quantity <= levelMedium) {
				return "medium";
			}
			return "high";
		}
	}

	public static class InventoryRestockConfig {
		private int delayMin;
		private int delayMax;
		private int threshold;
		private int deltaMin;
		private int deltaMax;

		public int getDelayMin() {
			return delayMin;
		}

		public void setDelayMin(int delayMin) {
			this.delayMin = delayMin;
		}

		public int getDelayMax() {
			return delayMax;
		}

		public void setDelayMax(int delayMax) {
			this.delayMax = delayMax;
		}

		public int getThreshold() {
			return threshold;
		}

		public void setThreshold(int threshold) {
			this.threshold = threshold;
		}

		public int getDeltaMin() {
			return deltaMin;
		}

		public void setDeltaMin(int deltaMin) {
			this.deltaMin = deltaMin;
		}

		public int getDeltaMax() {
			return deltaMax;
		}

		public void setDeltaMax(int deltaMax) {
			this.deltaMax = deltaMax;
		}

	}

	public static class InventoryCleanupConfig {
		private int searchLimit;
		private long ageThreshold;
		private long streamTrimCount;

		public int getSearchLimit() {
			return searchLimit;
		}

		public void setSearchLimit(int searchLimit) {
			this.searchLimit = searchLimit;
		}

		public long getAgeThreshold() {
			return ageThreshold;
		}

		public void setAgeThreshold(long ageThreshold) {
			this.ageThreshold = ageThreshold;
		}

		public long getStreamTrimCount() {
			return streamTrimCount;
		}

		public void setStreamTrimCount(long streamTrimCount) {
			this.streamTrimCount = streamTrimCount;
		}

	}

	public static class InventoryGeneratorConfig {
		private int onHandMin;
		private int onHandMax;
		private int deltaMin;
		private int deltaMax;
		private int allocatedMin;
		private int allocatedMax;
		private int reservedMin;
		private int reservedMax;
		private int virtualHoldMin;
		private int virtualHoldMax;
		private long requestDurationInSeconds;
		private long skusMax;
		private long storesMax;

		public int getOnHandMin() {
			return onHandMin;
		}

		public void setOnHandMin(int onHandMin) {
			this.onHandMin = onHandMin;
		}

		public int getOnHandMax() {
			return onHandMax;
		}

		public void setOnHandMax(int onHandMax) {
			this.onHandMax = onHandMax;
		}

		public int getDeltaMin() {
			return deltaMin;
		}

		public void setDeltaMin(int deltaMin) {
			this.deltaMin = deltaMin;
		}

		public int getDeltaMax() {
			return deltaMax;
		}

		public void setDeltaMax(int deltaMax) {
			this.deltaMax = deltaMax;
		}

		public int getAllocatedMin() {
			return allocatedMin;
		}

		public void setAllocatedMin(int allocatedMin) {
			this.allocatedMin = allocatedMin;
		}

		public int getAllocatedMax() {
			return allocatedMax;
		}

		public void setAllocatedMax(int allocatedMax) {
			this.allocatedMax = allocatedMax;
		}

		public int getReservedMin() {
			return reservedMin;
		}

		public void setReservedMin(int reservedMin) {
			this.reservedMin = reservedMin;
		}

		public int getReservedMax() {
			return reservedMax;
		}

		public void setReservedMax(int reservedMax) {
			this.reservedMax = reservedMax;
		}

		public int getVirtualHoldMin() {
			return virtualHoldMin;
		}

		public void setVirtualHoldMin(int virtualHoldMin) {
			this.virtualHoldMin = virtualHoldMin;
		}

		public int getVirtualHoldMax() {
			return virtualHoldMax;
		}

		public void setVirtualHoldMax(int virtualHoldMax) {
			this.virtualHoldMax = virtualHoldMax;
		}

		public long getRequestDurationInSeconds() {
			return requestDurationInSeconds;
		}

		public void setRequestDurationInSeconds(long requestDurationInSeconds) {
			this.requestDurationInSeconds = requestDurationInSeconds;
		}

		public long getSkusMax() {
			return skusMax;
		}

		public void setSkusMax(long skusMax) {
			this.skusMax = skusMax;
		}

		public long getStoresMax() {
			return storesMax;
		}

		public void setStoresMax(long storesMax) {
			this.storesMax = storesMax;
		}

	}

	public static class StompConfig implements Serializable {
		private static final long serialVersionUID = -623741573410463326L;
		private String protocol;
		private String host;
		private int port;
		private String endpoint;
		private String destinationPrefix;
		private String inventoryTopic;

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getDestinationPrefix() {
			return destinationPrefix;
		}

		public void setDestinationPrefix(String destinationPrefix) {
			this.destinationPrefix = destinationPrefix;
		}

		public String getInventoryTopic() {
			return inventoryTopic;
		}

		public void setInventoryTopic(String inventoryTopic) {
			this.inventoryTopic = inventoryTopic;
		}

		public static long getSerialversionuid() {
			return serialVersionUID;
		}

	}

}
