/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema;

import com.microsoft.dagx.schema.policy.PolicySchema;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.util.Set;

public class SchemaExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(SchemaRegistry.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sr = new SchemaRegistryImpl();
        sr.register(new PolicySchema());

        context.registerService(SchemaRegistry.class, sr);
        monitor.info("Initialized Schema Registry");

    }
}
