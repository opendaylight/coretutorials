package ncmount.impl.listener;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ncmount.impl.NcmountProvider;
import org.opendaylight.yang.gen.v1.org.opendaylight.coretutorials.ncmount.example.notifications.rev150611.ExampleNotificationsListener;
import org.opendaylight.yang.gen.v1.org.opendaylight.coretutorials.ncmount.example.notifications.rev150611.VrfRouteNotification;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom implementation of notification listener that measures the time until a specified number
 * of notifications was received. Can be used for performance testing of netconf notifications.
 */
public class PerformanceAwareNotificationListener implements ExampleNotificationsListener {

    private static final Logger LOG = LoggerFactory.getLogger(NcmountProvider.class);

    /**
     * Custom pattern to identify nodes where performance should be measured
     */
    private static final Pattern nodeIdPattern = Pattern.compile(".*-notif-([0-9]+)");

    private final AtomicLong notifCounter;
    private final long expectedNotificationCount;
    private Stopwatch stopWatch;
    private long totalPrefixesReceived = 0;

    public PerformanceAwareNotificationListener(final NodeId nodeId) {
        final Matcher matcher = nodeIdPattern.matcher(nodeId.getValue());
        Preconditions.checkArgument(matcher.matches());
        expectedNotificationCount = Long.valueOf(matcher.group(1));
        Preconditions.checkArgument(expectedNotificationCount > 0);
        this.notifCounter = new AtomicLong(this.expectedNotificationCount);
    }

    /**
     * Handle notifications and measure number of notifications/second
     * @param notification example notification
     */
    @Override
    public void onVrfRouteNotification(final VrfRouteNotification notification) {
        final long andDecrement = notifCounter.getAndDecrement();

        if(andDecrement == expectedNotificationCount) {
            this.stopWatch = Stopwatch.createStarted();
            LOG.info("First notification received at {}", stopWatch);
        }

        LOG.debug("Notification received, {} to go.", andDecrement);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Notification received: {}", notification);
        }

        totalPrefixesReceived += notification.getVrfPrefixes().getVrfPrefix().size();

        if(andDecrement == 1) {
            this.stopWatch.stop();
            LOG.info("Last notification received at {}", stopWatch);
            LOG.info("Elapsed ms for {} notifications: {}", expectedNotificationCount, stopWatch.elapsed(TimeUnit.MILLISECONDS));
            LOG.info("Performance (notifications/second): {}",
                    (expectedNotificationCount * 1.0/stopWatch.elapsed(TimeUnit.MILLISECONDS)) * 1000);
            LOG.info("Performance (prefixes/second): {}",
                    (totalPrefixesReceived * 1.0/stopWatch.elapsed(TimeUnit.MILLISECONDS)) * 1000);
        }

    }

    public static boolean shouldMeasurePerformance(final NodeId nodeId){
        return nodeIdPattern.matcher(nodeId.getValue()).matches();
    }

}
