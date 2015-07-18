package ntfbenchmark.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.NtfbenchPayloadListener;

public class NtfBenchTestListener implements NtfbenchPayloadListener {

    private final int expectedSize;
    private int received = 0;

    public NtfBenchTestListener(final int expectedSize) {
        this.expectedSize = expectedSize;
    }

    @Override
    public void onNtfbench(final Ntfbench notification) {
        if (expectedSize == notification.getPayload().size()) {
            received++;
        }
    };

    public int getReceived() {
        return received;
    }
}
