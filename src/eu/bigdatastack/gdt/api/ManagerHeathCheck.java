package eu.bigdatastack.gdt.api;

import com.codahale.metrics.health.HealthCheck;

public class ManagerHeathCheck extends HealthCheck {

    public ManagerHeathCheck() {}

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
