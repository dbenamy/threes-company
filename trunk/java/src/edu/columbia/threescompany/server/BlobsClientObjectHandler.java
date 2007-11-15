package edu.columbia.threescompany.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;

import org.quickserver.net.server.ClientHandler;
import org.quickserver.net.server.ClientObjectHandler;

import edu.columbia.threescompany.client.communication.MoveStatePair;

public class BlobsClientObjectHandler implements ClientObjectHandler {
	public void handleObject(ClientHandler handler, Object obj)
	throws SocketTimeoutException, IOException {
		if (!(obj instanceof MoveStatePair))
			throw new RuntimeException("Incorrect object received over stream!");
		
		MoveStatePairQueue.instance().add((MoveStatePair) obj);
	}
}
