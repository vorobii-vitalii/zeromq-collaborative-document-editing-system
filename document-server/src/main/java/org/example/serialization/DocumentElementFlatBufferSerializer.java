package org.example.serialization;

import static org.example.constants.DocumentElementSchema.CHAR_ID;
import static org.example.constants.DocumentElementSchema.DISAMBIGUATOR;
import static org.example.constants.DocumentElementSchema.IS_RIGHT;
import static org.example.constants.DocumentElementSchema.PARENT_CHAR_ID;
import static org.example.constants.DocumentElementSchema.VALUE;

import org.bson.Document;

import com.google.flatbuffers.FlatBufferBuilder;

import document.editing.DocumentElement;
import document.editing.Response;
import document.editing.ResponseHolder;


public class DocumentElementFlatBufferSerializer {
	private static final int INITIAL_NUM_BYTES = 150;

	public FlatBufferBuilder serialize(Document document) {
		var builder = new FlatBufferBuilder(INITIAL_NUM_BYTES);
		var charIdOffset = builder.createString(document.getString(CHAR_ID));
		var parentCharId = document.getString(PARENT_CHAR_ID);
		var parentIdOffset = parentCharId != null ? builder.createString(parentCharId) : 0;
		DocumentElement.startDocumentElement(builder);
		DocumentElement.addCharId(builder, charIdOffset);
		DocumentElement.addParentCharId(builder, parentIdOffset);
		DocumentElement.addIsRight(builder, document.getBoolean(IS_RIGHT));
		var disambiguator = document.getInteger(DISAMBIGUATOR);
		if (disambiguator != null) {
			DocumentElement.addDisambiguator(builder, disambiguator);
		}
		var character = document.getInteger(VALUE);
		DocumentElement.addCharacter(builder, character == null ? 0 : character);
		var documentElementOffset = DocumentElement.endDocumentElement(builder);
		ResponseHolder.startResponseHolder(builder);
		ResponseHolder.addResponseType(builder, Response.DocumentElement);
		ResponseHolder.addResponse(builder, documentElementOffset);
		ResponseHolder.finishResponseHolderBuffer(builder, ResponseHolder.endResponseHolder(builder));
		return builder;
	}
}
