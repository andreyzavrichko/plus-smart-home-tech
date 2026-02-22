package ru.yandex.practicum.serialization.config;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BaseAvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    private final EncoderFactory encoderFactory;
    private final DatumWriter<T> datumWriter;

    public BaseAvroSerializer(Schema schema) {
        this(EncoderFactory.get(), schema);
    }

    public BaseAvroSerializer(EncoderFactory encoderFactory, Schema schema) {
        this.encoderFactory = encoderFactory;
        this.datumWriter = new SpecificDatumWriter<>(schema);
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = encoderFactory.binaryEncoder(out, null);
            datumWriter.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize Avro message to topic: " + topic, e);
        }
    }
}