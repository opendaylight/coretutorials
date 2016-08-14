/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import java.util.Collection;
import java.util.Map;

import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/** A DataTreeChangeListener for testing. It doesn't do anything except
 *  for recording that it's been invoked.
 * @author jmedved
 *
 */
class ShardTestListener implements DOMDataTreeListener {
    private Long dataTreeEventsOk = (long)0;
    private Long dataTreeEventsFail = (long)0;

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> collection,
            final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> map) {
        dataTreeEventsOk++;
    }

    @Override
    public void onDataTreeFailed(final Collection<DOMDataTreeListeningException> collection) {
        dataTreeEventsFail++;
    }

    public Long getDataTreeEventsOk() {
        return dataTreeEventsOk;
    }

    public Long getDataTreeEventsFail() {
        return dataTreeEventsFail;
    }
}
