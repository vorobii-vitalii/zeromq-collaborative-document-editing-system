package org.example.modules;

import java.util.Iterator;
import java.util.function.Supplier;

import javax.inject.Named;
import javax.print.Doc;

import org.bson.Document;
import org.example.serialization.DocumentUpdatedEventSerializer;
import org.example.server.ZeroMQPublishingEventsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@Module
public class PublishingServerModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(PublishingServerModule.class);
	private static final String TCP_BIND_ADDRESS_TEMPLATE = "tcp://*:%d";

	@Provides
	ZContext zeroMqContext() {
		return new ZContext();
	}

	@Provides
	@Named("eventPublishSocket")
	ZMQ.Socket eventPublishSocket(ZContext context) {
		var socket = context.createSocket(SocketType.PUB);
		var port = getPort();
		socket.bind(TCP_BIND_ADDRESS_TEMPLATE.formatted(port));
		LOGGER.info("Created TCP reply socket, port = {}...", port);
		return socket;
	}

	@Provides
	ZeroMQPublishingEventsServer<ChangeStreamDocument<Document>> documentChangedEventPublishingServer(
			@Named("eventPublishSocket") ZMQ.Socket serverSocket,
			Supplier<Iterator<ChangeStreamDocument<Document>>> getChangeStreamCursor,
			MeterRegistry meterRegistry
	) {
		return new ZeroMQPublishingEventsServer<>(
				new DocumentUpdatedEventSerializer(),
				getChangeStreamCursor,
				serverSocket,
				Counter.builder("document.update.events.counter").register(meterRegistry),
				DistributionSummary.builder("document.update.total.bytes.written").register(meterRegistry)
		);
	}

	@Provides
	public Supplier<Iterator<ChangeStreamDocument<Document>>> getChangeStreamCursor(
			@Named("documentElementsCollection") MongoCollection<Document> collection
	) {
		return () -> (Iterator<ChangeStreamDocument<Document>>) collection.watch().fullDocument(FullDocument.UPDATE_LOOKUP).cursor();
	}

	private static int getPort() {
		return Integer.parseInt(System.getenv("PORT"));
	}

}
