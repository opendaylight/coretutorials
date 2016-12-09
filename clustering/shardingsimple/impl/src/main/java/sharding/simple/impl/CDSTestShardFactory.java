package sharding.simple.impl;

import akka.cluster.Cluster;
import java.util.Collection;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;

public class CDSTestShardFactory implements ShardFactory {

    private final DistributedShardFactory shardFactoryDelegate;
    private final ActorSystemProvider actorSystemProvider;

    public CDSTestShardFactory(DistributedShardFactory shardFactoryDelegate, ActorSystemProvider actorSystemProvider) {
        this.shardFactoryDelegate = shardFactoryDelegate;
        this.actorSystemProvider = actorSystemProvider;
    }

    @Override
    public ShardRegistration createShard(DOMDataTreeIdentifier prefix)
            throws DOMDataTreeShardingConflictException,
            DOMDataTreeShardCreationFailedException, DOMDataTreeProducerException {
        Cluster cluster = Cluster.get(actorSystemProvider.getActorSystem());
        Collection<MemberName> replicas =
                cluster.state().getAllRoles().stream().map(MemberName::forName).collect(Collectors.toList());
        return shardFactoryDelegate.createDistributedShard(prefix, replicas)::close;
    }
}
