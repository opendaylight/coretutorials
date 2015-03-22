/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ietfsyslog.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.Syslog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.ConsoleLoggingAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.ConsoleLoggingActionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.FileLoggingAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.RemoteLoggingAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.TerminalLoggingAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.file.logging.action.LoggingFiles;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.remote.logging.action.RemoteLoggingDestination;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.LoggingAdvancedLevelProcessing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.LoggingAdvancedLevelProcessing.SelectMessageSeverity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.LoggingLevelScope;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.logging.level.scope.LoggingFacility;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.logging.level.scope.LoggingFacilityAll;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.logging.level.scope.LoggingFacilityNone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.selector.logging.level.scope.logging.facility.LoggingFacilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.terminal.logging.action.UserScope;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.terminal.logging.action.user.scope.PerUser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.terminal.logging.action.user.scope.AllUsers;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.rev150305.syslog.terminal.logging.action.user.scope.per.user.UserName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.types.rev150305.Severity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.syslog.types.rev150305.SyslogFacility;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardOpenOption.*;

import java.nio.file.*;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.io.*;

public class IetfsyslogProvider implements BindingAwareProvider,
		DataChangeListener, AutoCloseable {

	private static final Logger LOG = LoggerFactory
			.getLogger(IetfsyslogProvider.class);
	private ProviderContext providerContext;
	private DataBroker dataService;
	private ListenerRegistration<DataChangeListener> dcReg;
	private PrintWriter config;
	public static final InstanceIdentifier<Syslog> SYSLOG_IID = InstanceIdentifier
			.builder(Syslog.class).build();

	private String processSelector(LoggingLevelScope scope,
			LoggingAdvancedLevelProcessing adv_proc) {
		String selector_str = "";
		String adv_op = "";

		if (adv_proc != null) {
			if (adv_proc.getSelectMessageSeverity() == SelectMessageSeverity.Equals) {
				adv_op = "=";
			} else if (adv_proc.getSelectMessageSeverity() == SelectMessageSeverity.NotEquals) {
				adv_op = "!=";
			}
		}

		if (scope instanceof LoggingFacility) {
			List<LoggingFacilities> facility_list = ((LoggingFacility) scope)
					.getLoggingFacilities();
			Iterator<LoggingFacilities> iterator = facility_list.iterator();
			while (iterator.hasNext()) {
				LoggingFacilities logging_facility = iterator.next();
				String facility = BindingReflections.findQName(
						logging_facility.getFacility()).getLocalName();
				Severity severity = logging_facility.getSeverity();
				if (!selector_str.isEmpty()) {
					selector_str += "; ";
				}
				selector_str += facility + "." + adv_op
						+ severity.toString().toLowerCase();
			}
		} else if (scope instanceof LoggingFacilityAll) {
			Severity severity = ((LoggingFacilityAll) scope).getSeverity();
			selector_str = "*." + adv_op + severity.toString().toLowerCase();
		} else if (scope instanceof LoggingFacilityNone) {
			selector_str = "*.none";
		}
	    return String.format("%-50s", this.formatout(selector_str, 50).toString());
	}

	private StringBuilder formatout(String input, int maxPerLine) {
		boolean isFirstLine = true;
		int avail = maxPerLine;
		String offstring = String.format("%-7s", "");
		StringBuilder strCache = new StringBuilder();

		String out[] = input.split(";");

		for (int i = 0; i < out.length; i++) {
			if (isFirstLine) {
				if (i < out.length - 1) { // not the last
					String ss = out[i] + ";";
					if (ss.length() >= avail) {
						strCache.append(ss).append("\\\r\n").append(offstring);
						avail = maxPerLine - offstring.length();
						isFirstLine = false;
					} else { // (ss.length() < avail)
						strCache.append(ss);
						avail -= ss.length();
						if (avail < out[i + 1].length()) {
							strCache.append("\\\r\n").append(offstring);
							avail = maxPerLine - offstring.length();
							isFirstLine = false;
						}
					}
				} else { // the last
					String ss = out[i];
					strCache.append(ss);
					isFirstLine = false;
				}

			} else { // !if (isFirstLine)
				if (i < out.length - 1) { // not the last
					String ss = out[i] + ";";
					strCache.append(ss);
					avail -= ss.length();
					if (avail < out[i + 1].length()) {
						strCache.append("\\\r\n").append(offstring);
						avail = maxPerLine - offstring.length();
					}
				} else { // the last
					String ss = out[i];
					strCache.append(ss);
				}
			}
		} // loop

		return strCache;
	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		LOG.info("IetfsyslogProvider Session Initiated");
		this.providerContext = session;
		this.dataService = session.getSALService(DataBroker.class);
		// Register the DataChangeListener for Toaster's configuration subtree
		if (dataService != null) {
			dcReg = dataService.registerDataChangeListener(
					LogicalDatastoreType.CONFIGURATION, SYSLOG_IID, this,
					DataChangeScope.SUBTREE);
		}
		LOG.info("onSessionInitiated: initialization done");
	}

	@Override
	public void close() throws Exception {
		// Close active registrations
		if (dcReg != null) {
			dcReg.close();
		}
		LOG.info("IetfsyslogProvider Closed");
	}

	/**
	 * Receives data change events on syslog's configuration subtree. This
	 * method processes syslog configuration data entered by ODL users through
	 * the ODL REST API.
	 */
	@Override
	public void onDataChanged(
			AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
		DataObject dataObject = arg0.getUpdatedSubtree();
		if (dataObject instanceof Syslog) {
			Syslog syslog = (Syslog) dataObject;
			PrintStream config = null;
			Date date = new Date();
			try {
				config = new PrintStream("rsyslog.conf");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			config.println("#  Config rules for rsyslog.");
			config.println("#");
			config.println("#  Generated by OpenDaylight on " + date.toString());
			config.println("#");
			config.println("#  For more information see rsyslog.conf(5) and /etc/rsyslog.conf");
			config.println("#");
			if (syslog.getConsoleLoggingAction() != null) {
				ConsoleLoggingAction CLA = syslog.getConsoleLoggingAction();
				config.print(processSelector(CLA.getLoggingLevelScope(),
						CLA.getLoggingAdvancedLevelProcessing()));
				config.println("/dev/console");
			}
			if (syslog.getFileLoggingAction() != null) {
				FileLoggingAction FLA = syslog.getFileLoggingAction();
				List<LoggingFiles> files = FLA.getLoggingFiles();
				Iterator<LoggingFiles> iterator = files.iterator();
				while (iterator.hasNext()) {
					LoggingFiles file = iterator.next();
					config.print(processSelector(file.getLoggingLevelScope(),
							file.getLoggingAdvancedLevelProcessing()));
					String file_name = file.getFileName().getValue();
					config.println(file_name);
				}
			}
			if (syslog.getRemoteLoggingAction() != null) {
				RemoteLoggingAction RLA = syslog.getRemoteLoggingAction();
				List<RemoteLoggingDestination> dests = RLA
						.getRemoteLoggingDestination();
				Iterator<RemoteLoggingDestination> iterator = dests.iterator();
				while (iterator.hasNext()) {
					RemoteLoggingDestination dest = iterator.next();
					config.print(processSelector(dest.getLoggingLevelScope(),
							dest.getLoggingAdvancedLevelProcessing()));
					String destination = new String(dest.getDestination()
							.getValue());
					String port = "";
					if (dest.getDestinationPort() != null) {
						port = ":"
								+ dest.getDestinationPort().getValue()
										.toString();
					}
					config.println("@" + destination + port);
				}
			}
			if (syslog.getTerminalLoggingAction() != null) {
				TerminalLoggingAction TLA = syslog.getTerminalLoggingAction();
				UserScope user_scope = TLA.getUserScope();
				if (user_scope instanceof AllUsers) {
					config.print(processSelector(((AllUsers) user_scope)
							.getAllUsers().getLoggingLevelScope(),
							((AllUsers) user_scope).getAllUsers()
									.getLoggingAdvancedLevelProcessing()));
					config.println(":omusrmsg:*");
				} else if (user_scope instanceof PerUser) {
					List<UserName> users = ((PerUser) user_scope).getUserName();
					Iterator<UserName> iterator = users.iterator();
					while (iterator.hasNext()) {
						UserName user = iterator.next();
						config.print(processSelector(
								user.getLoggingLevelScope(),
								user.getLoggingAdvancedLevelProcessing()));
						config.println(":omusrmsg:" + user.getUname());
					}
				}
			}
			if (syslog.getGlobalLoggingAction() != null) {
				/* global-logging-action is not handled by Linux */
			}
			if (syslog.getBufferedLoggingAction() != null) {
				/* buffered-logging-action is not handled by Linux */
			}
			config.flush();
			config.close();
			LOG.info("onDataChanged - new Syslog config: {}", syslog);
		} else {
			LOG.warn("onDataChanged - not instance of Syslog {}", dataObject);
		}
	}
}
