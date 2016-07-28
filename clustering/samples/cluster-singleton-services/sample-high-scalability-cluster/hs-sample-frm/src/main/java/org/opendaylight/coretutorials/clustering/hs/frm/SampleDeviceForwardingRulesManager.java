/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.frm;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.sample.node.common.rev160722.SampleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class SampleDeviceForwardingRulesManager implements ClusteredDataTreeChangeListener<SampleNode> {
    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceForwardingRulesManager.class);

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<SampleNode>> changes) {
        // TODO Auto-generated method stub

    }

}
