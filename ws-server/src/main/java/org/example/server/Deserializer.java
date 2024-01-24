package org.example.server;

public interface Deserializer<T> {
	T deserialize(byte[] bytes);
}
