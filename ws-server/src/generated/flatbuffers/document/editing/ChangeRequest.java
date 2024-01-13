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
public final class ChangeRequest extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }
  public static ChangeRequest getRootAsChangeRequest(ByteBuffer _bb) { return getRootAsChangeRequest(_bb, new ChangeRequest()); }
  public static ChangeRequest getRootAsChangeRequest(ByteBuffer _bb, ChangeRequest obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public ChangeRequest __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public int documentId() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public document.editing.Change changes(int j) { return changes(new document.editing.Change(), j); }
  public document.editing.Change changes(document.editing.Change obj, int j) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int changesLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public document.editing.Change.Vector changesVector() { return changesVector(new document.editing.Change.Vector()); }
  public document.editing.Change.Vector changesVector(document.editing.Change.Vector obj) { int o = __offset(6); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }

  public static int createChangeRequest(FlatBufferBuilder builder,
      int documentId,
      int changesOffset) {
    builder.startTable(2);
    ChangeRequest.addChanges(builder, changesOffset);
    ChangeRequest.addDocumentId(builder, documentId);
    return ChangeRequest.endChangeRequest(builder);
  }

  public static void startChangeRequest(FlatBufferBuilder builder) { builder.startTable(2); }
  public static void addDocumentId(FlatBufferBuilder builder, int documentId) { builder.addInt(0, documentId, 0); }
  public static void addChanges(FlatBufferBuilder builder, int changesOffset) { builder.addOffset(1, changesOffset, 0); }
  public static int createChangesVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startChangesVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endChangeRequest(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public ChangeRequest get(int j) { return get(new ChangeRequest(), j); }
    public ChangeRequest get(ChangeRequest obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

