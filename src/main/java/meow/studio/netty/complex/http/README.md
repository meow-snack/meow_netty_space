# Usage

```shell
# GET
curl 'http://127.0.0.1:1123/'
curl 'http://127.0.0.1:1123/?name=Kitty'
curl 'http://127.0.0.1:1123/headers'

# POST
curl -H 'Content-Type: application/json' -d '{"ping":"pong"}' 'http://127.0.0.1:1123/echo'

# DOWNLOAD
curl -OJ 'http://127.0.0.1:1123/download?file=README.md'

# SSE（Server-Sent Events，服务端推送事件）
curl -N 'http://127.0.0.1:1123/sse'
```

# DefaultFullHttpResponse vs DefaultHttpResponse

## 核心区别

* **DefaultFullHttpResponse（Full）**：状态行 + 头 + **完整内容（ByteBuf）**，一次性返回。
* **DefaultHttpResponse（非 Full）**：**只有状态行+头**，内容后续再**分块**发送。

## 典型场景

* **Full**：小文本/JSON/小文件、错误提示等“一次写完”的响应。
* **非 Full**：大文件下载、SSE 持续推送、流式输出、未知长度内容。

## 用法对照

### 一次性完整响应（Full）

```java
ByteBuf body = Unpooled.wrappedBuffer(bytes);
FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK, body);
resp.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
resp.headers().setInt(CONTENT_LENGTH, body.readableBytes());
ctx.writeAndFlush(resp); // 需要的话再按 keep-alive 关闭或保持
```

### 分块/流式响应（非 Full）

```java
// 1) 先发只有头的响应
DefaultHttpResponse head = new DefaultHttpResponse(HTTP_1_1, OK);
head.headers().set(CONTENT_TYPE, "text/event-stream; charset=UTF-8");
// 已知长度则设置 Content-Length；未知长度走 chunked：
HttpUtil.setTransferEncodingChunked(head, true);
ctx.write(head);

// 2) 发送若干内容块（举例：SSE/大文件）
ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer("data: hello\n\n", UTF_8)));
// 或：ctx.write(new ChunkedNioFile(fileChannel)); // 有 TLS 用它

// 3) 结束块
ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
```

## Header 与管线配合

* **Full**：通常自己设 `Content-Length`；有 `HttpObjectAggregator` 时最顺手。
* **非 Full**：二选一：

    * 已知长度 → 设 `Content-Length`；
    * 未知/流式 → `Transfer-Encoding: chunked` + `LastHttpContent` 结尾。
* 大文件/流式：pipeline 里放 `ChunkedWriteHandler`；有压缩时顺序通常是
  `router → compressor → chunked → codec`（出站是逆序）。

## 选择指引

* 返回 JSON/HTML、小字节数组 → **Full**
* SSE、持续输出、未知长度 → **非 Full**
* 大文件：

    * **无 SSL** → `DefaultHttpResponse + DefaultFileRegion`（零拷贝）
    * **有 SSL** → `DefaultHttpResponse + ChunkedNioFile`

## 常见坑

* 用 **非 Full** 却忘写 `LastHttpContent` → 客户端挂起不结束。
* 同时设置 `Content-Length` **又**使用 chunked → 头部冲突。
* 大内容用 **Full** 全塞内存 → 内存压力大、不可边写边传。
* `HttpHeaderNames.KEEP_ALIVE` 已弃用：保持长连用
  `resp.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);`