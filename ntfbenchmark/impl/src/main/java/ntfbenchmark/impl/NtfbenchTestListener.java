package ntfbenchmark.impl;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.NtfbenchPayloadListener;

import com.google.common.util.concurrent.Futures;

public class NtfbenchTestListener implements NtfbenchPayloadListener {

    private final int expectedSize;
    private int received = 0;

    public NtfbenchTestListener(final int expectedSize) {
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
    
	public Future<?> getAllDone() {
		return Futures.immediateFuture(null);
	}

}
