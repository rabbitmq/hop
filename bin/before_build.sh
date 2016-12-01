#!/bin/sh

${HOP_RABBITMQCTL:="sudo rabbitmqctl"}

# guest:guest has full access to /

$HOP_RABBITMQCTL add_vhost /
$HOP_RABBITMQCTL add_user guest guest
$HOP_RABBITMQCTL set_permissions -p / guest ".*" ".*" ".*"

# Reduce retention policy for faster publishing of stats
$HOP_RABBITMQCTL eval 'supervisor2:terminate_child(rabbit_mgmt_sup_sup, rabbit_mgmt_sup), application:set_env(rabbitmq_management,       sample_retention_policies, [{global, [{605, 1}]}, {basic, [{605, 1}]}, {detailed, [{10, 1}]}]), rabbit_mgmt_sup_sup:start_child().'
$HOP_RABBITMQCTL eval 'supervisor2:terminate_child(rabbit_mgmt_agent_sup_sup, rabbit_mgmt_agent_sup), application:set_env(rabbitmq_management_agent, sample_retention_policies, [{global, [{605, 1}]}, {basic, [{605, 1}]}, {detailed, [{10, 1}]}]), rabbit_mgmt_agent_sup_sup:start_child().'
