#!/bin/bash

set -e
(
if lsof -Pi :6379 -sTCP:LISTEN -t >/dev/null ; then
    echo "Please terminate the local redis-server on 6379"
    exit 1
fi
)

echo "Building the Redis Kafka Connector and starting docker"
(
docker-compose --project-directory ../redis-kafka-connect up -d --build
)

function clean_up {
    echo -e "\n\nSHUTTING DOWN\n\n"
    curl --output /dev/null -X DELETE http://localhost:8083/connectors/inventory-updates-sink || true
    curl --output /dev/null -X DELETE http://localhost:8083/connectors/inventory-stream-source || true
    (
    cd ../redis-kafka-connect
    docker-compose down
    )
    if [ -z "$1" ]
    then
      echo -e "Bye!\n"
    else
      echo -e "$1"
    fi
}

sleep 5
echo -ne "\n\nWaiting for the systems to be ready.."
function test_systems_available {
  COUNTER=0
  until $(curl --output /dev/null --silent --head --fail http://localhost:$1); do
      printf '.'
      sleep 2
      (( COUNTER+=1 ))
      if [[ $COUNTER -gt 30 ]]; then
        MSG="\nWARNING: Could not reach configured kafka system on http://localhost:$1 \nNote: This script requires curl.\n"

          if [[ "$OSTYPE" == "darwin"* ]]; then
            MSG+="\nIf using OSX please try reconfiguring Docker and increasing RAM and CPU. Then restart and try again.\n\n"
          fi

        echo -e "$MSG"
        clean_up "$MSG"
        exit 1
      fi
  done
}

test_systems_available 8082
test_systems_available 8083

trap clean_up EXIT

echo -e "\nCreating topic 'inventory-updates':"
(
cd ../redis-kafka-connect
docker-compose exec broker /usr/bin/kafka-topics --create --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic inventory-updates
)

sleep 2
echo -e "\nRegistering schema on topic 'inventory-updates':"
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{"schema": "{\"type\":\"record\",\"name\":\"Payment\",\"namespace\":\"my.examples\",\"fields\":[{\"name\":\"store\",\"type\":\"string\"},{\"name\":\"sku\",\"type\":\"string\"},{\"name\":\"allocated\",\"type\":\"string\"}]}"}' http://localhost:8081/subjects/inventory-updates-value/versions -w "\n"

sleep 2
echo -e "\nAdding Redis Kafka Sink Connector for the 'inventory-updates' topic into the 'inventory-updates' stream:"
curl -X POST -H "Content-Type: application/json" --data '
  {"name": "inventory-updates-sink",
   "config": {
     "connector.class":"com.redis.kafka.connect.RedisSinkConnector",
     "tasks.max":"1",
     "topics":"inventory-updates",
     "redis.uri":"redis://redis:6379",
     "key.converter": "org.apache.kafka.connect.storage.StringConverter",
     "value.converter": "org.apache.kafka.connect.json.JsonConverter",
     "value.converter.schemas.enable": "false"
}}' http://localhost:8083/connectors -w "\n"

sleep 2
echo -e "\nAdding Redis Kafka Source Connector for the 'inventory-stream' stream:"
curl -X POST -H "Content-Type: application/json" --data '
  {"name": "inventory-stream-source",
   "config": {
     "tasks.max":"1",
     "connector.class":"com.redis.kafka.connect.RedisSourceConnector",
     "redis.uri":"redis://redis:6379",
     "redis.stream.name":"inventory-stream",
     "topic": "inventory-stream"
}}' http://localhost:8083/connectors -w "\n"

sleep 2
echo -e "\nStarting Brewdis:"
./gradlew bootRun

echo -e '''

Use <ctrl>-c to quit'''

read -r -d '' _ </dev/tty
