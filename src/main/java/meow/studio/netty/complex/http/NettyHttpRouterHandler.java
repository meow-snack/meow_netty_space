package meow.studio.netty.complex.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static meow.studio.netty.complex.http.JsonSupport.MAPPER;

public class NettyHttpRouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

        URI uri = new URI(req.uri());
        String path = uri.getPath();
        Map<String, List<String>> q = decodeQuery(uri.getRawQuery());

        if ("/favicon.ico".equals(path)) {
            writeEmptyIco(ctx, req);
            return;
        }
        try {
            if (req.method().equals(HttpMethod.GET)) {
                handleGet(ctx, req, path, q);
            } else if (req.method().equals(HttpMethod.POST)) {
                handlePost(ctx, req, path);
            } else if (req.method().equals(HttpMethod.OPTIONS)) {
                DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, NO_CONTENT);
                setCommonHeaders(resp, req);
                ctx.writeAndFlush(resp);
            } else {
                writeJson(ctx, req, METHOD_NOT_ALLOWED, Map.of("error", "Method Not Allowed", "allowed", List.of("GET", "POST", "OPTIONS")));
            }
        } catch (Throwable t) {
            t.printStackTrace();
            writeJson(ctx, req, INTERNAL_SERVER_ERROR, Map.of("error", "Internal Server Error", "message", t.getMessage()));
        }
    }

    private void handleGet(ChannelHandlerContext ctx, FullHttpRequest req, String path, Map<String, List<String>> q) throws Exception {
        switch (path) {
            case"/" -> {
                String name = first(q, "name", "meow");
                writeJson(ctx, req, OK, Map.of(
                        "message", "Hello, " + name + "!",
                        "time", Instant.now().toString(),
                        "path", path
                ));
            }
            case "/headers" -> {
                Map<String, String> headers = new LinkedHashMap<>();
                req.headers().forEach(h -> headers.put(h.getKey(), h.getValue()));
                writeJson(ctx, req, OK, Map.of("headers", headers));
            }
            case "/download" -> {
                String fileName = first(q, "file", "README.md");
                File f = new File(fileName);
                if (!f.exists() || f.isDirectory()) {
                    writeJson(ctx, req, NOT_FOUND, Map.of("error", "File Not Found", "file", fileName));
                    return;
                }
                writeFile(ctx, req, f);
            }
            case "/sse" -> {
                startSse(ctx, req);
            }
            default -> writeJson(ctx, req, NOT_FOUND, Map.of("error", "Not Found", "path", path));
        }
    }

    private void handlePost(ChannelHandlerContext ctx, FullHttpRequest req, String path) throws Exception {
        switch (path) {
            case "/echo" -> {
                String ct = Optional.ofNullable(req.headers().get(CONTENT_TYPE)).orElse("");
                if (!ct.toLowerCase().contains("application/json")) {
                    writeJson(ctx, req, UNSUPPORTED_MEDIA_TYPE,
                            Map.of("error", "Content-Type must be application/json"));
                    return;
                }

                try {
                    byte[] bytes = ByteBufUtil.getBytes(req.content());
                    Map<String, Object> body = MAPPER.readValue(bytes, new TypeReference<>() {});
                    writeJson(ctx, req, OK, Map.of(
                            "message", "Meow~",
                            "received", body
                    ));
                } catch (JsonProcessingException e) {
                    writeJson(ctx, req, BAD_REQUEST,
                            Map.of("error", "invalid json", "message", e.getOriginalMessage()));
                } catch (Exception e) {
                    writeJson(ctx, req, INTERNAL_SERVER_ERROR,
                            Map.of("error", "server error", "message", e.getMessage()));
                }
            }
            default -> writeJson(ctx, req, NOT_FOUND, Map.of("error", "Not Found", "path", path));
        }
    }
    private void setCommonHeaders(HttpResponse resp, FullHttpRequest req) {
        resp.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        resp.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        resp.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");

        if (HttpUtil.isKeepAlive(req)) {
            resp.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
    }

    private void writeAndMaybeClose(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        ChannelFuture f = ctx.writeAndFlush(resp);
        if (!HttpUtil.isKeepAlive(req)) {
            // 判断是否有 “keep-alive” 标识
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    private static Map<String, List<String>> decodeQuery(String rawQuery) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        QueryStringDecoder decoder = new QueryStringDecoder(rawQuery, CharsetUtil.UTF_8, false);
        decoder.parameters().forEach((k, v) -> map.put(k, new ArrayList<>(v)));
        return map;
    }

    private static String first(Map<String, List<String>> q, String key, String def) {
        List<String> v = q.get(key);
        return (v == null || v.isEmpty()) ? def : v.get(0);
    }

    private void writeEmptyIco(ChannelHandlerContext ctx, FullHttpRequest req) {
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        setCommonHeaders(resp, req);
        resp.headers().set(CONTENT_TYPE, "image/x-icon");
        resp.headers().setInt(CONTENT_LENGTH, 0);
        writeAndMaybeClose(ctx, req, resp);
    }

    private void writeJson(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, Object payload) {
        byte[] bytes;
        try {
            bytes = MAPPER.writeValueAsBytes(payload);
        } catch (Exception e) {
            String fallback = "{\"error\":\"serialization failed\",\"msg\":\"" + e.getMessage() + "\"}";
            bytes = fallback.getBytes(CharsetUtil.UTF_8);
            status = INTERNAL_SERVER_ERROR;
        }

        var resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes));
        setCommonHeaders(resp, req);
        resp.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        resp.headers().setInt(CONTENT_LENGTH, bytes.length);
        writeAndMaybeClose(ctx, req, resp);
    }

    private void writeFile(ChannelHandlerContext ctx, FullHttpRequest req, File file) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"); FileChannel fileChannel = raf.getChannel()) {
            long fileLength = raf.length();
            DefaultHttpResponse resp =  new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            setCommonHeaders(resp, req);

            resp.headers().set(CONTENT_TYPE, "application/octet-stream");
            resp.headers().set(CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            HttpUtil.setContentLength(resp, fileLength);

            ctx.write(resp);
            ctx.write(new ChunkedNioFile(fileChannel, 0, fileLength, 8192));

            ChannelFuture last = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            if (HttpUtil.isKeepAlive(req)) {
                last.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private void startSse(ChannelHandlerContext ctx, FullHttpRequest req) {
        DefaultHttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        setCommonHeaders(resp, req);
        resp.headers().set(CONTENT_TYPE, "text/event-stream; charset=UTF-8");
        resp.headers().set(CACHE_CONTROL, "no-cache");
        resp.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ctx.writeAndFlush(resp);

        final int[] counter = {0};
        ScheduledFuture<?> task = ctx.executor().scheduleAtFixedRate(() -> {
            String data = "event: tick\nid: " + counter[0] + "\ndata: " + Instant.now() + "\n\n";
            ctx.writeAndFlush(new DefaultHttpContent(Unpooled.copiedBuffer(data, CharsetUtil.UTF_8)));
            counter[0]++;
            if (counter[0] >= 10) {
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(f -> ctx.close());
            }
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);

        ctx.channel().closeFuture().addListener(f -> task.cancel(false));
    }
}
