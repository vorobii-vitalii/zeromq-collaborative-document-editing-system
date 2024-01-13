package org.example;

import java.util.List;

import org.example.components.DaggerDocumentServerComponent;
import org.example.server.ZeroMQRequestReplyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class DocumentRequestReplyServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentRequestReplyServer.class);

	public static void main(String[] args) {
		var component = DaggerDocumentServerComponent.create();
		var routerSocket = component.routerSocket();
		var dealerSocket = component.dealerSocket();
		LOGGER.info("Creating proxy from {} to {}", routerSocket, dealerSocket);

		var workers = component.createDocumentRequestReplyWorkers();
		LOGGER.info("Starting workers...");
		startWorkers(workers);
		LOGGER.info("Workers started");

		ZMQ.proxy(routerSocket, dealerSocket, null);
		LOGGER.info("For some reason main server stopped...");
	}

	private static void startWorkers(List<ZeroMQRequestReplyServer> workers) {
		workers.forEach(worker -> {
			var thread = new Thread(worker::startServer);
			thread.setDaemon(true);
			thread.start();
		});
	}

}
