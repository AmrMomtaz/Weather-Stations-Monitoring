/**
 * Created by Amr Momtaz.
 */

import org.store.BitCaskHandle;
import org.store.BitcaskStore;

import java.util.List;

/**
 * Main Driver code used for testing.
 */
public class Main {
    public static void main(String[] args) {
        BitcaskStore bitcaskStore = new BitcaskStore();
        BitCaskHandle bitCaskHandle = bitcaskStore.open
                ("testt", List.of(BitcaskStore.OPTIONS.READ_WRITE_OPTION));
//        for (int i = 0 ; i < 1000 ; i++) bitcaskStore.put(bitCaskHandle, String.valueOf(i), (i + "").repeat(50));
//        bitcaskStore.close(bitCaskHandle);
//        bitcaskStore.merge(bitCaskHandle);
        bitcaskStore.fold(bitCaskHandle, (k, v) -> {
            System.out.println(k + " " + v);
            return null;
        });
    }
}
