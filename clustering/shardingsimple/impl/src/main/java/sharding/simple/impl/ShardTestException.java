/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.impl;

public class ShardTestException extends Exception {
    /**
     * @param message: Exception message
     */
    public ShardTestException(String message) {
        super(message);
    }

    /**
     * @param cause: Exception cause
     */
    public ShardTestException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message: Exception message
     * @param cause: Exception cause
     */
    public ShardTestException(String message, Throwable cause) {
        super(message, cause);
    }

}
