package org.example.request_handling;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import document.editing.RequestHolder;

public interface RequestHandler {
	byte getHandledType();
	void handleRequest(RequestHolder requestHolder, ZMQ.Socket socket, ZMsg msg);
}
