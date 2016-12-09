package sharding.simple.impl;

import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yangtools.concepts.Registration;

public interface ShardFactory {

    ShardRegistration createShard(DOMDataTreeIdentifier prefix) throws DOMDataTreeShardingConflictException, DOMDataTreeShardCreationFailedException, DOMDataTreeProducerException;

    interface ShardRegistration extends Registration {}

}
