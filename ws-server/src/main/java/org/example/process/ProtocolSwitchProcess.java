package org.example.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class ProtocolSwitchProcess implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolSwitchProcess.class);

	private final ZMQ.Socket frontend;
	private final ZMQ.Socket backend;

	public ProtocolSwitchProcess(ZMQ.Socket frontend, ZMQ.Socket backend) {
		this.frontend = frontend;
		this.backend = backend;
	}

	@Override
	public void run() {
		LOGGER.info("Starting protocol switch process!");
		ZMQ.proxy(frontend, backend, null);
	}
}
