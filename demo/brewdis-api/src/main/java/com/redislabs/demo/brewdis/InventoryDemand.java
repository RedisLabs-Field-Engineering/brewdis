package com.redislabs.demo.brewdis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redislabs.mesclun.RedisModulesCommands;
import com.redislabs.mesclun.StatefulRedisModulesConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;

import static com.redislabs.demo.brewdis.BrewdisField.*;

@Component
@Slf4j
public class InventoryDemand implements InitializingBean {


    @Autowired
    private Config config;
    @Autowired
    private StatefulRedisModulesConnection<String, String> connection;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    private OfInt delta;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterPropertiesSet() {
        this.delta = new Random().ints(config.getInventory().getGenerator().getDeltaMin(), config.getInventory().getGenerator().getDeltaMax()).iterator();
    }

    @Scheduled(fixedRateString = "${inventory.generator.rate}")
    public void generate() throws JsonProcessingException {
        RedisModulesCommands<String, String> commands = connection.sync();
        for (String session : commands.smembers("sessions")) {
            String store = commands.srandmember("session:stores:" + session);
            if (store == null) {
                continue;
            }
            String sku = commands.srandmember("session:skus:" + session);
            if (sku == null) {
                continue;
            }
            Map<String, String> update = new HashMap<>();
            update.put(STORE_ID, store);
            update.put(PRODUCT_ID, sku);
            update.put(ALLOCATED, String.valueOf(delta.nextInt()));
            kafkaTemplate.send(config.getInventory().getUpdateTopic(), mapper.writeValueAsString(update));
        }

    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name(config.getInventory().getUpdateTopic()).build();
    }

}
