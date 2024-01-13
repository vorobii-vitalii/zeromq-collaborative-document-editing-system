package org.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.bson.Document;
import org.example.constants.DocumentElementSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

@Testcontainers(disabledWithoutDocker = true)
public class IntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

	private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:latest");
	private static final int MONGO_PORT = 27017;
	private static final String MONGO_DB = "test";
	private static final String DOCUMENTS_COLLECTION = "documents";

	private static final DockerImageName DOCUMENT_CHANGE_WATCHER_IMAGE =
			DockerImageName.parse("document-editor/document-change-watcher");
	private static final int SERVER_PORT = 3104;
	public static final int WAIT_MS = 10_000;

	Network network = Network.newNetwork();

	@Container
	private final MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_IMAGE)
			.withNetwork(network)
			.withNetworkAliases("mongo")
			.withExposedPorts(MONGO_PORT)
			.withLogConsumer(new Slf4jLogConsumer(LOGGER));

	GenericContainer<?> documentChangeWatcherContainer;

	@BeforeEach
	void init() {
		documentChangeWatcherContainer = new GenericContainer<>(DOCUMENT_CHANGE_WATCHER_IMAGE)
				.withNetwork(network)
				.withNetworkAliases("document-server")
				.dependsOn(mongoDBContainer)
				.withEnv(Map.of(
						"MONGO_URL", "mongodb://mongo:" + MONGO_PORT,
						"MONGO_DB", MONGO_DB,
						"MONGO_COLLECTION", DOCUMENTS_COLLECTION,
						"PORT", String.valueOf(SERVER_PORT)
				))
				.withLogConsumer(new Slf4jLogConsumer(LOGGER))
				.withExposedPorts(SERVER_PORT)
				.waitingFor(Wait.forLogMessage(".*Starting publishing server.*", 1));
		documentChangeWatcherContainer.start();
	}

	@AfterEach
	void afterEach() {
		documentChangeWatcherContainer.stop();
	}

	@Test
	void insertElementIntoCollectionCDCTest() {
		var mongoClient = MongoClients.create(MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(mongoDBContainer.getConnectionString()))
				.build());
		var database = mongoClient.getDatabase(MONGO_DB);
		var documentsCollection = database.getCollection(DOCUMENTS_COLLECTION);
		LOGGER.info("Established connection to Mongo");

		try (var context = new ZContext()) {
			var subscribeSocket = context.createSocket(SocketType.SUB);
			subscribeSocket.connect("tcp://localhost:" + documentChangeWatcherContainer.getMappedPort(SERVER_PORT));
			subscribeSocket.subscribe(ZMQ.SUBSCRIPTION_ALL);
			documentsCollection.insertOne(
					new Document()
							.append(DocumentElementSchema.DOCUMENT_ID, 1)
							.append(DocumentElementSchema.CHAR_ID, "2")
							.append(DocumentElementSchema.PARENT_CHAR_ID, "1")
							.append(DocumentElementSchema.IS_RIGHT, false)
							.append(DocumentElementSchema.DISAMBIGUATOR, 1)
							.append(DocumentElementSchema.VALUE, 15)
			);
			subscribeSocket.setReceiveTimeOut(WAIT_MS);
			verifyMsgReturned(subscribeSocket);
			// TODO: Verify content of message
		}
	}

	private static void verifyMsgReturned(ZMQ.Socket subscribeSocket) {
		var receivedEventBytes = subscribeSocket.recv();
		assertThat(receivedEventBytes).isNotNull();
	}

}
