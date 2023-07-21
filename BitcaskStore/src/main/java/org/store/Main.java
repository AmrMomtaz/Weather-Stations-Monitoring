/**
 * Created by Amr Momtaz.
 */

package org.store;

import java.io.IOException;

/**
 * Main Driver code used for testing.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        BitcaskStore bitcaskStore = new BitcaskStore();
        BitCaskHandle bitCaskHandle = bitcaskStore.open("testt");
        String value = "Amr Momtaz plays league of legends all the time";
        for (int i = 0 ; i < 1000 ; i++) {
            bitcaskStore.put(bitCaskHandle, String.valueOf(i), value);
        }
        bitcaskStore.close(bitCaskHandle);
        for (int i = 0 ; i < 1000 ; i++) {
            System.out.println(bitcaskStore.get(bitCaskHandle, String.valueOf(i)));
        }
    }
}
