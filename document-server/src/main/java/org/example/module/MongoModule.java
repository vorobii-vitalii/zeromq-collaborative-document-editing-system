package org.example.module;

import javax.inject.Named;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import dagger.Module;
import dagger.Provides;

@Module
public class MongoModule {

	@Provides
	MongoClientSettings mongoClientSettings() {
		return MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(getMongoURL()))
				.build();
	}

	@Provides
	MongoClient mongoClient(MongoClientSettings mongoClientSettings) {
		return MongoClients.create(mongoClientSettings);
	}

	@Provides
	MongoDatabase mongoDatabase(MongoClient mongoClient) {
		return mongoClient.getDatabase(getMongoDB());
	}

	@Provides
	@Named("documentElementsCollection")
	MongoCollection<Document> documentElementsCollection(MongoDatabase mongoDatabase) {
		return mongoDatabase.getCollection(getCollection());
	}

	private static String getMongoURL() {
		return System.getenv("MONGO_URL");
	}

	private static String getMongoDB() {
		return System.getenv("MONGO_DB");
	}

	private static String getCollection() {
		return System.getenv("MONGO_COLLECTION");
	}

}
