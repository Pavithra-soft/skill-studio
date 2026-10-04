package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class KafkaCatalog {

    private KafkaCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("kafka", "Apache Kafka",
                "Kafka is a log. Partitions give you scale. Keys give you order. Consumer groups give you work sharing. Poison pills are how you sleep.",
                Concepts.of("Topics, keys, and order",
                                "Order is per partition, not per topic. The same key lands in the same partition. That is the contract you design around.",
                                "A user's booking events used a null key, round-robined, and applied a cancellation before the booking. Keying by userId fixed the story.")
                        .depth("""
                                A topic is a named, append-only log split into partitions. The producer hash of the key picks the partition (by default). Order is guaranteed only inside that partition. If you need user-level order, the user id is the key. Null keys round-robin and you will not replay a user's story in order.

                                Changing the partition count later reshuffles keys — old data stays, new data hashes differently. Plan for it or use a custom partitioner you will regret. Compaction and retention are per-topic policies, not magic delete.

                                In interviews, correct 'Kafka is globally ordered' in one sentence, then say what you keyed.
                                """)
                        .qa("Is Kafka globally ordered?",
                                "No. Per partition only. If an interviewer says exactly-in-order across the topic, they want this correction.",
                                "What happens to keys when you add partitions?")
                        .qa("How do you keep a user's events in order?",
                                "Use userId as the key so they share a partition. Do not rely on timestamps. Do not send the same user to two partitions with a clever partitioner.",
                                "When would you skip a key on purpose?")
                        .sample("Key the entity",
                                """
                                        producer.send(new ProducerRecord<>(topic, userId, event));
                                        """,
                                "Same user, same partition. Cancel cannot beat booking."),
                Concepts.of("Consumer groups",
                                "One group, many members, partitions split between them. A second group is a second independent read of the same log.",
                                "Search indexing and fraud detection both read payments. Two groups, two commits, no stolen partitions.")
                        .depth("""
                                A group is a competing-consumer team. Kafka assigns partitions so each partition has one owner in that group. Scale by adding members up to the partition count; extra members idle. A second group is a second product with its own offsets.

                                Rebalances pause work. Sticky and cooperative assignors reduce the pause. group.id is a product name, not a hostname. Using the machine name as group.id is how you get N independent consumers and a lag explosion.

                                In interviews, one group versus two, and where offsets live.
                                """)
                        .qa("Two services need the same events. One group or two?",
                                "Two groups if they are different products. One group if they are identical workers sharing load.",
                                "What is a stop-the-world rebalance?")
                        .qa("Where are offsets stored?",
                                "Usually __consumer_offsets, committed by the client. Manual commit after your side effect succeeds. Auto-commit can skip or duplicate on crash.",
                                "When do you use transactional consume-process-produce?")
                        .sample("Independent readers",
                                """
                                        spring.kafka.consumer.group-id=job-monitor-search
                                        # another app:
                                        spring.kafka.consumer.group-id=job-monitor-mail
                                        """,
                                "Two groups, two commits."),
                Concepts.of("Idempotent producer",
                                "Retries without idempotence duplicate messages. enable.idempotence is the default you should not turn off. It is not exactly-once to the consumer.",
                                "A timeout on a cover-letter event retried and created two tracker rows until the producer stayed idempotent and the consumer keyed on event id.")
                        .depth("""
                                Without idempotence, a retry after a timeout can append twice. The idempotent producer uses a producer id, epoch, and sequence per partition so the broker drops duplicates from that session. acks=all is part of the story. It is not end-to-end exactly-once — that needs transactions plus an idempotent consumer.

                                Do not disable idempotence to silence a warning. After a producer restart, sequences reset; consumers can still see a logical duplicate if you republish. Persist a business id.

                                In interviews, split broker-side idempotence from consumer-side idempotence.
                                """)
                        .qa("Does idempotence give exactly-once to the consumer?",
                                "No. It stops duplicate appends from one producer session. Consumers can still reprocess. Exactly-once end to end is transactions plus idempotent consume.",
                                "What is a producer epoch?")
                        .qa("Why is acks=all part of the story?",
                                "The sequence is only meaningful if the leader and ISRs accepted the batch. acks=0 with idempotence is a contradiction.",
                                "What happens after a producer restart?")
                        .sample("Keep the default",
                                """
                                        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
                                        props.put(ProducerConfig.ACKS_CONFIG, "all");
                                        """,
                                "Retries will not double-append from this session."),
                Concepts.of("Offsets and delivery",
                                "At-least-once is the default you will actually run. Commit after the side effect. Commit before it and you can lose. Duplicate handling is mandatory.",
                                "A mail sender auto-committed then crashed mid-SMTP. The offset moved, the mail never left. Manual commit after success plus an idempotency key on the mail id fixed both loss and double-send.")
                        .depth("""
                                The consumer poll returns records. Your handler does work. Then you commit. Auto-commit on an interval is easy and wrong for side effects: crash windows lose or duplicate. enable.auto.commit=false and commit after success is the staff default.

                                At-least-once means you will see a record twice. Idempotent handlers (unique constraints, upserts, idempotency keys) are not optional. At-most-once means you commit first and accept loss. Exactly-once in Kafka is a transactional read-process-write to another topic, not 'my HTTP call ran once'.

                                In interviews, pick at-least-once and explain the commit point.
                                """)
                        .qa("When do you commit?",
                                "After the side effect succeeded (or was recorded in an outbox). Not on poll, not on a timer that ignores failure.",
                                "What is the crash window with auto-commit?")
                        .qa("Is Kafka exactly-once for sending email?",
                                "No. Email is a side effect outside the log. You can get at-least-once plus an idempotency key on the provider. Do not claim EOS for SMTP.",
                                "What does a transactional produce actually cover?")
                        .sample("Ack after work",
                                """
                                        @KafkaListener(topics = "apply.submitted")
                                        public void on(ApplyEvent event, Acknowledgment ack) {
                                            mail.sendOnce(event);
                                            ack.acknowledge();
                                        }
                                        """,
                                "sendOnce is keyed. Then the offset moves."),
                Concepts.of("Poison pills and retry",
                                "A record that always throws will block its partition forever if you retry in place. Retry topics, then a dead-letter, then skip.",
                                "One malformed JSON froze mail classification until the listener published to a DLT and acknowledged.")
                        .depth("""
                                Deserialization errors and poison business data are different. Both can stall a partition if you infinite-retry. Spring Kafka's DefaultErrorHandler can seek, retry, and recover to a DLT. Retry topics with backoff keep the main group moving. Sleeping in the listener is how you create lag storms.

                                Log offset, partition, key, and correlation id. Alert on DLT depth. Replay is a product: fix the schema, then republish. Seek past only after the DLT write succeeds.

                                In interviews, this story beats 'we use Kafka'.
                                """)
                        .qa("A JSON blob cannot deserialize. What do you do?",
                                "Do not infinite-retry the partition. Send to DLT with the reason, skip, alert. Fix the producer. Replay from DLT when the schema is fixed.",
                                "Who owns the DLT consumer?")
                        .qa("Retry in the listener or a retry topic?",
                                "A retry topic keeps the main group moving and lets you backoff without holding a partition. In-listener sleeps create lag storms.",
                                "How do you cap retry count?")
                        .sample("Skip after DLT",
                                """
                                        deadLetter.publish(record, error);
                                        ack.acknowledge();
                                        """,
                                "The rest of the group keeps working."),
                Concepts.of("Compaction and changelog",
                                "Compacted topics keep the latest value per key. That is how Kafka Streams stores state without a second database for every key. It is not a delete-everything button.",
                                "A customer-profile topic compacted by customerId so consumers only cared about the latest address, not every historical change.")
                        .depth("""
                                cleanup.policy=compact drops older records with the same key after the compaction cycle. A null payload is a tombstone, which eventually removes the key. Compaction is not immediate. If you need a queue of events, compacting will destroy history — use delete plus retention instead.

                                Changelog topics for Kafka Streams are compacted. Treating a compacted topic as an audit log is a design error. Treat it as a remote HashMap with lag.

                                In interviews, tombstone, key requirement, and why null keys cannot compact usefully.
                                """)
                        .qa("Can you compact a topic of unordered events with null keys?",
                                "No usefully. Compaction is per key. Null keys do not have an identity to keep.",
                                "What is a tombstone?")
                        .qa("Audit log or compacted topic?",
                                "Audit needs history and retention. Compacted is latest-per-key. Do not mix the product requirements on one topic.",
                                "When is compact,delete the policy?")
                        .sample("Compacted profile",
                                """
                                        cleanup.policy=compact
                                        min.cleanable.dirty.ratio=0.5
                                        """,
                                "Key is customerId. Screen only."),
                Concepts.of("Transactions and EOS",
                                "A Kafka transaction lets you write several partitions atomically and fence zombies. It is not a distributed transaction with Postgres unless you add an outbox.",
                                "A consume-process-produce pipeline used transactional send plus read_committed so a crash did not leave a half-written downstream topic. Postgres still used an outbox.")
                        .depth("""
                                transactional.id identifies the producer. After a crash, a new epoch fences the old one. Consumers with isolation.level=read_committed skip uncommitted records. This is how you get atomic writes to several topics.

                                It does not enlist JDBC. Dual write to Postgres and Kafka is still a dual write. Outbox remains the correct pattern when the database is the source of truth.

                                In interviews, fence, read_committed, and 'not your DB transaction'.
                                """)
                        .qa("Does a Kafka transaction commit my JPA work?",
                                "No. Different systems. Use an outbox in the DB transaction, then a publisher.",
                                "What is fencing?")
                        .qa("Who should enable read_committed?",
                                "Consumers that must not see aborted transactional records. Everyone else may see aborted data as if it were normal — usually not what you want for EOS pipelines.",
                                "When is EOS overkill?")
                        .sample("Transactional produce",
                                """
                                        template.executeInTransaction(ops -> {
                                            ops.send("apply.events", key, event);
                                            ops.send("apply.metrics", key, metric);
                                            return true;
                                        });
                                        """,
                                "Both records land or neither does — in Kafka, not in Postgres."),
                Concepts.of("Schema contracts",
                                "Producers and consumers agree on a schema. A field rename without a plan is a poison pill. The schema registry is a product, not a JAR you forgot to add.",
                                "Mail deployed on Tuesday because Search still understood Monday's JobPosted event — optional fields, no reuse of field ids.")
                        .depth("""
                                JSON without a schema will fail open until it fails closed at 2am. Avro/Protobuf with a registry give you compatibility checks: backward means a new consumer reads old data, forward the opposite. Pick one and enforce it in CI.

                                Never reuse a field id for a new meaning. Never make a new field required without a default if old producers still exist. Poison pills from schema breaks belong in the DLT story.

                                In interviews, compatibility mode and a field-add example.
                                """)
                        .qa("You add an optional field. Who breaks?",
                                "Nobody if consumers ignore unknowns and producers you have not upgraded omit it. A new required field without a default breaks old producers.",
                                "What does backward compatibility mean?")
                        .qa("JSON versus Avro for events?",
                                "JSON is debuggable and sloppy. Avro plus a registry is how you stop silent field reuse. Either can work; neither works without a compatibility rule.",
                                "Where do you reject a breaking schema?")
                        .sample("Additive event",
                                """
                                        public record JobPosted(String id, String title, String company, String source) {}
                                        """,
                                "source is new and optional in JSON. Old consumers ignore it.")
        );
    }
}
