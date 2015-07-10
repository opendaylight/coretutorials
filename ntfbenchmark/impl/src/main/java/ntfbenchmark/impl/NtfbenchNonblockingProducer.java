package ntfbenchmark.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;

import com.google.common.util.concurrent.ListenableFuture;

public class NtfbenchNonblockingProducer extends NtfbenchAbstractProducer {

	public NtfbenchNonblockingProducer(
			NotificationPublishService publishService, int iterations,
			int payloadSize) {
		super(publishService, iterations, payloadSize);
	}

	@Override
	public void run() {
		int ntfOk = 0;
		int ntfError = 0;
		
		for (int i = 0; i < this.iterations; i++) {
			try {
				ListenableFuture<? extends Object> result = this.publishService.offerNotification(this.ntf);
				if (NotificationPublishService.REJECTED == result) {
					ntfError++;					
				} else {
					ntfOk++;
				}
			} catch (Exception e) {
				ntfError++;
			}
		}
		
		this.ntfOk = ntfOk;
		this.ntfError = ntfError;
	}

}
