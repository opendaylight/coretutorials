package ntfbenchmark.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;

public class NtfbenchBlockingProducer extends AbstractNtfbenchProducer {

    public NtfbenchBlockingProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        super(publishService, iterations, payloadSize);
    }

    @Override
    public void run() {
        int ntfOk = 0;
        int ntfError = 0;

        for (int i = 0; i < this.iterations; i++) {
            try {
                this.publishService.putNotification(this.ntf);
                ntfOk++;
            } catch (final Exception e) {
                ntfError++;
            }
        }

        this.ntfOk = ntfOk;
        this.ntfError = ntfError;
    }
}
