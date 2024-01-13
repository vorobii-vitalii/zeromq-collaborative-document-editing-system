package org.example.request_handling.impl;

import static org.example.constants.DocumentElementSchema.CHAR_ID;
import static org.example.constants.DocumentElementSchema.DISAMBIGUATOR;
import static org.example.constants.DocumentElementSchema.DOCUMENT_ID;
import static org.example.constants.DocumentElementSchema.IS_RIGHT;
import static org.example.constants.DocumentElementSchema.PARENT_CHAR_ID;
import static org.example.constants.DocumentElementSchema.VALUE;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.example.request_handling.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;

import co.elastic.apm.api.CaptureSpan;
import document.editing.ChangeRequest;
import document.editing.ChangeResponse;
import document.editing.Request;
import document.editing.RequestHolder;
import document.editing.Response;
import document.editing.ResponseHolder;
import io.micrometer.core.instrument.Counter;

public class ChangeRequestHandler implements RequestHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeRequestHandler.class);
	protected static final String SET = "$set";
	protected static final UpdateOptions UPSERT_UPDATE_OPTIONS = new UpdateOptions().upsert(true);
	private static final int INITIAL_NUM_BYTES = 150;

	private final MongoCollection<Document> documentsCollection;
	private final Counter totalChangesCount;

	public ChangeRequestHandler(MongoCollection<Document> documentsCollection, Counter totalChangesCount) {
		this.documentsCollection = documentsCollection;
		this.totalChangesCount = totalChangesCount;
	}

	@Override
	public byte getHandledType() {
		return Request.ChangeRequest;
	}

	@CaptureSpan
	@Override
	public void handleRequest(RequestHolder requestHolder, ZMQ.Socket socket, ZMsg msg) {
		var changeRequest = (ChangeRequest) requestHolder.request(new ChangeRequest());
		if (changeRequest != null) {
			var documentId = changeRequest.documentId();
			var changesVector = changeRequest.changesVector();
			var n = changesVector.length();
			LOGGER.info("Applying {} changes to document = {}", n, documentId);
			List<UpdateOneModel<Document>> databaseUpdates = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				var change = changesVector.get(i);
				if (change.hasCharacter()) {
					databaseUpdates.add(new UpdateOneModel<>(
							new Document()
									.append(DOCUMENT_ID, documentId)
									.append(PARENT_CHAR_ID, change.parentCharId())
									.append(IS_RIGHT, change.isRight())
									.append(DISAMBIGUATOR, change.disambiguator()),
							new Document().append(
									SET,
									new Document().append(VALUE, change.character()).append(CHAR_ID, change.charId())
							),
							UPSERT_UPDATE_OPTIONS
					));
				} else {
					databaseUpdates.add(new UpdateOneModel<>(
							Filters.and(
									Filters.eq(DOCUMENT_ID, documentId),
									Filters.eq(CHAR_ID, change.charId())
							),
							new Document().append(SET, new Document(VALUE, null))
					));
				}
			}
			totalChangesCount.increment(n);
			if (databaseUpdates.isEmpty()) {
				LOGGER.warn("Empty change request...");
			} else {
				documentsCollection.bulkWrite(databaseUpdates);
			}
			LOGGER.info("Applied {} updates to document {}", n, documentId);
			var builder = new FlatBufferBuilder(INITIAL_NUM_BYTES);
			ChangeResponse.startChangeResponse(builder);
			var changesResponseOffset = ChangeResponse.endChangeResponse(builder);
			ResponseHolder.startResponseHolder(builder);
			ResponseHolder.addResponseType(builder, Response.ChangeResponse);
			ResponseHolder.addResponse(builder, changesResponseOffset);
			ResponseHolder.finishResponseHolderBuffer(builder, ResponseHolder.endResponseHolder(builder));
			msg.getLast().reset(builder.sizedByteArray());
			msg.send(socket);
		}
	}

}
