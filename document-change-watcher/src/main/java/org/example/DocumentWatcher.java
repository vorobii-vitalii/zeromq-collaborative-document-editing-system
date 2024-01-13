package org.example;

import org.example.components.DaggerDocumentWatcherComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentWatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentWatcher.class);

	public static void main(String[] args) {
		LOGGER.info("Starting server...");
		var server = DaggerDocumentWatcherComponent.create().createServer();
		server.startPublishingServer();
	}

}
