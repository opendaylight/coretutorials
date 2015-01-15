package org.opendaylight.toaster;

// !!! NOTE: The imports must be in this order, or checkstyle will not pass!!!
// In Eclipse, use CONTROL+SHIFT+o or CMD+SHIFT+o (mac) to properly order imports

// MD-SAL APIs
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;

// Interfaces generated from the toaster yang model
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;

// Yangtools methods to manipulate RPC DTOs
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterImpl implements BindingAwareProvider, DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ToasterImpl.class);

    private ProviderContext providerContext;
    private DataBroker dataService;
    private ListenerRegistration<DataChangeListener> dcReg;

    public static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();

    /**************************************************************************
     * AutoCloseable Method
     *************************************************************************/
    /**
     * Called when MD-SAL closes the active session. Cleanup is performed, i.e.
     * all active registrations with MD-SAL are closed,
     */
    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

        // Close active registrations
        dcReg.close();

        LOG.info("ToasterImpl: registrations closed");
    }

    /**************************************************************************
     * BindingAwareProvider Methods
     *************************************************************************/
    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.providerContext = session;
        this.dataService = session.getSALService(DataBroker.class);

        // Register the DataChangeListener for Toaster's configuration subtree
        dcReg = dataService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                                                TOASTER_IID,
                                                this,
                                                DataChangeScope.SUBTREE );

        LOG.info("onSessionInitiated: initialization done");
    }

    /**************************************************************************
     * DataChangeListener Methods
     *************************************************************************/

    /**
     * Receives data change events on toaster's configuration subtree. This
     * method processes toaster configuration data entered by ODL users through
     * the ODL REST API.
     */
    @Override
    public void onDataChanged( final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        DataObject dataObject = change.getUpdatedSubtree();
        if( dataObject instanceof Toaster ) {
            Toaster toaster = (Toaster) dataObject;
            LOG.info("onDataChanged - new Toaster config: {}", toaster);
        } else {
            LOG.warn("onDataChanged - not instance of Toaster {}", dataObject);
        }
    }
}