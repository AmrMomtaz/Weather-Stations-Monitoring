/**
 * Created by Amr Momtaz.
 */

package org.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;

public class BitcaskStore implements BitcaskAPI {

    // Constants
    public static final long MAX_FILE_SIZE = 10000*1024; // The maximum size in bytes for each file segment
    private static final Logger logger = LogManager.getLogger(BitcaskStore.class);

    // State variables


    public BitcaskStore() {}


    @Override
    public BitCaskHandle open(String directoryName, List<String> opts) {
        // TODO: check that the directory is already created
        return null;
    }

    @Override
    public BitCaskHandle open(String directoryName) {
        File directory = new File(directoryName);
        if (! directory.exists()) {
            boolean created = directory.mkdirs();
            if (! created) {
                logger.error("Couldn't create root directory: " + directoryName);
                throw new RuntimeException();
            }
        }
        try {
            return new BitCaskHandle(directoryName);
        } catch (Exception e) {
            logger.error("Couldn't create a new bitcask handler");
            throw new RuntimeException();
        }
    }

    @Override
    public String get(BitCaskHandle bitCaskHandle, String key) {
        try {
            return bitCaskHandle.getValue(key);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean put(BitCaskHandle bitCaskHandle, String key, String value) {
        try {
            bitCaskHandle.addEntry(key, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean delete(BitCaskHandle bitCaskHandle, String key) {
        return false;
    }

    @Override
    public List<String> listKeys(BitCaskHandle bitCaskHandle) {return bitCaskHandle.listKeys();}

    @Override
    public boolean merge(BitCaskHandle bitCaskHandle) {
        return false;
    }

    @Override
    public boolean sync(BitCaskHandle bitCaskHandle) {
        return false;
    }

    @Override
    public boolean close(BitCaskHandle bitCaskHandle) {
        try {
            bitCaskHandle.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
