package org.example.server;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public interface SocketMessageReader {
	ZMsg read(ZMQ.Socket socket);
}
