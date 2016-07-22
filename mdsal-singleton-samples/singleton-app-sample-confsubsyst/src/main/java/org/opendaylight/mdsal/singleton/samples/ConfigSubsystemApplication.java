/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.singleton.samples;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;

/**
 *
 */
public class ConfigSubsystemApplication implements AutoCloseable, ClusterSingletonService {

    private static final ServiceGroupIdentifier IDENT = ServiceGroupIdentifier.create("confAppIdent");

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENT;
    }

    @Override
    public void instantiateServiceInstance() {
        // TODO Auto-generated method stub

    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        // TODO Auto-generated method stub
        return null;
    }

}
