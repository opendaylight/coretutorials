package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import com.google.common.base.Predicate;
import java.io.IOException;
import java.net.NetworkInterface;
import org.anarres.dhcp.common.address.InterfaceAddress;
import org.apache.directory.server.dhcp.netty.DhcpServer;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210.AbstractDhcpModule {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpModule.class);

    public DhcpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DhcpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210.DhcpModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public static void main(String[] args) {
        startServer(67, new ExampleLeaseManagerModule.StaticLeaseManager("9.9.9.9"));
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Starting DHCP server on port {}", getPort());
        return startServer(getPort(), getLeaseManagerDependency());
    }

    private static AutoCloseable startServer(final Integer port, final LeaseManager manager) {
        final DhcpServer dhcpServer = new DhcpServer(manager, port);

        try {
            // TODO make the interface list configurable
            dhcpServer.addInterfaces(new Predicate<NetworkInterface>() {
                public boolean apply(final NetworkInterface input) {
                    return true;
                }
            });
            // Add localhost
            dhcpServer.addInterface(InterfaceAddress.forString("127.0.0.1/0"));
            dhcpServer.addInterface(InterfaceAddress.forString("127.0.0.1"));
            dhcpServer.addInterface(InterfaceAddress.forString("::1/128"));
            // TODO dont use start, provide nio event loop group ourselves
            dhcpServer.start();
            LOG.info("DHCP server started on port {}", port);

            return new AutoCloseable() {
                @Override public void close() throws Exception {
                    LOG.debug("Stopping DHCP server on port {}", port);
                    dhcpServer.stop();
                    LOG.info("DHCP server stopper  on port {}", port);
                }
            };
        } catch (IOException e) {
            // FIXME
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            // FIXME
            throw new IllegalStateException(e);
        }
    }

}
