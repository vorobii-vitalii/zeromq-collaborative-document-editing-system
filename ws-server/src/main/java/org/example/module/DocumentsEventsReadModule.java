package org.example.module;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.example.process.DocumentEventsReadProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import dagger.Module;
import dagger.Provides;
import document.editing.DocumentUpdatedEvent;
import reactor.netty.http.websocket.WebsocketOutbound;

@Module(includes = ZeroMQModule.class)
public class DocumentsEventsReadModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsEventsReadModule.class);

	@Singleton
	@Provides
	@Named("documentEventServerSubscribeSocket")
	ZMQ.Socket documentEventServerSubscribeSocket(ZContext context, @Named("documentEventServerURL") String documentEventServerURL) {
		var socket = context.createSocket(SocketType.SUB);
		LOGGER.info("Establishing connection to document event server, url = {}", documentEventServerURL);
		socket.connect(documentEventServerURL);
		socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
		return socket;
	}

	@Provides
	@Singleton
	@Named("subscribersByDocumentId")
	ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId() {
		return new ConcurrentHashMap<>();
	}

	@Singleton
	@Provides
	DocumentEventsReadProcess documentEventsReadProcess(
			@Named("documentEventServerSubscribeSocket")
			ZMQ.Socket documentEventServerSubscribeSocket,
			@Named("subscribersByDocumentId")
			ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId
	) {
		return new DocumentEventsReadProcess(
				documentEventServerSubscribeSocket,
				subscribersByDocumentId,
				() -> !Thread.currentThread().isInterrupted(),
				b -> DocumentUpdatedEvent.getRootAsDocumentUpdatedEvent(ByteBuffer.wrap(b))
		);
	}

	@Provides
	@Named("documentEventServerURL")
	public String getDocumentEventServer() {
		return System.getenv("DOCUMENT_EVENT_SERVER_URL");
	}

}
