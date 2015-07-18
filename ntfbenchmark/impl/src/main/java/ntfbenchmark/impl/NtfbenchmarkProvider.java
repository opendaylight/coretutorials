/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.NtfbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.TestStatusOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtfbenchmarkProvider implements BindingAwareProvider, AutoCloseable, NtfbenchmarkService {

    private static final Logger LOG = LoggerFactory.getLogger(NtfbenchmarkProvider.class);
    private NotificationPublishService publishService;
    private NotificationService listenService;
    private static final int testTimeout = 5;

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("NtfbenchmarkProvider Session Initiated");

        publishService = session.getSALService(NotificationPublishService.class);
        listenService = session.getSALService(NotificationService.class);

        session.addRpcImplementation(NtfbenchmarkService.class, this);
    }

    @Override
    public void close() throws Exception {
        LOG.info("NtfbenchmarkProvider Closed");
    }

    @Override
    public Future<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
        final int producerCount = input.getNumProducts().intValue();
        final int listenerCount = input.getNumListeners().intValue();
        final int iterations = input.getIterations().intValue();
        final int payloadSize = input.getIterations().intValue();

        final List<AbstractNtfbenchProducer> producers = new ArrayList<>(producerCount);
        final List<ListenerRegistration<NtfBenchTestListener>> listeners = new ArrayList<>(listenerCount);
        for (int i = 0; i < producerCount; i++) {
            producers.add(new NtfbenchBlockingProducer(publishService, iterations, payloadSize));
        }

        for (int i = 0; i < listenerCount; i++) {
            final NtfBenchTestListener listener = new NtfBenchTestListener(payloadSize);
            listeners.add(listenService.registerNotificationListener(listener));
        }

        try {
            final ExecutorService executor = Executors.newFixedThreadPool(input.getNumProducts().intValue());

            LOG.info("Test Started");
            final long startTime = System.nanoTime();

            for (int i = 0; i < input.getNumProducts().intValue(); i++) {
                executor.submit(producers.get(i));
            }
            executor.shutdown();
            try {
                executor.awaitTermination(testTimeout, TimeUnit.MINUTES);
            } catch (final InterruptedException e) {
                LOG.error("Out of time: test did not finish within the {} min deadline ", testTimeout);
            }

            final long endTime = System.nanoTime();
            LOG.info("Test Done");

            final long elapsedTime = endTime - startTime;

            long allListeners = 0;
            long allProducersOk = 0;
            long allProducersError = 0;

            for (final ListenerRegistration<NtfBenchTestListener> listenerRegistration : listeners) {
                allListeners += listenerRegistration.getInstance().getReceived();
            }

            for (final AbstractNtfbenchProducer abstractNtfbenchProducer : producers) {
                allProducersOk += abstractNtfbenchProducer.getNtfOk();
                allProducersError += abstractNtfbenchProducer.getNtfError();
            }

            final StartTestOutput output =
                    new StartTestOutputBuilder().setExecTime(elapsedTime).setListenerOk(allListeners)
                            .setProducerOk(allProducersOk).setProducerError(allProducersError).build();
            return RpcResultBuilder.success(output).buildFuture();
        } finally {
            for (final ListenerRegistration<NtfBenchTestListener> listenerRegistration : listeners) {
                listenerRegistration.close();
            }
        }
    }

    @Override
    public Future<RpcResult<TestStatusOutput>> testStatus() {
        // TODO Auto-generated method stub
        return null;
    }

}
