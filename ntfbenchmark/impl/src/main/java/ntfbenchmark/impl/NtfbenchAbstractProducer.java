package ntfbenchmark.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.NtfbenchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.Payload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.PayloadBuilder;

public abstract class NtfbenchAbstractProducer implements Runnable {
    protected final NotificationPublishService publishService;
    protected final int iterations;
    protected final Ntfbench ntf;
    
	/**
	 * @return the ntfOk
	 */
	public int getNtfOk() {
		return ntfOk;
	}

	/**
	 * @return the ntfError
	 */
	public int getNtfError() {
		return ntfError;
	}

	protected int ntfOk = 0;
	protected int ntfError = 0;

	public NtfbenchAbstractProducer(NotificationPublishService publishService,
			int iterations, int payloadSize) {
		this.publishService = publishService;
		this.iterations = iterations;
		
		List<Payload> listVals = new ArrayList<>();
		for (int i = 0; i < payloadSize; i++) {
			listVals.add(new PayloadBuilder().setId(i).build());
		}

		ntf = new NtfbenchBuilder().setPayload(listVals).build();
	}
}
