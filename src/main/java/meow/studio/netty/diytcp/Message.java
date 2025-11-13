package meow.studio.netty.diytcp;

import java.util.Arrays;

public class Message {
    public final byte type;
    public final int seq;
    public final byte[] payload;


    public Message(byte type, int seq, byte[] payload) {
        this.type = type;
        this.seq = seq;
        this.payload = payload == null ? new byte[0] : payload;
    }

    @Override
    public String toString() {
        return String.format("Message{type=0x%s, seq=%s, payload=%s}", Integer.toHexString(type & 0xff), seq, Arrays.toString(payload));
    }
}
