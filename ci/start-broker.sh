#!/usr/bin/env bash

RABBITMQ_IMAGE_TAG=${RABBITMQ_IMAGE_TAG:-3.11}
RABBITMQ_IMAGE=${RABBITMQ_IMAGE:-rabbitmq}
DELAYED_MESSAGE_EXCHANGE_PLUGIN_VERSION=${DELAYED_MESSAGE_EXCHANGE_PLUGIN_VERSION:-3.11.1}

wait_for_message() {
  while ! docker logs "$1" | grep -q "$2";
  do
      sleep 5
      echo "Waiting 5 seconds for $1 to start..."
  done
}

echo "Download required plugins"
wget "https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/$DELAYED_MESSAGE_EXCHANGE_PLUGIN_VERSION/rabbitmq_delayed_message_exchange-$DELAYED_MESSAGE_EXCHANGE_PLUGIN_VERSION.ez"

make -C "${PWD}"/tls-gen/basic

mkdir -p rabbitmq-configuration/tls
cp -R "${PWD}"/tls-gen/basic/result/* rabbitmq-configuration/tls
chmod o+r rabbitmq-configuration/tls/*

echo "[rabbitmq_jms_topic_exchange,rabbitmq_delayed_message_exchange]." > rabbitmq-configuration/enabled_plugins

echo "loopback_users = none

listeners.ssl.default = 5671

ssl_options.cacertfile = /etc/rabbitmq/tls/ca_certificate.pem
ssl_options.certfile   = /etc/rabbitmq/tls/server_$(hostname)_certificate.pem
ssl_options.keyfile    = /etc/rabbitmq/tls/server_$(hostname)_key.pem
ssl_options.verify     = verify_peer
ssl_options.fail_if_no_peer_cert = false" > rabbitmq-configuration/rabbitmq.conf

echo "Running RabbitMQ ${RABBITMQ_IMAGE}:${RABBITMQ_IMAGE_TAG}"

docker rm -f rabbitmq 2>/dev/null || echo "rabbitmq was not running"
docker run -d --name rabbitmq \
    --network host \
    -v "${PWD}"/rabbitmq-configuration:/etc/rabbitmq \
    -v "${PWD}"/rabbitmq_delayed_message_exchange-3.11.1.ez:/opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-3.11.1.ez \
    "${RABBITMQ_IMAGE}":"${RABBITMQ_IMAGE_TAG}"

wait_for_message rabbitmq "completed with"

docker exec rabbitmq rabbitmq-diagnostics erlang_version
docker exec rabbitmq rabbitmqctl version