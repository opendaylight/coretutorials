/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.server;

import org.apache.directory.server.dhcp.options.DhcpOption;

import com.google.common.io.BaseEncoding;

/**
 * Custom HEX based DHCP option
 */
public class CustomDhcpOption extends DhcpOption {
    private final byte tag;

    public CustomDhcpOption(byte tag, String hexadecimalDataRepresentation) throws IllegalArgumentException {
        this.tag = tag;
        setData(BaseEncoding.base16().decode(hexadecimalDataRepresentation.toUpperCase()));
    }

    @Override
    public byte getTag() {
        return tag;
    }

}