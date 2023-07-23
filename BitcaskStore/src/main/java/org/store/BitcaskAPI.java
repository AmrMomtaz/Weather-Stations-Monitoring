/**
 * Created by Amr Momtaz.
 */

package org.store;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Declaration of the API described by the https://riak.com/assets/bitcask-intro.pdf.
 */
public interface BitcaskAPI {

    /**
     * Opens a new or existing Bitcask datastore with additional options.
     * Valid options include read_write (if this process is going to be a
     * writer and not just a reader) and sync_on_put (if this writer would
     * prefer to sync the write file after every write operation).
     * The directory must be readable and writable by this process, and
     * only one process may open a Bitcask with read write at a time.
     */
    BitCaskHandle open(String directoryName, List<BitcaskStore.OPTIONS> opts);

    /**
     * Opens a new or existing Bitcask datastore for read-only access.
     * The directory and all files in it must be readable by this process.
     */
    BitCaskHandle open(String directoryName);

    /**
     * Retrieves a value by key from a Bitcask datastore.
     */
    String get(BitCaskHandle bitCaskHandle, String key);

    /**
     * Stores a key and value in a Bitcask datastore.
     * Returns true if the operation is successful and false otherwise.
     */
    boolean put(BitCaskHandle bitCaskHandle, String key, String value);

    /**
     * Delete a key from a Bitcask datastore.
     * Returns true if the operation is successful and false otherwise.
     */
    boolean delete(BitCaskHandle bitCaskHandle, String key);

    /**
     * Lists all keys in a Bitcask datastore.
     */
    List<String> listKeys(BitCaskHandle bitCaskHandle);

    /**
     * Merge several data files within a Bitcask datastore into a more
     * compact form. Also, produce hint-files for faster startup.
     * Returns true if the operation is successful and false otherwise.
     */
    boolean merge(BitCaskHandle bitCaskHandle);

    /**
     * Force any writes to sync to disk.
     * Returns true if the operation is successful and false otherwise.
     */
    boolean sync(BitCaskHandle bitCaskHandle);

    /**
     * Close a Bitcask data store and flush all pending writes (if any) to disk.
     * Returns true if the operation is successful and false otherwise.
     */
    boolean close(BitCaskHandle bitCaskHandle);

    /**
     * Folds over all K/V pairs in a Bitcask datastore.
     */
    void fold(BitCaskHandle bitCaskHandle, BiFunction<String, String, Void> function);
}
