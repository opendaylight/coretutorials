package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.net.InetAddress;
import org.apache.directory.server.dhcp.options.DhcpOption;
import com.google.common.base.Charsets;
import com.google.common.primitives.Ints;

public class CustomDhcpOption extends DhcpOption {
	private final byte tag;
	
	public CustomDhcpOption(byte tag, byte[] data) {
		this.tag = tag;
		this.setData(data);
	}
	
	public CustomDhcpOption(byte tag, InetAddress address) {
		this.tag = tag;
		this.setData(address.getAddress());
	}
	
	public CustomDhcpOption(byte tag, String string) {
		this.tag = tag;
		this.setData(string.getBytes(Charsets.ISO_8859_1));
	}
	
	public CustomDhcpOption(byte tag, int integer) {
		this.tag = tag;
		this.setData(Ints.toByteArray(integer));
	}
	
	public CustomDhcpOption(byte tag, InetAddress[] addresses) {
		this.tag = tag;
		byte[] data = new byte[addresses.length * 4];
		for (int i = 0; i < addresses.length; i++) {
			System.arraycopy(addresses[i].getAddress(), 0, data, i * 4, 4);
		}
		this.setData(data);
	}

	@Override
	public byte getTag() {
		return tag;
	}

}