package org.example.request_handling.impl;

import static org.example.constants.DocumentElementSchema.CHAR_ID;
import static org.example.constants.DocumentElementSchema.DISAMBIGUATOR;
import static org.example.constants.DocumentElementSchema.DOCUMENT_ID;
import static org.example.constants.DocumentElementSchema.IS_RIGHT;
import static org.example.constants.DocumentElementSchema.PARENT_CHAR_ID;
import static org.example.constants.DocumentElementSchema.VALUE;

import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.example.request_handling.RequestHandler;
import org.example.serialization.DocumentElementFlatBufferSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import co.elastic.apm.api.CaptureSpan;
import document.editing.GetRequest;
import document.editing.Request;
import document.editing.RequestHolder;
import io.micrometer.core.instrument.DistributionSummary;

public class GetRequestHandler implements RequestHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GetRequestHandler.class);

	private static final int NO_OPS = 0;
	private static final int SEND_MORE_OPS = ZMQ.SNDMORE;
	private static final int BATCH_SIZE = 1000;
	public static final int ENABLED = 1;

	public static final Document READ_DOCUMENT_PROJECTION = new Document()
			.append(CHAR_ID, ENABLED)
			.append(PARENT_CHAR_ID, ENABLED)
			.append(IS_RIGHT, ENABLED)
			.append(DISAMBIGUATOR, ENABLED)
			.append(VALUE, ENABLED);

	private final MongoCollection<Document> documentsCollection;
	private final DistributionSummary counterBytesWritten;
	private final DistributionSummary counterDocumentElementsWritten;
	private final DocumentElementFlatBufferSerializer serializer = new DocumentElementFlatBufferSerializer();

	public GetRequestHandler(
			MongoCollection<Document> documentsCollection,
			DistributionSummary counterBytesWritten,
			DistributionSummary counterDocumentElementsWritten
	) {
		this.documentsCollection = documentsCollection;
		this.counterBytesWritten = counterBytesWritten;
		this.counterDocumentElementsWritten = counterDocumentElementsWritten;
	}

	@Override
	public byte getHandledType() {
		return Request.GetRequest;
	}

	@CaptureSpan
	@Override
	public void handleRequest(RequestHolder requestHolder, ZMQ.Socket socket, ZMsg msg) {
		var getRequest = (GetRequest) requestHolder.request(new GetRequest());
		if (getRequest != null) {
			var documentId = getRequest.documentId();
			LOGGER.info("Processing get document request, documentId = {}", documentId);
			final var count = new AtomicInteger();
			// Approximately because ZeroMQ adds headers...
			final var totalBytesWrittenApproximately = new AtomicInteger();
			msg.getLast().destroy();
			try (var cursor = fetchDocumentElementsByDocumentId(documentId)) {
				cursor.forEachRemaining(document -> {
					var flatBuffer = serializer.serialize(document);
					var arr = flatBuffer.sizedByteArray();
					msg.add(arr);
					count.incrementAndGet();
					totalBytesWrittenApproximately.addAndGet(arr.length);
				});
				if (count.get() == 0) {
					msg.add(new byte[] {});
				}
				msg.send(socket);
			}
			LOGGER.info("{} document elements written", count.get());
			counterDocumentElementsWritten.record(count.get());
			counterBytesWritten.record(totalBytesWrittenApproximately.get());
		}
	}

	private MongoCursor<Document> fetchDocumentElementsByDocumentId(int documentId) {
		return documentsCollection
				.find(createFilterByDocumentId(documentId))
				.batchSize(BATCH_SIZE)
				.projection(READ_DOCUMENT_PROJECTION)
				.cursor();
	}

	private static Bson createFilterByDocumentId(int documentId) {
		return Filters.eq(DOCUMENT_ID, documentId);
	}

}
