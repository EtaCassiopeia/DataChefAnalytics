#!/usr/bin/env bash

docker-compose up -d kafka
docker-compose up kafka-create-topics

echo "Load files"

for i in {1..4}
do
   KAFKA_BROKERS=localhost:9092 eval '/tmp/loader-0.1.0-SNAPSHOT/bin/loader ./csv/$i/impressions_$i.csv'
   KAFKA_BROKERS=localhost:9092 eval '/tmp/loader-0.1.0-SNAPSHOT/bin/loader ./csv/$i/clicks_$i.csv'
   KAFKA_BROKERS=localhost:9092 eval '/tmp/loader-0.1.0-SNAPSHOT/bin/loader ./csv/$i/conversions_$i.csv'
done

docker-compose up -d api aggregator
