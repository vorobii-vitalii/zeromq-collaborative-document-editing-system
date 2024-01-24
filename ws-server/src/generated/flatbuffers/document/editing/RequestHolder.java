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
public final class RequestHolder extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }
  public static RequestHolder getRootAsRequestHolder(ByteBuffer _bb) { return getRootAsRequestHolder(_bb, new RequestHolder()); }
  public static RequestHolder getRootAsRequestHolder(ByteBuffer _bb, RequestHolder obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public RequestHolder __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public byte requestType() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public Table request(Table obj) { int o = __offset(6); return o != 0 ? __union(obj, o + bb_pos) : null; }

  public static int createRequestHolder(FlatBufferBuilder builder,
      byte requestType,
      int requestOffset) {
    builder.startTable(2);
    RequestHolder.addRequest(builder, requestOffset);
    RequestHolder.addRequestType(builder, requestType);
    return RequestHolder.endRequestHolder(builder);
  }

  public static void startRequestHolder(FlatBufferBuilder builder) { builder.startTable(2); }
  public static void addRequestType(FlatBufferBuilder builder, byte requestType) { builder.addByte(0, requestType, 0); }
  public static void addRequest(FlatBufferBuilder builder, int requestOffset) { builder.addOffset(1, requestOffset, 0); }
  public static int endRequestHolder(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public RequestHolder get(int j) { return get(new RequestHolder(), j); }
    public RequestHolder get(RequestHolder obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

