package io.descoped.helidon.application.test.server;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class PathElementTest {

    static final Logger LOG = LoggerFactory.getLogger(PathElementTest.class);

    @Test
    void name() {
        Map<byte[], String> map = new TreeMap<>(new Comparator<byte[]>() {
            @Override
            public int compare(byte[] left, byte[] right) {
                for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
                    int a = (left[i] & 0xff);
                    int b = (right[j] & 0xff);
                    if (a != b) {
                        return a - b;
                    }
                }
                return left.length - right.length;
            }
        });

        byte b1 = 0x0034;
        byte b2 = 64;
        int r1 = b1 & 0xff;
        int r2 = b2 & 0xff;

        LOG.trace("{} {} {} {}", b1, r1, r2, 0xff);

        /*
            [count][offset]bytes[offset][bytes]

            [2][4]path[1]1
            [2][4]path[1]2
            [2][4]pbth[1]2


         */

//        map = new HashMap<>();

//        Arrays.equals()

        {
            ByteBuffer bb = ByteBuffer.allocateDirect(511);
            bb.putInt("/path/2".length());
            bb.put("/path/2".getBytes());
            bb.flip();
            byte[] key = new byte[bb.remaining()];
            bb.get(key);
            map.put(key, "B");
        }

        {
            ByteBuffer bb = ByteBuffer.allocateDirect(511);
            bb.putInt("/path/1".length());
            bb.put("/path/1".getBytes());
            bb.flip();
            byte[] key = new byte[bb.remaining()];
            bb.get(key);
            map.put(key, "A");
        }

        LOG.trace("{}", map);
    }
}
