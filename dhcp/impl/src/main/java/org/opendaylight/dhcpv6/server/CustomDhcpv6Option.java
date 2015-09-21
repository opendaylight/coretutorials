/*
 * Copyright (c) 2015 Cisco Systems and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcpv6.server;

import org.anarres.dhcp.v6.options.Dhcp6Option;

import com.google.common.io.BaseEncoding;

/**
 * Custom HEX based DHCPv6 option
 */
public class CustomDhcpv6Option extends Dhcp6Option {

    private final short tag;

    public CustomDhcpv6Option(short tag, String hexadecimalDataRepresentation) throws IllegalArgumentException {
        this.tag = tag;
        setData(BaseEncoding.base16().decode(hexadecimalDataRepresentation.toUpperCase()));
    }

    @Override
    public short getTag() {
        return tag;
    }

}
