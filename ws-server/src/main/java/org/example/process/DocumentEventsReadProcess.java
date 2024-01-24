package org.example.process;

import java.util.List;
import java.util.Map;

import org.example.server.Deserializer;
import org.example.server.ServerCondition;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.google.flatbuffers.FlatBufferBuilder;

import document.editing.DocumentElement;
import document.editing.DocumentUpdatedEvent;
import document.editing.Response;
import document.editing.ResponseHolder;
import io.netty.buffer.Unpooled;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.netty.http.websocket.WebsocketOutbound;

public class DocumentEventsReadProcess implements Runnable {
	private static final int INITIAL_NUM_BYTES = 512;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEventsReadProcess.class);

	private final ZMQ.Socket documentEventsServiceSocket;
	private final Map<Integer, List<WebsocketOutbound>> subscribersMap;
	private final ServerCondition serverCondition;
	private final Deserializer<DocumentUpdatedEvent> documentUpdatedEventDeserializer;

	public DocumentEventsReadProcess(
			ZMQ.Socket documentEventsServiceSocket,
			Map<Integer, List<WebsocketOutbound>> subscribersMap,
			ServerCondition serverCondition,
			Deserializer<DocumentUpdatedEvent> documentUpdatedEventDeserializer
	) {
		this.documentEventsServiceSocket = documentEventsServiceSocket;
		this.subscribersMap = subscribersMap;
		this.serverCondition = serverCondition;
		this.documentUpdatedEventDeserializer = documentUpdatedEventDeserializer;
	}

	@Override
	public void run() {
		while (serverCondition.shouldContinue()) {
			var newMessageBytes = documentEventsServiceSocket.recv();
			if (newMessageBytes == null) {
				continue;
			}
			LOGGER.info("Received new message of length = {}", newMessageBytes.length);
			var documentUpdatedEvent = documentUpdatedEventDeserializer.deserialize(newMessageBytes);
			int documentId = documentUpdatedEvent.documentId();
			LOGGER.info("Received update on document {}", documentId);
			var subscribers = subscribersMap.get(documentId);
			if (subscribers == null) {
				LOGGER.info("No subscribers to document {}. Ignoring...", documentId);
			} else {
				var builder = toDocumentElement(documentUpdatedEvent);
				subscribers.forEach(subscriber -> subscriber.send(Flux.just(Unpooled.wrappedBuffer(builder.sizedByteArray())))
						.subscribe(new CoreSubscriber<>() {
							@Override
							public void onSubscribe(Subscription s) {
								s.request(1);
							}

							@Override
							public void onNext(Void unused) {
								LOGGER.info("Buffer written!");
							}

							@Override
							public void onError(Throwable t) {
								LOGGER.error("Error on write", t);
							}

							@Override
							public void onComplete() {
								LOGGER.info("Update write completed...");
							}
						}));
			}
		}
	}

	private FlatBufferBuilder toDocumentElement(DocumentUpdatedEvent documentUpdatedEvent) {
		var builder = new FlatBufferBuilder(INITIAL_NUM_BYTES);
		var charIdOffset = builder.createString(documentUpdatedEvent.charId());
		var parentCharId = documentUpdatedEvent.parentCharId();
		var parentIdOffset = parentCharId != null ? builder.createString(parentCharId) : 0;
		var documentElementOffset = DocumentElement.createDocumentElement(
				builder,
				charIdOffset,
				parentIdOffset,
				documentUpdatedEvent.isRight(),
				documentUpdatedEvent.disambiguator(),
				documentUpdatedEvent.character()
		);
		ResponseHolder.finishResponseHolderBuffer(
				builder,
				ResponseHolder.createResponseHolder(builder, Response.DocumentElement, documentElementOffset));
		return builder;
	}

}
