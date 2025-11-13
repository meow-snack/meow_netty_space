package meow.studio.netty.diytcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.CRC32;

public class MessageEncoder extends MessageToByteEncoder<Message> {
    private static final byte VERSION = Protocol.VERSION;

    /**
     * +------------+---------+--------+---------+-----------+--------------+--------+
     * | magic(2)   | ver(1)  | type(1)| seq(4)  | len(4)    | payload(N)   | crc(4) |
     * +------------+---------+--------+---------+-----------+--------------+--------+
     *   0xCAFE        1         0..n     自增      N 字节数      任意字节       CRC32
     *   short       byte        byte     int       int
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        byte[] payload = msg.payload;
        int len = payload.length;

        out.writeShort(Protocol.MAGIC); // 2 Byte
        out.writeByte(VERSION);         // 1 Byte
        out.writeByte(msg.type);        // 1 Byte
        out.writeInt(msg.seq);          // 4 Byte
        out.writeInt(len);              // 4 Byte

        out.writeBytes(payload);

        CRC32 crc = new CRC32();
        crc.update(msg.payload, 0, len);
        out.writeInt((int) crc.getValue());
    }
}
