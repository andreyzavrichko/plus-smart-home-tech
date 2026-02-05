package ru.yandex.practicum.collector.avro;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class AvroBinarySerializer {

    private AvroBinarySerializer() {}

    public static byte[] toBytes(SpecificRecord record) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            SpecificDatumWriter<SpecificRecord> writer = new SpecificDatumWriter<>(record.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Avro serialization failed", e);
        }
    }
}
