package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import org.apache.directory.server.dhcp.options.DhcpOption;

import com.google.common.io.BaseEncoding;

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