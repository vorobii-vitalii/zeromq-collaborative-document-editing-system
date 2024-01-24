package org.example.server;

import java.util.Iterator;
import java.util.function.Supplier;

import org.example.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;

public class ZeroMQPublishingEventsServer<EVENT_TYPE> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZeroMQPublishingEventsServer.class);

	private final Serializer<EVENT_TYPE> serializer;
	private final Supplier<Iterator<EVENT_TYPE>> eventsIteratorSupplier;
	private final ZMQ.Socket serverSocket;
	private final Counter eventsCounter;
	private final DistributionSummary bytesWritten;

	public ZeroMQPublishingEventsServer(
			Serializer<EVENT_TYPE> serializer,
			Supplier<Iterator<EVENT_TYPE>> eventsIteratorSupplier,
			ZMQ.Socket serverSocket,
			Counter eventsCounter,
			DistributionSummary bytesWritten
	) {
		this.serializer = serializer;
		this.eventsIteratorSupplier = eventsIteratorSupplier;
		this.serverSocket = serverSocket;
		this.eventsCounter = eventsCounter;
		this.bytesWritten = bytesWritten;
	}

	public void startPublishingServer() {
		LOGGER.info("Starting publishing server...");
		Iterator<EVENT_TYPE> events = eventsIteratorSupplier.get();
		events.forEachRemaining(event -> {
			eventsCounter.increment();
			LOGGER.info("Received one more event {}", event);
			serializer.serialize(event).ifPresent(bytes -> {
				bytesWritten.record(bytes.length);
				serverSocket.send(bytes);
			});
		});
		LOGGER.info("All events have been written, exiting server...");
	}

}
