package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.util.Collections;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.DhcpModuleFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.DhcpModuleMXBean;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.ExampleLeaseManagerModuleFactory;

public class DhcpModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "dhcpModuleTestInstance";
    private static final PortNumber PORT = new PortNumber(5067);

    @Before
    public void setUp() {
        final DhcpModuleFactory dhcpModuleFactory = new DhcpModuleFactory();
        final ExampleLeaseManagerModuleFactory leaseManagerModuleFactory = new ExampleLeaseManagerModuleFactory();
        final NettyThreadgroupModuleFactory nettyThreadgroupModuleFactory = new NettyThreadgroupModuleFactory();

        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, dhcpModuleFactory,
            leaseManagerModuleFactory, nettyThreadgroupModuleFactory));
    }

    @Test
    public void testCreateBean() throws ConflictingVersionException, ValidationException, InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);

        CommitStatus commitStatus = transaction.commit();

        assertBeanCount(1, DhcpModuleFactory.NAME);
        assertStatus(commitStatus, 3, 0, 0);
    }

    @Test
    public void testDestroy() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, DhcpModuleFactory.NAME);
        CommitStatus commitStatus = transaction.commit();

        assertBeanCount(1, DhcpModuleFactory.NAME);
        assertStatus(commitStatus, 0, 0, 3);
    }

    @Test
    public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException, InstanceNotFoundException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, DhcpModuleFactory.NAME);

        DhcpModuleMXBean dhcpModuleMXBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(DhcpModuleFactory.NAME, INSTANCE_NAME),
                DhcpModuleMXBean.class);
        dhcpModuleMXBean.setLeaseManager(transaction.createModule(ExampleLeaseManagerModuleFactory.NAME, "otherLeaseManager"));
        dhcpModuleMXBean.setWorkerThreadGroup(transaction.createModule(NettyThreadgroupModuleFactory.NAME, "otherThreadgroup"));
        CommitStatus commitStatus = transaction.commit();

         assertBeanCount(1, DhcpModuleFactory.NAME);
         assertStatus(commitStatus, 2, 1, 2);
    }

    private ObjectName createInstance(final ConfigTransactionJMXClient transaction)  throws InstanceAlreadyExistsException {
        ObjectName dhcpModuleName = transaction.createModule(DhcpModuleFactory.NAME, INSTANCE_NAME);
        DhcpModuleMXBean dhcpModuleMXBean = transaction.newMXBeanProxy(dhcpModuleName, DhcpModuleMXBean.class);
        ObjectName leaseManagerModuleName =
                transaction.createModule(ExampleLeaseManagerModuleFactory.NAME, "leaseManager");
        ObjectName threadgroupModuleName = transaction.createModule(NettyThreadgroupModuleFactory.NAME, "threadgroup");

        dhcpModuleMXBean.setLeaseManager(leaseManagerModuleName);
        dhcpModuleMXBean.setNetworkInterface(Collections.singletonList("lo"));
        dhcpModuleMXBean.setPort(PORT);
        dhcpModuleMXBean.setWorkerThreadGroup(threadgroupModuleName);

        return dhcpModuleName;
    }

}
