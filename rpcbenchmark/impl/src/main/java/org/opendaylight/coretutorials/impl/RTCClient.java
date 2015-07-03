package org.opendaylight.coretutorials.impl;

public interface RTCClient {		
	long getRpcOk(); 
	long getRpcError(); 	
	void runTest(int iterations); 
	void close();
}
