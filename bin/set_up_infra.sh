#!/bin/sh

wget https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.8.14/rabbitmq-server-generic-unix-3.8.14.tar.xz
tar xf rabbitmq-server-generic-unix-3.8.14.tar.xz
mv rabbitmq_server-3.8.14 rabbitmq

rabbitmq/sbin/rabbitmq-server -detached

rabbitmq/sbin/rabbitmqctl await_startup
sleep 7
rabbitmq/sbin/rabbitmqctl status

true
