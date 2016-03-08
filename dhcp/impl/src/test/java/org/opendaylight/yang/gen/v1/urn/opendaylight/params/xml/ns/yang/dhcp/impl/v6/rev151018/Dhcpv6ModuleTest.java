package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Ignore;
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

@Ignore
public class Dhcpv6ModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "dhcpv6ModuleTestInstance";
    private static final List<String> NETWORK_INTERFACES = Arrays.<String>asList("lo");
    private static final PortNumber PORT = new PortNumber(5470);

    @Before
    public void setUp() {
        Dhcpv6ModuleFactory dhcpv6ModuleFactory = new Dhcpv6ModuleFactory();
        Examplev6LeaseManagerModuleFactory examplev6LeaseManagerModuleFactory =
                new Examplev6LeaseManagerModuleFactory();
        NettyThreadgroupModuleFactory nettyThreadgroupModuleFactory = new NettyThreadgroupModuleFactory();

        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, dhcpv6ModuleFactory,
                examplev6LeaseManagerModuleFactory, nettyThreadgroupModuleFactory));
    }

    @Test
    public void testCreateBean()
            throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);

        CommitStatus commitStatus = transaction.commit();

        assertBeanCount(1, Dhcpv6ModuleFactory.NAME);
        assertStatus(commitStatus, 3, 0, 0);
    }

    @Test
    public void testDestroy() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, Dhcpv6ModuleFactory.NAME);
        CommitStatus commitStatus = transaction.commit();

        assertBeanCount(1, Dhcpv6ModuleFactory.NAME);
        assertStatus(commitStatus, 0, 0, 3);
    }

    @Test
    public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException, InstanceNotFoundException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, Dhcpv6ModuleFactory.NAME);

        DhcpModuleMXBean dhcpv6ModuleMXBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(Dhcpv6ModuleFactory.NAME, INSTANCE_NAME), DhcpModuleMXBean.class);
        dhcpv6ModuleMXBean
            .setLeaseManager(transaction.createModule(Examplev6LeaseManagerModuleFactory.NAME, "otherv6LeaseManager"));
        dhcpv6ModuleMXBean
            .setWorkerThreadGroup(transaction.createModule(NettyThreadgroupModuleFactory.NAME, "otherThreadGroup"));
        CommitStatus commitStatus = transaction.commit();

        assertBeanCount(1, Dhcpv6ModuleFactory.NAME);
        assertStatus(commitStatus, 2, 1, 2);
    }

    private ObjectName createInstance(ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        ObjectName dhcpv6Name = transaction.createModule(Dhcpv6ModuleFactory.NAME, INSTANCE_NAME);
        Dhcpv6ModuleMXBean dhcpv6MXBean = transaction.newMXBeanProxy(dhcpv6Name, Dhcpv6ModuleMXBean.class);
        ObjectName v6LeaseManagerName =
                transaction.createModule(Examplev6LeaseManagerModuleFactory.NAME, "v6LeaseManager");
        ObjectName threadgroupName = transaction.createModule(NettyThreadgroupModuleFactory.NAME, "threadgroup");

        dhcpv6MXBean.setWorkerThreadGroup(threadgroupName);
        dhcpv6MXBean.setPort(PORT);
        dhcpv6MXBean.setNetworkInterface(NETWORK_INTERFACES);
        // default options
        dhcpv6MXBean.setServerId(1);
        dhcpv6MXBean.setLeaseManager(v6LeaseManagerName);

        return dhcpv6Name;
    }

}
