package org.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.OPTIONAL;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import document.editing.Change;
import document.editing.ChangeRequest;
import document.editing.Request;
import document.editing.RequestHolder;

@Testcontainers(disabledWithoutDocker = true)
public class IntegrationTest {
	private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:latest");
	private static final int MONGO_PORT = 27017;
	private static final DockerImageName DOCUMENT_SERVER_IMAGE = DockerImageName.parse(getDocumentServerImageName());

	@NotNull
	private static String getDocumentServerImageName() {
		return Optional.ofNullable(System.getProperty("documentServerImage"))
				.filter(v -> !v.isBlank())
				.orElse("document-editor/document-server");
	}

	private static final String MONGO_DB = "test";
	private static final String DOCUMENTS_COLLECTION = "documents";
	private static final int SERVER_PORT = 3104;
	private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
	private static final int TREE_HEIGHT = 8;
	private static final int DOC_ID = 12;
	private static final String IDENTITY = "test";
	private static final int NUM_PARALLEL_CONNECTIONS = 100;

	Network network = Network.newNetwork();

	@Container
	private final MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_IMAGE)
			.withNetwork(network)
			.withNetworkAliases("mongo")
			.withExposedPorts(MONGO_PORT);

	GenericContainer<?> documentServerContainer;
	SecureRandom random = new SecureRandom();

	@BeforeEach
	void init() {
		documentServerContainer = new GenericContainer<>(DOCUMENT_SERVER_IMAGE)
				.withNetwork(network)
				.withNetworkAliases("document-server")
				.dependsOn(mongoDBContainer)
				.withEnv(Map.of(
						"MONGO_URL", "mongodb://mongo:" + MONGO_PORT,
						"MONGO_DB", MONGO_DB,
						"MONGO_COLLECTION", DOCUMENTS_COLLECTION,
						"SERVER_URL", "tcp://*:" + SERVER_PORT
				))
				.withLogConsumer(new Slf4jLogConsumer(LOGGER))
				.withExposedPorts(SERVER_PORT);
		documentServerContainer.start();
	}

	@AfterEach
	void afterTest() {
		documentServerContainer.stop();
	}

	@Test
	void singleClientMakesHugeChangesPerformanceTest() {
		var mongoClient = MongoClients.create(MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(mongoDBContainer.getConnectionString()))
				.build());
		var database = mongoClient.getDatabase(MONGO_DB);
		var documentsCollection = database.getCollection(DOCUMENTS_COLLECTION);

		LOGGER.info("Established connection to Mongo");

		// Remove all document elements
		documentsCollection.deleteMany(new Document());

		LOGGER.info("Deleted all");

		var changes = createChanges(TREE_HEIGHT, DOC_ID);
		var changeRequest = createChangeRequest(changes);

		LOGGER.info("Created change request with {} changes", changes.changes().size());

		long duration = measure(() -> {
			try (var context = new ZContext()) {
				var socket = context.createSocket(SocketType.REQ);
				socket.setIdentity(IDENTITY.getBytes(StandardCharsets.UTF_8));
				socket.connect("tcp://localhost:" + documentServerContainer.getMappedPort(SERVER_PORT));
				socket.send(changeRequest.sizedByteArray());
				LOGGER.info("Sent");
				socket.recv();
				LOGGER.info("Received reply...");
			}
		});
		LOGGER.info("Duration = {}", duration);
		assertThat(duration).isLessThanOrEqualTo(2000L);
		assertThat(documentsCollection.countDocuments()).isEqualTo(changes.changes().size());
	}

	@Test
	void multipleClientMakeSmallChangesPerformanceTest() throws InterruptedException {
		var mongoClient = MongoClients.create(MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(mongoDBContainer.getConnectionString()))
				.build());
		var database = mongoClient.getDatabase(MONGO_DB);
		var documentsCollection = database.getCollection(DOCUMENTS_COLLECTION);

		LOGGER.info("Established connection to Mongo");

		// Remove all document elements
		documentsCollection.deleteMany(new Document());

		LOGGER.info("Deleted all");

		var allChanges = createChanges(TREE_HEIGHT, DOC_ID);

		var smallChangesList =
				allChanges.partitionFor(NUM_PARALLEL_CONNECTIONS).stream().map(this::createChangeRequest).toList();

		LOGGER.info("Created change request with {} changes", allChanges.changes().size());

		var countDownLatch = new CountDownLatch(smallChangesList.size());
		LOGGER.info("Threads = {}", smallChangesList.size());
		var context = new ZContext();
		long duration = measure(() -> {
			for (int i = 0; i < smallChangesList.size(); i++) {
				var socket = context.createSocket(SocketType.REQ);
				final int threadId = i;
				Thread.startVirtualThread(() -> {
					socket.setIdentity(("client" + threadId).getBytes(StandardCharsets.UTF_8));
					socket.connect("tcp://localhost:" + documentServerContainer.getMappedPort(SERVER_PORT));
					socket.send(smallChangesList.get(threadId).sizedByteArray());
					LOGGER.info("Sent");
					socket.recv();
					LOGGER.info("Received reply...");
					countDownLatch.countDown();
				});
			}
		});
		countDownLatch.await();
		LOGGER.info("Duration = {}", duration);
		assertThat(duration).isLessThanOrEqualTo(2000L);
		assertThat(documentsCollection.countDocuments()).isEqualTo(allChanges.changes().size());
		context.close();
	}


	private ChangesDTO createChanges(int treeHeight, int docId) {
		List<ChangeDTO> allChanges = new ArrayList<>();
		ChangeDTO rootChange =
				new ChangeDTO(getRandomCharId(), null, getRandomCharacter(), false, getRandomDisambiguator());
		List<ChangeDTO> prevLevelChanges = List.of(rootChange);
		for (int i = 0; i < treeHeight; i++) {
			allChanges.addAll(prevLevelChanges);
			List<ChangeDTO> newChanges = new ArrayList<>();
			for (ChangeDTO prevLevelChange : prevLevelChanges) {
				newChanges.add(
						new ChangeDTO(getRandomCharId(), prevLevelChange.charId(), getRandomCharacter(), false, getRandomDisambiguator()));
				newChanges.add(
						new ChangeDTO(getRandomCharId(), prevLevelChange.charId(), getRandomCharacter(), true, getRandomDisambiguator()));
			}
			prevLevelChanges = newChanges;
		}
		allChanges.addAll(prevLevelChanges);
		return new ChangesDTO(allChanges, docId);
	}

	private int getRandomDisambiguator() {
		return random.nextInt(1000) + 1;
	}

	private int getRandomCharacter() {
		return random.nextInt(100) + 1;
	}

	private String getRandomCharId() {
		return UUID.randomUUID().toString();
	}

	private long measure(Runnable action) {
		long start = System.currentTimeMillis();
		action.run();
		return System.currentTimeMillis() - start;
	}

	private FlatBufferBuilder createChangeRequest(ChangesDTO changesDTO) {
		var documentId = changesDTO.docId();

		var flatBufferBuilder = new FlatBufferBuilder(1024);

		var changesOffsets = changesDTO.changes().stream().mapToInt(changeDTO -> {
			var charIdOffset = flatBufferBuilder.createString(changeDTO.charId());
			var parentCharIdOffset = changeDTO.parentCharId() == null ? 0 : flatBufferBuilder.createString(changeDTO.parentCharId());
			return Change.createChange(
					flatBufferBuilder,
					charIdOffset,
					parentCharIdOffset,
					changeDTO.isRight(),
					changeDTO.disambiguator(),
					changeDTO.character()
			);
		}).toArray();

		int changeRequestOffset = ChangeRequest.createChangeRequest(
				flatBufferBuilder,
				documentId,
				ChangeRequest.createChangesVector(flatBufferBuilder, changesOffsets));

		flatBufferBuilder.finish(RequestHolder.createRequestHolder(flatBufferBuilder, Request.ChangeRequest, changeRequestOffset));
		return flatBufferBuilder;
	}

	private record ChangesDTO(List<ChangeDTO> changes, int docId) {

		public List<ChangesDTO> partitionFor(int n) {
			List<ChangesDTO> changesDTOS = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				changesDTOS.add(new ChangesDTO(new ArrayList<>(), docId));
			}
			for (int i = 0; i < changes.size(); i++) {
				var change = changes.get(i);
				changesDTOS.get(i % n).changes.add(change);
			}
			return changesDTOS;
		}

	}

	private record ChangeDTO(
			String charId,
			String parentCharId,
			int character,
			boolean isRight,
			int disambiguator
	) {
	}

}
