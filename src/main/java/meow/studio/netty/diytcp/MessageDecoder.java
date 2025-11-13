package meow.studio.netty.diytcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.zip.CRC32;

public class MessageDecoder extends ByteToMessageDecoder {
    /**
     * +------------+---------+--------+---------+-----------+--------------+--------+
     * | magic(2)   | ver(1)  | type(1)| seq(4)  | len(4)    | payload(N)   | crc(4) |
     * +------------+---------+--------+---------+-----------+--------------+--------+
     *   0xCAFE        1         0..n     自增      N 字节数      任意字节       CRC32
     *   short       byte        byte     int       int
     */

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 标记当前读指针
        in.markReaderIndex();

        // 头数据不完整: 重读
        if (in.readableBytes() < 2 + 1 + 1 + 4 + 4) {
            in.resetReaderIndex();
            return;
        }

        // 读取头数据
        short magic = in.readShort();
        byte version = in.readByte();
        byte type = in.readByte();
        int seq = in.readInt();
        int len = in.readInt();

        // 判断是否是我们的包及包版本是否匹配
        if (magic != Protocol.MAGIC || version != Protocol.VERSION || len<0) {
            ctx.close();
            return;
        }

        // 包数据不完整: 重读
        if (in.readableBytes() < len + 4) {
            in.resetReaderIndex();
            return;
        }

        // 读取包数据
        byte[] payload = new byte[len];
        in.readBytes(payload);

        // 进行 CRC 校验
        int target_crc = in.readInt();
        CRC32 crc = new CRC32();
        crc.update(payload, 0, len);

        // CRC 校验失败, 关闭连接
        if ((int)crc.getValue() != target_crc) {
            ctx.close();
            return;
        }

        out.add(new Message(type, seq, payload));
    }
}
