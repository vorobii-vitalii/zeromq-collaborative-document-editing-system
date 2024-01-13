package org.example.server;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.request_handling.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import co.elastic.apm.api.CaptureTransaction;
import document.editing.RequestHolder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;

public class ZeroMQRequestReplyServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZeroMQRequestReplyServer.class);

	private final ServerCondition serverCondition;
	private final SocketMessageReader socketMessageReader;
	private final Map<Byte, RequestHandler> requestHandlerByOperationCode;
	private final Counter countRequests;
	private final Timer requestProcessingTimer;
	private final ZMQ.Socket replySocket;
	private final DistributionSummary requestBytesDistribution;

	public ZeroMQRequestReplyServer(
			ServerCondition serverCondition,
			SocketMessageReader socketMessageReader,
			Set<RequestHandler> requestHandlers,
			Counter countRequests,
			Timer requestProcessingTimer,
			ZMQ.Socket replySocket,
			DistributionSummary requestBytesDistribution
	) {
		this.serverCondition = serverCondition;
		this.socketMessageReader = socketMessageReader;
		this.requestHandlerByOperationCode = requestHandlers
				.stream()
				.collect(Collectors.toMap(RequestHandler::getHandledType, v -> v));
		this.countRequests = countRequests;
		this.requestProcessingTimer = requestProcessingTimer;
		this.replySocket = replySocket;
		this.requestBytesDistribution = requestBytesDistribution;
	}

	public void startServer() {
		LOGGER.info("Starting request-reply server");
		while (serverCondition.shouldProcessRequests()) {
			var zMsg = socketMessageReader.read(replySocket);
			processRequest(zMsg, replySocket);
		}
		LOGGER.info("Exiting server...");
	}

	@CaptureTransaction
	private void processRequest(ZMsg msg, ZMQ.Socket socket) {
		countRequests.increment();
		requestBytesDistribution.record(msg.contentSize());
		LOGGER.info("Processing message of size {} bytes...", msg.contentSize());
		requestProcessingTimer.record(() -> {
			var requestHolder = RequestHolder.getRootAsRequestHolder(ByteBuffer.wrap(msg.getLast().getData()));
			var requestType = requestHolder.requestType();
			var requestHandler = requestHandlerByOperationCode.get(requestType);
			if (requestHandler != null) {
				requestHandler.handleRequest(requestHolder, socket, msg);
			} else {
				LOGGER.warn("Cannot process message of type = {}, sending empty response", requestHolder.requestType());
				msg.send(socket);
			}
		});
	}

}
