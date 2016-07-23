/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package clustering.impl;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class that holds information about the host where program is executing.
 * @author jmedved
 *
 */
public class HostInfo {
    private static final Logger LOG = LoggerFactory.getLogger(HostInfo.class);

    private String hostName = "Host Name not specified";
    private final List<String> ipAddresses = new ArrayList<>();

    /** Constructor where private fields for the class get initialized.
     *
     */
    public HostInfo() {
        // Get all IP addresses on all interfaces
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                List<InterfaceAddress> ifAddrs = interfaces.nextElement().getInterfaceAddresses();
                for (InterfaceAddress ifAddr : ifAddrs) {
                    ipAddresses.add(ifAddr.getAddress().getHostAddress());
                }
            }
        } catch (SocketException e1) {
            LOG.info("Could not initialize IP Addresses, {}", e1);
        }

        // Get the hostname
        try {
            InetAddress ip = InetAddress.getLocalHost();
            this.hostName = ip.getHostName();

        } catch (UnknownHostException e) {
            LOG.info("Could not initialize host name, {}", e);
        }

        LOG.info("Initialized HostInfo: hostName {}, ipAddresses {}", hostName, ipAddresses);
}

    public String getHostName() {
        return hostName;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public long getJvmUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

}
