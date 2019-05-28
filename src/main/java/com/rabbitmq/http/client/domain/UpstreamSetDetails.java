package com.rabbitmq.http.client.domain;

/**
 * Any of the properties from an upstream can be overridden in an upstream set.
 */
public class UpstreamSetDetails extends UpstreamDetails {
    /**
     * The name of an upstream. Mandatory.
     */
    private String upstream;

    public String getUpstream() {
        return upstream;
    }

    public UpstreamSetDetails setUpstream(String upstream) {
        this.upstream = upstream;
        return this;
    }

    @Override
    public String toString() {
        return "UpstreamSetDetails{" +
                "upstream='" + upstream + '\'' +
                "} " + super.toString();
    }
}
