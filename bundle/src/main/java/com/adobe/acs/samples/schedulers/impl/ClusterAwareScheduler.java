/*
 * #%L
 * ACS AEM Samples
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.samples.schedulers.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "ACS AEM Samples - Cluster Aware Scheduled Service",
    description = "Sample scheduled service using CQ 5.6/AEM6 TopologyEventListener method",
    immediate = true // Load immediately
)

@Properties({
    @Property(
            label = "Cron expression defining when this Scheduled Service will run",
            description = "[every minute = 0 * * * * ?] Visit www.cronmaker.com to generate cron expressions.",
            name = "scheduler.expression",
            value = "0 1 0 ? * *"
    ),
    @Property(
            label = "Allow concurrent executions",
            description = "Allow concurrent executions of this Scheduled Service. This is almost always false.",
            name = "scheduler.concurrent",
            propertyPrivate = true,
            boolValue = false
    )
})

@Service
public class ClusterAwareScheduler implements Runnable, TopologyEventListener {
    private final Logger log = LoggerFactory.getLogger(ClusterAwareScheduler.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private boolean isLeader = false;

    @Override
    public void run() {
        // Scheduled services that do not have to be cluster aware do not need
        // to implement this check OR extend TopologyEventListener
        if (!isLeader) {
            return;
        }

        // Scheduled service logic, only run on the Master
        ResourceResolver adminResourceResolver = null;
        try {
            // Be careful not to leak the adminResourceResolver
            adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            // execute your scheduled service logic here ...

        } catch (LoginException e) {
            log.error("Error obtaining the admin resource resolver.", e);
        } finally {
            // ALWAYS close resolvers you open
            if (adminResourceResolver != null) {
                adminResourceResolver.close();
            }
        }
    }

    /** Topology Aware Methods **/

    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if (event.getType() == TopologyEvent.Type.TOPOLOGY_CHANGED
                || event.getType() == TopologyEvent.Type.TOPOLOGY_INIT) {
            this.isLeader = event.getNewView().getLocalInstance().isLeader();
        }
    }
}
