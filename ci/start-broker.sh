#!/usr/bin/env bash

RABBITMQ_IMAGE_TAG=${RABBITMQ_IMAGE_TAG:-3.11}
RABBITMQ_IMAGE=${RABBITMQ_IMAGE:-rabbitmq}

wait_for_message() {
  while ! docker logs "$1" | grep -q "$2";
  do
      sleep 5
      echo "Waiting 5 seconds for $1 to start..."
  done
}


mkdir -p rabbitmq-configuration

echo "[rabbitmq_management, rabbitmq_shovel,rabbitmq_shovel_management,rabbitmq_federation,rabbitmq_federation_management]." \
  > rabbitmq-configuration/enabled_plugins

echo "Running RabbitMQ ${RABBITMQ_IMAGE}:${RABBITMQ_IMAGE_TAG}"

docker rm -f rabbitmq 2>/dev/null || echo "rabbitmq was not running"
docker run -d --name rabbitmq \
    --network host \
    -v "${PWD}"/rabbitmq-configuration:/etc/rabbitmq \
    "${RABBITMQ_IMAGE}":"${RABBITMQ_IMAGE_TAG}"

wait_for_message rabbitmq "completed with"

docker exec rabbitmq rabbitmq-diagnostics erlang_version
docker exec rabbitmq rabbitmqctl version