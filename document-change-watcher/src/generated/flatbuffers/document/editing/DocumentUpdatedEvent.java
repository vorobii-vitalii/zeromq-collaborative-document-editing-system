// automatically generated by the FlatBuffers compiler, do not modify

package document.editing;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.BooleanVector;
import com.google.flatbuffers.ByteVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.DoubleVector;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FloatVector;
import com.google.flatbuffers.IntVector;
import com.google.flatbuffers.LongVector;
import com.google.flatbuffers.ShortVector;
import com.google.flatbuffers.StringVector;
import com.google.flatbuffers.Struct;
import com.google.flatbuffers.Table;
import com.google.flatbuffers.UnionVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class DocumentUpdatedEvent extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }
  public static DocumentUpdatedEvent getRootAsDocumentUpdatedEvent(ByteBuffer _bb) { return getRootAsDocumentUpdatedEvent(_bb, new DocumentUpdatedEvent()); }
  public static DocumentUpdatedEvent getRootAsDocumentUpdatedEvent(ByteBuffer _bb, DocumentUpdatedEvent obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public DocumentUpdatedEvent __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String charId() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer charIdAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer charIdInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public String parentCharId() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer parentCharIdAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public ByteBuffer parentCharIdInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 1); }
  public boolean isRight() { int o = __offset(8); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean hasDisambiguator() { return 0 != __offset(10); }
  public int disambiguator() { int o = __offset(10); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public int character() { int o = __offset(12); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public int documentId() { int o = __offset(14); return o != 0 ? bb.getInt(o + bb_pos) : 0; }

  public static int createDocumentUpdatedEvent(FlatBufferBuilder builder,
      int charIdOffset,
      int parentCharIdOffset,
      boolean isRight,
      int disambiguator,
      int character,
      int documentId) {
    builder.startTable(6);
    DocumentUpdatedEvent.addDocumentId(builder, documentId);
    DocumentUpdatedEvent.addCharacter(builder, character);
    DocumentUpdatedEvent.addDisambiguator(builder, disambiguator);
    DocumentUpdatedEvent.addParentCharId(builder, parentCharIdOffset);
    DocumentUpdatedEvent.addCharId(builder, charIdOffset);
    DocumentUpdatedEvent.addIsRight(builder, isRight);
    return DocumentUpdatedEvent.endDocumentUpdatedEvent(builder);
  }

  public static void startDocumentUpdatedEvent(FlatBufferBuilder builder) { builder.startTable(6); }
  public static void addCharId(FlatBufferBuilder builder, int charIdOffset) { builder.addOffset(0, charIdOffset, 0); }
  public static void addParentCharId(FlatBufferBuilder builder, int parentCharIdOffset) { builder.addOffset(1, parentCharIdOffset, 0); }
  public static void addIsRight(FlatBufferBuilder builder, boolean isRight) { builder.addBoolean(2, isRight, false); }
  public static void addDisambiguator(FlatBufferBuilder builder, int disambiguator) { builder.addInt(3, disambiguator, 0); }
  public static void addCharacter(FlatBufferBuilder builder, int character) { builder.addInt(4, character, 0); }
  public static void addDocumentId(FlatBufferBuilder builder, int documentId) { builder.addInt(5, documentId, 0); }
  public static int endDocumentUpdatedEvent(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }
  public static void finishDocumentUpdatedEventBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  public static void finishSizePrefixedDocumentUpdatedEventBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public DocumentUpdatedEvent get(int j) { return get(new DocumentUpdatedEvent(), j); }
    public DocumentUpdatedEvent get(DocumentUpdatedEvent obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}
