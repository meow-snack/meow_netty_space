package meow.studio.netty.diytcp;

public class Protocol {
    public static final short MAGIC = (short) 0x1123;
    public static final byte VERSION = (byte) 1;

    public static final byte TYPE_ECHO_REQ = 0x01;
    public static final byte TYPE_ECHO_RES = 0x02;
    public static final byte TYPE_PING = 0x10;
    public static final byte TYPE_PONG = 0x11;

    private Protocol(){}
}
