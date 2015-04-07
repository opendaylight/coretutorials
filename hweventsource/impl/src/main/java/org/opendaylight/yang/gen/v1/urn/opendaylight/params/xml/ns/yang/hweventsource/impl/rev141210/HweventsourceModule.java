/*
 *  Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.impl.rev141210;

import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.coretutorials.hweventsource.impl.HweventsourceBIProvider;
import org.opendaylight.coretutorials.hweventsource.sample.HelloWorldEventSourceManager;
import org.opendaylight.coretutorials.hweventsource.sample.SampleEventSourceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HweventsourceModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.impl.rev141210.AbstractHweventsourceModule {
	
	private static final Logger LOG = LoggerFactory.getLogger(HweventsourceModule.class);
	
	public HweventsourceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HweventsourceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.impl.rev141210.HweventsourceModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

//    	final HweventsourceBAProvider providerBA = new HweventsourceBAProvider();
    	final HweventsourceBIProvider providerBI = new HweventsourceBIProvider();
//    	final ProviderContext bindingCtx = getBrokerDependency().registerProvider(providerBA);
    	final ProviderSession domCtx = getDomBrokerDependency().registerProvider(providerBI);
        final Short numberSampleEventSources = getNumberEventSources();
        final Short messageGeneratePeriod = getMessageGeneratePeriod();
        final EventSourceRegistry eventSourceRegistry = getEventSourceRegistryDependency();

        final DOMNotificationPublishService domPublish = domCtx.getService(DOMNotificationPublishService.class);
        final HelloWorldEventSourceManager esm = new HelloWorldEventSourceManager(eventSourceRegistry);
        final SampleEventSourceGenerator seg = new SampleEventSourceGenerator(esm, domPublish, numberSampleEventSources, messageGeneratePeriod);
        LOG.info("Hello world event source has been started");
        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                esm.close();
                providerBI.close();
//                providerBA.close();
            }
        };

    }

}
