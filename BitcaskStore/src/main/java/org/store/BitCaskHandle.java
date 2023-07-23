/**
 * Created by Amr Momtaz.
 */

package org.store;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Handler for a bitcask directory, performs all R/W operations.
 */
public class BitCaskHandle {

    private final Map<String, KeyDirRecord> keyDir;
    private final String rootDir; // represents the store's root directory
    private final boolean isReadOnly; // Determines if this handle is only a reader
    private final boolean isSyncOn; // Determines if this process syncs after each write
    private int epoch; // Determines the number of merge which is done and the epoch of execution
    private int currentFileID; // Determines the file ID within a specific epoch (starts with 1)
    private DataOutputStream dataOutputStream;

    /**
     * Constructor which initializes the handler and initializes keyDir map with current data
     * available in the root directory data given if any.
     */
    BitCaskHandle(String rootDir, boolean isReadOnly, boolean isSyncOn) throws IOException {
        // Initializing main attributes
        this.keyDir = new HashMap<>();
        this.rootDir = rootDir;
        this.isReadOnly = isReadOnly;
        this.isSyncOn = isSyncOn;

        // Initializing the handler state
        File folder = new File(rootDir);
        if (folder.exists() && folder.isDirectory()) {
            final Comparator<String> stringComparator = (s1, s2) -> {
                if (s1.length() < s2.length()) return -1;
                else if (s1.length() > s2.length()) return 1;
                else return s1.compareTo(s2);
            };
            List<String> hintFiles =
                    new ArrayList<>(Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                            .map(File::getName)
                            .filter(fileName -> fileName.startsWith("hint"))
                            .toList());
            List<String> dataFiles =
                    new ArrayList<>(Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                            .map(File::getName)
                            .filter(fileName -> fileName.startsWith("epoch"))
                            .toList());
            if (hintFiles.size() != 0) { // Use hint files for fast recovery
                hintFiles.sort(stringComparator);
                String[] lastFileSegment = hintFiles.get(hintFiles.size()-1).split("_");
                this.epoch = Integer.parseInt(lastFileSegment[2]);
                int lastFileId = Integer.parseInt(lastFileSegment[3]);
                for (int i = 1 ; i <= lastFileId ; i++) {
                    this.currentFileID = i;
                    try (DataInputStream dataInputStream = new DataInputStream
                            (new BufferedInputStream(new FileInputStream("hint_" + getCurrentFileId())))) {
                        while(dataInputStream.available() >= 0) {
                            HintRecord hintRecord = readNextHintRecord(dataInputStream);
                            String fileId = getCurrentFileId();
                            int valueSize = hintRecord.valueSize();
                            int valuePosition = hintRecord.valuePosition();
                            int timestamp = hintRecord.timestamp();
                            String key = hintRecord.key();
                            this.keyDir.put(key, new KeyDirRecord(fileId, valueSize, valuePosition, timestamp));
                        }
                    }
                }
            }
            else if (dataFiles.size() != 0) { // No hint files are available so
                dataFiles.sort(stringComparator);
                String[] lastFileSegment = dataFiles.get(dataFiles.size()-1).split("_");
                this.epoch = Integer.parseInt(lastFileSegment[1]);
                int lastFileId = Integer.parseInt(lastFileSegment[2]);
                for (int i = 1 ; i <= lastFileId ; i++) {
                    this.currentFileID = i;
                    try (DataInputStream dataInputStream = new DataInputStream
                            (new BufferedInputStream(new FileInputStream(getCurrentFileId())))) {
                        int currentPosition = 0;
                        while (dataInputStream.available() >= 12) {
                            DataRecord dataRecord = readNextDataRecord(dataInputStream);
                            String fileId = getCurrentFileId();
                            int valueSize = dataRecord.valueSize();
                            int valuePosition = currentPosition + 12 + dataRecord.keySize();
                            int timestamp = dataRecord.timestamp();
                            currentPosition += 12 + dataRecord.keySize() + dataRecord.valueSize();
                            if (dataRecord.value().equals(BitcaskStore.DELETED_VALUE))
                                this.keyDir.remove(dataRecord.key());
                            else this.keyDir.put(dataRecord.key(),
                                    new KeyDirRecord(fileId, valueSize, valuePosition, timestamp));
                        }
                    }
                }
            }
            else { // The root directory is new
                this.epoch = 1;
                this.currentFileID = 1;
            }
        }
        else throw new RuntimeException("rootDir is invalid");
    }

    /**
     * Returns a value from the store given a certain key.
     */
    String getValue(String key) throws IOException {
        if (! keyDir.containsKey(key)) return null;
        else {
            String value;
            KeyDirRecord keyDirRecord = keyDir.get(key);
            try (InputStream is = new FileInputStream(keyDirRecord.fileId())) {
                long actualSkipped = is.skip(keyDirRecord.valuePosition());
                if (actualSkipped != keyDirRecord.valuePosition()) throw new RuntimeException();
                value = new String(is.readNBytes(keyDirRecord.valueSize()));
            }
            return value.equals(BitcaskStore.DELETED_VALUE) ? null : value;
        }
    }

    /**
     * Adds a new entry in the Bitcask store.
     */
    void addEntry(String key, String value) throws IOException {
        if (this.isReadOnly) throw new RuntimeException("Handler has no write permission");
        else if (this.dataOutputStream == null) updateDataOutputStream();
        else if (this.dataOutputStream.size() >= BitcaskStore.MAX_FILE_SIZE) {
            this.dataOutputStream.close();
            this.currentFileID++;
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
        if (this.isSyncOn) this.flush();
    }

    /**
     * Returns a list which contains all the keys in the store.
     */
    List<String> listKeys() {
        return keyDir.keySet().stream().toList();
    }

    void fold(BiFunction<String, String, Void> function) {
        listKeys().forEach(key -> {
            try {
                String value = getValue(key);
                function.apply(key, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Merges all files and remove unnecessary files, creates hint files for
     * faster crash recovery and updates the epoch version.
     */
    void merge() throws IOException {
        // Closing the data output stream and updating state.
        this.close();
        this.dataOutputStream = null;
        int prevEpoch = this.epoch;
        int prevId = this.currentFileID;
        this.epoch++;
        this.currentFileID = 1;

        // Writing new values in the merged files and creating hint files
        this.keyDir.keySet().forEach(key -> {
            try {
                String value = getValue(key);
                if (! value.equals(BitcaskStore.DELETED_VALUE)) this.addEntry(key, value);
                else this.keyDir.remove(key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.close();

        // Deleting old files
        List<File> filesToDelete = new ArrayList<>();
        for (int id = 1 ; id <= prevId ; id++)
            filesToDelete.add(new File(this.rootDir + "/epoch_" + prevEpoch + "_" + id));
        for (File file : filesToDelete)
            if (! file.delete()) throw new RuntimeException("Couldn't delete file " + file.getName());

        // Creating Hint files
    }

    /**
     * Flushes the current data output stream if any.
     */
    void flush() throws IOException {
        if (this.dataOutputStream != null)
            this.dataOutputStream.flush();
    }

    /**
     * Closes the current DataOutputStream.
     */
    void close() throws IOException {
        if (this.dataOutputStream != null)
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
     * Reads the next data record from the given inputStream.
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

    /**
     * Reads the next hint record from the given input stream.
     */
    private HintRecord readNextHintRecord(DataInputStream inputStream) throws IOException {
        int timestamp = inputStream.readInt();
        int keySize = inputStream.readInt();
        int valueSize = inputStream.readInt();
        int valuePosition = inputStream.readInt();
        String key = new String(inputStream.readNBytes(keySize));
        return new HintRecord(timestamp, keySize, valueSize, valuePosition, key);
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
    private record HintRecord(int timestamp, int keySize, int valueSize, int valuePosition, String key) {}

    /**
     * Represents a record for the in-memory key directory
     */
    private record KeyDirRecord(String fileId, int valueSize, int valuePosition, int timestamp){}
}