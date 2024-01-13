package org.example.serialization;

import java.util.Optional;

public interface Serializer<T> {
	Optional<byte[]> serialize(T obj);
}
