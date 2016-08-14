/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

public class ShardTestStats {
    private final long txOk;
    private final long txError;
    private final long txSubmitted;
    private final long execTime;

    ShardTestStats(int txOk, int txError, int txSubmitted, long execTime) {
        this.txOk = txOk;
        this.txError = txError;
        this.txSubmitted = txSubmitted;
        this.execTime = execTime;
    }

    public long getTxOk() {
        return txOk;
    }

    public long getTxError() {
        return txError;
    }

    public long getTxSubmitted() {
        return txSubmitted;
    }

    public long getExecTime() {
        return execTime;
    }

}
