#!/bin/sh

sudo apt-get update -y
sudo apt-get install -y init-system-helpers socat adduser logrotate

cd /tmp/
wget https://github.com/rabbitmq/rabbitmq-server/releases/download/rabbitmq_v3_6_11/rabbitmq-server_3.6.11-1_all.deb
sudo dpkg --install rabbitmq-server_3.6.11-1_all.deb
sudo rm rabbitmq-server_3.6.11-1_all.deb

sleep 3