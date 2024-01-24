package org.example.serialization;

import static org.example.constants.DocumentElementSchema.CHAR_ID;
import static org.example.constants.DocumentElementSchema.DISAMBIGUATOR;
import static org.example.constants.DocumentElementSchema.DOCUMENT_ID;
import static org.example.constants.DocumentElementSchema.IS_RIGHT;
import static org.example.constants.DocumentElementSchema.PARENT_CHAR_ID;
import static org.example.constants.DocumentElementSchema.VALUE;

import java.util.Optional;

import org.bson.Document;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import document.editing.DocumentUpdatedEvent;

public class DocumentUpdatedEventSerializer implements Serializer<ChangeStreamDocument<Document>> {
	private static final int INITIAL_BUFFER_SIZE = 150;

	@Override
	public Optional<byte[]> serialize(ChangeStreamDocument<Document> changeStreamDocument) {
		var document = changeStreamDocument.getFullDocument();
		if (document == null) {
			return Optional.empty();
		}
		var builder = new FlatBufferBuilder(INITIAL_BUFFER_SIZE);
		var parentCharId = document.getString(PARENT_CHAR_ID);
		var parentIdOffset = parentCharId != null ? builder.createString(parentCharId) : 0;
		var charIdOffset = builder.createString(document.getString(CHAR_ID));
		DocumentUpdatedEvent.startDocumentUpdatedEvent(builder);
		DocumentUpdatedEvent.addCharId(builder, charIdOffset);
		DocumentUpdatedEvent.addParentCharId(builder, parentIdOffset);
		DocumentUpdatedEvent.addIsRight(builder, document.getBoolean(IS_RIGHT));
		DocumentUpdatedEvent.addDocumentId(builder, document.getInteger(DOCUMENT_ID));
		var disambiguator = document.getInteger(DISAMBIGUATOR);
		if (disambiguator != null) {
			DocumentUpdatedEvent.addDisambiguator(builder, disambiguator);
		}
		var character = document.getInteger(VALUE);
		if (character != null) {
			DocumentUpdatedEvent.addCharacter(builder, character);
		}
		DocumentUpdatedEvent.finishDocumentUpdatedEventBuffer(builder, DocumentUpdatedEvent.endDocumentUpdatedEvent(builder));
		return Optional.of(builder.sizedByteArray());
	}
}
