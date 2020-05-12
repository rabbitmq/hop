#!/bin/sh

wget https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.8.3/rabbitmq-server-generic-unix-3.8.3.tar.xz
tar xf rabbitmq-server-generic-unix-3.8.3.tar.xz
mv rabbitmq_server-3.8.3 rabbitmq

rabbitmq/sbin/rabbitmq-server -detached

sleep 10

rabbitmq/sbin/rabbitmqctl status

true
