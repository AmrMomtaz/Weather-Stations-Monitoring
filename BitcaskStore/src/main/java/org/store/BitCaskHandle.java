/**
 * Created by Amr Momtaz.
 */

package org.store;

import java.io.*;
import java.util.*;

/**
 * Handler for a bitcask directory, performs all R/W operations.
 */
public class BitCaskHandle {

    private final Map<String, KeyDirRecord> keyDir;
    private final String rootDir; // represents the store's root directory
    private int epoch; // Determines the number of merge which is done and the epoch of execution
    private int currentFileID; // Determines the file ID within a specific epoch
    private DataOutputStream dataOutputStream;

    public BitCaskHandle(String rootDir) {
        this.keyDir = new HashMap<>();
        this.rootDir = rootDir;
        this.epoch = 1;
        this.currentFileID = 1;
    }

    /**
     * Returns a value from the store given a certain key.
     */
    public String getValue(String key) throws IOException {
        if (! keyDir.containsKey(key)) return null;
        else {
            String value;
            KeyDirRecord keyDirRecord = keyDir.get(key);
            try (InputStream is = new FileInputStream(keyDirRecord.fileId())) {
                long actualSkipped = is.skip(keyDirRecord.valuePosition());
                if (actualSkipped != keyDirRecord.valuePosition()) throw new RuntimeException();
                value = new String(is.readNBytes(keyDirRecord.valueSize()));
            }
            return value;
        }
    }

    /**
     * Adds a new entry in the Bitcask store.
     */
    public void addEntry(String key, String value) throws IOException {
        if (this.dataOutputStream == null) updateDataOutputStream();
        else if (this.dataOutputStream.size() >= BitcaskStore.MAX_FILE_SIZE) {
            this.dataOutputStream.close();
            this.currentFileID += 1;
            updateDataOutputStream();
        }
        // Creating entry in the key-dir
        String fileID = getCurrentFileId();
        int keySize = key.getBytes().length;
        int valueSize = value.getBytes().length;
        int valuePosition = this.dataOutputStream.size() + keySize + 12;
        int timestamp = (int)(System.currentTimeMillis()/1000);
        KeyDirRecord keyDirRecord
            = new KeyDirRecord(fileID, valueSize, valuePosition, timestamp);
        this.keyDir.put(key, keyDirRecord);

        // Writing the record to the active data file
        DataRecord dataRecord
            = new DataRecord(timestamp, keySize, valueSize, key, value);
        writeDataRecord(dataRecord);
    }

    /**
     * Returns a list which contains all the keys in the store.
     */
    public List<String> listKeys() {
        return keyDir.keySet().stream().toList();
    }

    /**
     * Closes the current DataOutputStream.
     */
    public void close() throws IOException {
        this.dataOutputStream.close();
    }


    //
    // Private Methods
    //

    /**
     * Updates the data output stream instance with the current file name.
     */
    private void updateDataOutputStream() throws FileNotFoundException {
        this.dataOutputStream = new DataOutputStream
                (new BufferedOutputStream(new FileOutputStream(getCurrentFileId())));
    }
    private String getCurrentFileId() {
        return this.rootDir + "/epoch_" + this.epoch + "_" + currentFileID;
    }

    /**
     * Returns a new data record from a given inputStream.
     */
    private DataRecord readNextDataRecord(DataInputStream inputStream) throws IOException {
        int timestamp = inputStream.readInt();
        int keySize = inputStream.readInt();
        int valueSize = inputStream.readInt();
        String key = new String(inputStream.readNBytes(keySize));
        String value = new String(inputStream.readNBytes(valueSize));
        return new DataRecord(timestamp, keySize, valueSize, key, value);
    }

    /**
     * Writes a new record in the last data file.
     */
    private void writeDataRecord(DataRecord record) throws IOException {
        this.dataOutputStream.writeInt(record.timestamp());
        this.dataOutputStream.writeInt(record.keySize());
        this.dataOutputStream.writeInt(record.valueSize());
        this.dataOutputStream.writeBytes(record.key());
        this.dataOutputStream.writeBytes(record.value());
    }

    //
    // Nested Types
    //

    /**
     * Represents a record in a data file.
     */
    private record DataRecord
        (int timestamp, int keySize, int valueSize, String key, String value) {}

    /**
     * Represents a record in a hint file.
     */
    private record HintRecord() {}

    /**
     * Represents a record for the in-memory key directory
     */
    private record KeyDirRecord(String fileId, int valueSize, int valuePosition, int timestamp){}
}
