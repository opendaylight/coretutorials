package org.opendaylight.toaster;

// !!! NOTE: The imports must be in this order, or checkstyle will not pass!!!
// In Eclipse, use CONTROL+SHIFT+o or CMD+SHIFT+o (mac) to properly order imports

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

// MD-SAL APIs
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;

// Interfaces generated from the toaster yang model
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterBuilder;

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
    private static final DisplayString TOASTER_MANUFACTURER = new DisplayString("Opendaylight");
    private static final DisplayString TOASTER_MODEL_NUMBER = new DisplayString("Model 1 - Binding Aware");

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

        // Initialize operational and default config data in MD-SAL data store
        initToasterOperational();
        initToasterConfiguration();

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

    /**************************************************************************
     * ToasterImpl Private Methods
     *************************************************************************/

    /**
     * Populates toaster's initial operational data into the MD-SAL operational
     * data store.
     * Note - we are simulating a device whose manufacture and model are fixed
     * (embedded) into the hardware. / This is why the manufacture and model
     * number are hardcoded.
     */
    private void initToasterOperational() {
        // Build the initial toaster operational data
        Toaster toaster = new ToasterBuilder().setToasterManufacturer( TOASTER_MANUFACTURER )
                .setToasterModelNumber( TOASTER_MODEL_NUMBER )
                .setToasterStatus( ToasterStatus.Up )
                .build();

        // Put the toaster operational data into the MD-SAL data store
        WriteTransaction tx = dataService.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TOASTER_IID, toaster);

        // Perform the tx.submit asynchronously
        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("initToasterOperational: transaction succeeded");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("initToasterOperational: transaction failed");
            }
        });

        LOG.info("initToasterOperational: operational status populated: {}", toaster);
    }

    /**
     * Populates toaster's default config data into the MD-SAL configuration
     * data store.  Note the database write to the tree are done in a synchronous fashion
     */
    private void initToasterConfiguration() {
        // Build the default toaster config data
        Toaster toaster = new ToasterBuilder().setDarknessFactor((long)1000)
                .build();

        // Place default config data in data store tree
        WriteTransaction tx = dataService.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, TOASTER_IID, toaster);
        // Perform the tx.submit synchronously
        tx.submit();

        LOG.info("initToasterConfiguration: default config populated: {}", toaster);
    }
}