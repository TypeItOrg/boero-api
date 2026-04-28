#!/bin/sh
set -eu

WATCH_PATHS="src build.gradle settings.gradle"
APP_PID=""

start_app() {
  ./gradlew bootRun &
  APP_PID=$!
  echo "Spring Boot iniciado con PID ${APP_PID}"
}

stop_app() {
  if [ -n "${APP_PID}" ] && kill -0 "${APP_PID}" 2>/dev/null; then
    kill "${APP_PID}" 2>/dev/null || true
    wait "${APP_PID}" 2>/dev/null || true
  fi
}

shutdown() {
  stop_app
  exit 0
}

trap shutdown INT TERM

start_app

while inotifywait -r -e modify,create,delete,move ${WATCH_PATHS}; do
  echo "Cambio detectado, reiniciando Spring Boot"
  stop_app
  start_app
done
