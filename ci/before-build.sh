#!/bin/sh

CTL=${HOP_RABBITMQCTL:="DOCKER:rabbitmq"}
PLUGINS=${HOP_RABBITMQ_PLUGINS:="DOCKER:rabbitmq"}

case $CTL in
        DOCKER*)
          PLUGINS="docker exec ${CTL##*:} rabbitmq-plugins"
          CTL="docker exec ${CTL##*:} rabbitmqctl";;
esac

$PLUGINS enable rabbitmq_management

sleep 3

# guest:guest has full access to /

$CTL add_vhost /
$CTL add_user guest guest
$CTL set_permissions -p / guest ".*" ".*" ".*"
$CTL set_topic_permissions -p / guest amq.topic ".*" ".*"

$CTL add_vhost vh1
$CTL set_permissions -p vh1 guest ".*" ".*" ".*"

$CTL add_vhost vh2
$CTL set_permissions -p vh2 guest ".*" ".*" ".*"

# Reduce retention policy for faster publishing of stats
$CTL eval 'supervisor2:terminate_child(rabbit_mgmt_sup_sup, rabbit_mgmt_sup), application:set_env(rabbitmq_management,       sample_retention_policies, [{global, [{605, 1}]}, {basic, [{605, 1}]}, {detailed, [{10, 1}]}]), rabbit_mgmt_sup_sup:start_child().'
$CTL eval 'supervisor2:terminate_child(rabbit_mgmt_agent_sup_sup, rabbit_mgmt_agent_sup), application:set_env(rabbitmq_management_agent, sample_retention_policies, [{global, [{605, 1}]}, {basic, [{605, 1}]}, {detailed, [{10, 1}]}]), rabbit_mgmt_agent_sup_sup:start_child().'

# Enable shovel plugin
$PLUGINS enable rabbitmq_shovel
$PLUGINS enable rabbitmq_shovel_management
$PLUGINS enable rabbitmq_federation
$PLUGINS enable rabbitmq_federation_management

# Enable mqtt plugin
$PLUGINS enable rabbitmq_mqtt

# Enable stream plugin
$PLUGINS enable rabbitmq_stream
$PLUGINS enable rabbitmq_stream_management

# So that virtual host descriptions are available
$CTL enable_feature_flag all

true
