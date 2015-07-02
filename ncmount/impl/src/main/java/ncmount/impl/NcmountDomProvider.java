package ncmount.impl;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class NcmountDomProvider implements Provider, AutoCloseable, DOMRpcImplementation, DOMDataChangeListener {

    public static final YangInstanceIdentifier NETCONF_TOPO_IID;
    private static final DOMRpcIdentifier SHOW_NODE_RPC_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create(ShowNodeInput.QNAME, "show-node")));

    static {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder();
        builder
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), TopologyNetconf.QNAME.getLocalName());

        NETCONF_TOPO_IID = builder.build();
    }

    private DOMMountPointService mountPointService;

    @Override
    public void onSessionInitiated(final Broker.ProviderSession providerSession) {
        // get the DOM versions of MD-SAL services
        final DOMDataBroker dataBroker = providerSession.getService(DOMDataBroker.class);
        this.mountPointService = providerSession.getService(DOMMountPointService.class);

        final DOMRpcProviderService service = providerSession.getService(DOMRpcProviderService.class);
        service.registerRpcImplementation(this, SHOW_NODE_RPC_ID);
        // TODO do the same for 2 other rpcs

        final YangInstanceIdentifier nodeIid = YangInstanceIdentifier.builder(NETCONF_TOPO_IID).node(Node.QNAME).build();

        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                nodeIid,
                this,
                AsyncDataBroker.DataChangeScope.SUBTREE);
    }


    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        // Deprecated, not using
        return Collections.emptySet();
    }

    @Override
    public void close() throws Exception {
        // TODO close resources
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final DOMRpcIdentifier domRpcIdentifier, final NormalizedNode<?, ?> normalizedNode) {
        if(domRpcIdentifier.equals(SHOW_NODE_RPC_ID)) {
            return showNode(normalizedNode);
        }
        return null;
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> showNode(final NormalizedNode<?, ?> normalizedNode) {
        // TODO handle

        // Normalized node is the generic supertype for all normalized nodes e.g. container node, map node etc.
        // Its really important to know the normalized node structure when trying to parse/itarate over normalized nodes

        final YangInstanceIdentifier iid = null;
        mountPointService.getMountPoint(iid).get().getService(DOMDataBroker.class).get().newReadOnlyTransaction();
        mountPointService.getMountPoint(iid).get().getService(DOMRpcService.class).get().invokeRpc(null, null);
        mountPointService.getMountPoint(iid).get().getService(DOMNotificationService.class);
        return null;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> asyncDataChangeEvent) {
        // TODO the data change has to be handled in the same way as in NcmountProvider
    }
}
