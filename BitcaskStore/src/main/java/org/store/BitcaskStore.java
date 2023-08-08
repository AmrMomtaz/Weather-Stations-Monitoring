/**
 * Created by Amr Momtaz.
 */

package org.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.function.BiFunction;

public class BitcaskStore implements BitcaskAPI {

    // Constants
    public static final long MAX_FILE_SIZE = 1*1024; // The maximum size in bytes for each file segment
    public static final String DELETED_VALUE = "___DELETED___1019___"; // Determines that a certain key was deleted
    private static final Logger logger = LogManager.getLogger(BitcaskStore.class);

    public enum OPTIONS {
        READ_WRITE_OPTION,
        SYNC_ON_PUT_OPTION
    }

    // State variables

    @Override
    public BitCaskHandle open(String directoryName, List<OPTIONS> opts) {
        boolean isReadOnly = true, isSyncOn = false;
        if (opts != null) {
            if (opts.contains(OPTIONS.READ_WRITE_OPTION)) isReadOnly = false;
            if (opts.contains(OPTIONS.SYNC_ON_PUT_OPTION)) isSyncOn = true;
        }
        File directory = new File(directoryName);
        if (! directory.exists()) {
            boolean created = directory.mkdirs();
            if (! created) {
                logger.error("Couldn't create root directory: " + directoryName);
                throw new RuntimeException();
            }
        }
        try {
            return new BitCaskHandle(directoryName, isReadOnly, isSyncOn);
        } catch (Exception e) {
            logger.error("Couldn't initialize the bitcask handler");
            throw new RuntimeException(e);
        }
    }

    @Override
    public BitCaskHandle open(String directoryName) {
        return open(directoryName, null);
    }

    @Override
    public String get(BitCaskHandle bitCaskHandle, String key) {
        try {
            return bitCaskHandle.getValue(key);
        } catch (IOException e) {
            logger.error("Couldn't get value for the following key (" + key + "). [" + e + "]");
            return null;
        }
    }

    @Override
    public boolean put(BitCaskHandle bitCaskHandle, String key, String value) {
        try {
            bitCaskHandle.addEntry(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Couldn't add new entry. [" + e + "]");
            return false;
        }
    }

    @Override
    public boolean delete(BitCaskHandle bitCaskHandle, String key) {
        return this.put(bitCaskHandle, key, DELETED_VALUE);
    }

    @Override
    public List<String> listKeys(BitCaskHandle bitCaskHandle) {return bitCaskHandle.listKeys();}

    @Override
    public boolean merge(BitCaskHandle bitCaskHandle) {
        try {
            bitCaskHandle.merge();
            return true;
        } catch (Exception e) {
            logger.error("Couldn't merge the store. [" + e + "]");
            return false;
        }
    }

    @Override
    public boolean sync(BitCaskHandle bitCaskHandle) {
        try {
            bitCaskHandle.flush();
            return true;
        }
        catch (Exception e) {
            logger.error("Couldn't sync the handler. [" + e + "]");
            return false;
        }
    }

    @Override
    public boolean close(BitCaskHandle bitCaskHandle) {
        try {
            bitCaskHandle.close();
            return true;
        } catch (Exception e) {
            logger.error("Couldn't close the handler. [" + e + "]");
            return false;
        }
    }

    @Override
    public void fold(BitCaskHandle bitCaskHandle,
                     BiFunction<String, String, Void> function) {
        bitCaskHandle.fold(function);
    }
}