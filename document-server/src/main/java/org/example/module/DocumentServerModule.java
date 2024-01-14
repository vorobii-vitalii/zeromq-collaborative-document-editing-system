package org.example.module;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.bson.Document;
import org.example.constants.ServerConstants;
import org.example.request_handling.RequestHandler;
import org.example.request_handling.impl.ChangeRequestHandler;
import org.example.request_handling.impl.GetRequestHandler;
import org.example.server.ZeroMQRequestReplyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.mongodb.client.MongoCollection;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@Module
public class DocumentServerModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentServerModule.class);
	public static final int DEFAULT_NUM_WORKERS = 10;

	@Singleton
	@Provides
	ZContext zeroMqContext() {
		LOGGER.info("Created ZMQ context...");
		return new ZContext(4);
	}

	@Provides
	@Named("routerSocket")
	ZMQ.Socket routerSocket(ZContext context, @Named("serverAddress") String serverAddress) {
		var socket = context.createSocket(SocketType.ROUTER);
		boolean isBound = socket.bind(serverAddress);
		LOGGER.info("Created router socket, url = {} bound = {}", getServerAddress(), isBound);
		return socket;
	}

	@Provides
	@Named("dealerSocket")
	ZMQ.Socket dealerSocket(ZContext context) {
		var socket = context.createSocket(SocketType.DEALER);
		boolean wasBound = socket.bind(ServerConstants.WORKERS_ZMQ_ADDRESS);
		LOGGER.info("Created dealer socket, url = {} bound = {}", ServerConstants.WORKERS_ZMQ_ADDRESS, wasBound);
		return socket;
	}

	// Request handlers Start

	@Provides
	@IntoSet
	RequestHandler getRequestHandler(
			@Named("documentElementsCollection") MongoCollection<Document> documentsCollection,
			MeterRegistry meterRegistry
	) {
		return new GetRequestHandler(
				documentsCollection,
				DistributionSummary.builder("document.get.bytes.written").baseUnit("bytes").register(meterRegistry),
				DistributionSummary.builder("request.size.get.document.elements.written").register(meterRegistry)
		);
	}

	@Provides
	@IntoSet
	RequestHandler changeRequestHandler(
			@Named("documentElementsCollection") MongoCollection<Document> documentsCollection,
			MeterRegistry meterRegistry
	) {
		return new ChangeRequestHandler(documentsCollection, meterRegistry.counter("total.changes"));
	}

	// Request handlers End

	@Provides
	List<ZeroMQRequestReplyServer> requestReplyWorkers(
			Set<RequestHandler> requestHandlers,
			MeterRegistry meterRegistry,
			ZContext context
	) {
		return IntStream.range(0, getNumberOfWorkers())
				.mapToObj(v -> new ZeroMQRequestReplyServer(
						() -> !Thread.currentThread().isInterrupted(),
						ZMsg::recvMsg,
						requestHandlers,
						meterRegistry.counter("messages.count"),
						meterRegistry.timer("message.processing.time"),
						createWorkerSocket(context, v),
						DistributionSummary.builder("message.request.size.bytes").baseUnit("bytes").register(meterRegistry)))
				.toList();
	}

	private ZMQ.Socket createWorkerSocket(ZContext context, int id) {
		var workerSocket = context.createSocket(SocketType.REP);
		workerSocket.setIdentity(("Worker_" + id).getBytes(StandardCharsets.UTF_8));
		final boolean connected = workerSocket.connect(ServerConstants.WORKERS_ZMQ_ADDRESS);
		LOGGER.info("Created worker socket url = {} connected = {}", ServerConstants.WORKERS_ZMQ_ADDRESS, connected);
		return workerSocket;
	}

	@Provides
	@Named("serverAddress")
	public String getServerAddress() {
		return System.getenv("SERVER_URL");
	}

	private static int getNumberOfWorkers() {
		return Optional.ofNullable(System.getenv("NUM_WORKERS"))
				.map(Integer::parseInt)
				.orElse(DEFAULT_NUM_WORKERS);
	}

}
