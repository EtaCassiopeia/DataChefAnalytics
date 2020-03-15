#!/usr/bin/env bash

sbt clean

sbt "project aggregator" docker:publishLocal
sbt "project campaigns-api" docker:publishLocal
sbt "project loader" universal:packageBin

unzip -o -d /tmp ./loader/target/universal/loader-0.1.0-SNAPSHOT.zip
