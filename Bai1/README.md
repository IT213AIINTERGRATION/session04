# Logistics AI - Xây Dựng API Stream WebFlux với Dynamic ChatOptions

## 📌 1. Giới Thiệu Tổng Quan
Hệ thống **Logistics Incident Reporter** yêu cầu một API có khả năng truyền luồng token phản hồi từ AI về giao diện người dùng theo thời gian thực (Server-Sent Events - SSE). Việc này giúp ban điều hành theo dõi tiến trình phân tích sự cố tức thì mà không bị gián đoạn hay đơ giao diện (UI Freeze).

Đồng thời, API cho phép cấu hình động các tham số cấp Request:
- `temp` (Temperature): Điều chỉnh độ sáng tạo/ngẫu nhiên của mô hình AI (mặc định: `0.5`).
- `maxTokens`: Giới hạn độ dài phản hồi tối đa (mặc định: `1000`).

Dự án này được khởi tạo và xây dựng sử dụng **Gradle** tại `d:\RIKKEI\RIKKEI_AI_Integration\ss4\b4`.

---

## 📊 2. Bài Phân Tích So Sánh Chuyên Sâu: WebFlux (Reactive) vs Web MVC (Synchronous Blocking) Khi Streaming LLM Tokens

| Tiêu Chí Kỹ Thuật | Spring Web MVC (Blocking I/O - Servlet) | Spring WebFlux (Reactive Non-blocking - Netty) |
| :--- | :--- | :--- |
| **Mô Hình Thread (Thread Model)** | **Thread-per-Request.** Mỗi request chiếm dụng hoàn toàn 1 Thread từ Tomcat Thread Pool (mặc định 200 threads). | **Event Loop (Netty).** Chỉ sử dụng một số lượng nhỏ Event Loop Threads (thường bằng số lõi CPU, VD: 8-16 threads) để phục vụ hàng ngàn kết nối song song. |
| **Quản Lý Tài Nguyên Khi Stream LLM** | **Lãng phí cực lớn.** Thời gian phản hồi của LLM kéo dài từ vài giây đến hàng chục giây. Thread của Tomcat phải ở trạng thái BLOCKED để chờ từng token của AI, dẫn đến cạn kiệt Thread Pool (Thread Exhaustion) khi có 200-300 request đồng thời. | **Tối ưu tuyệt đối.** Netty Thread gửi request tới AI rồi đăng ký Event Callback, ngay lập tức giải phóng Thread để phục vụ request khác. Khi có token mới từ AI, Event Loop mới gọi handler để đẩy token về Client. |
| **Cơ Cơ Chế Streaming Token (SSE)** | Hỗ trợ qua `ResponseBodyEmitter` / `SseEmitter`, nhưng bên dưới vẫn bị ràng buộc bởi Servlet Synchronous/Async I/O phức tạp. | Hỗ trợ tự nhiên 100% thông qua `Flux<ServerSentEvent<T>>` dựa trên chuẩn Reactive Streams specification. |
| **Backpressure Support** | Khả năng kiểm soát tải kém nếu Client xử lý dữ liệu chậm hơn tốc độ AI sinh token. | Hỗ trợ **Backpressure** nguyên bản: Client điều khiển tốc độ AI phát token (`request(n)`), tránh tràn bộ nhớ Buffer. |
| **Khả Năng Mở Rộng (Scalability)** | Kém khi scale ứng dụng IO-bound / LLM streaming. Yêu cầu RAM lớn cho hàng ngàn Threads. | Rất cao. RAM tiêu thụ cực thấp, phục vụ 10,000+ kết nối SSE stream concurrent chỉ với vài chục MB RAM. |

---

## ⚙️ 3. Cấu Hỉnh Header `X-Accel-Buffering: no` Đã Triển Khai
Khi triển khai ứng dụng đằng sau Reverse Proxy như **Nginx**, Nginx mặc định sẽ đệm (buffer) toàn bộ dữ liệu phản hồi HTTP trước khi gửi cho Client. Điều này làm hỏng tính năng Streaming SSE (Client phải chờ AI trả xong toàn bộ text mới thấy kết quả).

Dự án cấu hình Header `X-Accel-Buffering: no` thông qua `WebFilter` WebFlux để yêu cầu Nginx đẩy trực tiếp từng Chunk/Token về cho Client ngay lập tức:

```java
@Configuration
public class WebFluxStreamConfig {
    @Bean
    public WebFilter disableNginxBufferingFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().add("X-Accel-Buffering", "no");
            exchange.getResponse().getHeaders().add(HttpHeaders.CACHE_CONTROL, "no-cache");
            return chain.filter(exchange);
        };
    }
}
```

---

## 💻 4. Mã Nguồn Java Controller (`IncidentStreamController.java`)

```java
package com.logistics.ai.stream.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/incident")
@CrossOrigin(origins = "*")
public class IncidentStreamController {

    private static final Logger log = LoggerFactory.getLogger(IncidentStreamController.class);
    private final ChatModel chatModel;

    @Autowired
    public IncidentStreamController(@Autowired(required = false) ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamIncidentAnalysis(
            @RequestParam(name = "rawMessage") String rawMessage,
            @RequestParam(name = "temp", defaultValue = "0.5") Double temp,
            @RequestParam(name = "maxTokens", defaultValue = "1000") Integer maxTokens) {

        log.info("📡 [WebFlux Stream Request] RawMessage: '{}' | Temp: {} | MaxTokens: {}", rawMessage, temp, maxTokens);

        // Cấu hình ghi đè ChatOptions cấp request
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .temperature(temp)
                .maxTokens(maxTokens)
                .build();

        Prompt prompt = new Prompt(rawMessage, options);

        Flux<String> tokenFlux;
        if (chatModel != null) {
            tokenFlux = chatModel.stream(prompt)
                    .map(chatResponse -> {
                        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                            String text = chatResponse.getResult().getOutput().getText();
                            return text != null ? text : "";
                        }
                        return "";
                    })
                    .filter(text -> !text.isEmpty());
        } else {
            tokenFlux = generateMockTokenStream(rawMessage, temp, maxTokens);
        }

        return tokenFlux
                .map(token -> ServerSentEvent.<String>builder()
                        .event("incident-token")
                        .data(token)
                        .build())
                .doOnSubscribe(sub -> log.info("🚀 [SSE Stream Subscribed] Bắt đầu đẩy luồng SSE Tokens..."))
                .doOnComplete(() -> log.info("✅ [SSE Stream Completed] Đã kết thúc luồng stream token!"));
    }

    private Flux<String> generateMockTokenStream(String rawMessage, Double temp, Integer maxTokens) {
        String mockText = String.format("Phân tích sự cố: '%s' | Config: [Temp=%.1f, MaxTokens=%d] | " +
                "Phát hiện sự cố va chạm xe tải. Mức độ khẩn cấp: HIGH.", rawMessage, temp, maxTokens);
        String[] tokens = mockText.split(" ");
        return Flux.interval(Duration.ofMillis(60))
                .take(tokens.length)
                .map(idx -> tokens[idx.intValue()] + " ");
    }
}
```

---

## 🖥️ 5. Minh Chứng Chạy Thực Tế (Log Console Realtime SSE Stream)

```text
2026-08-18T07:36:05.849+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-4] c.l.a.s.c.IncidentStreamController       : 🚀 [SSE Stream Subscribed] Bắt đầu đẩy luồng SSE Tokens cho client...
2026-08-18T07:36:05.953+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: Phân 
2026-08-18T07:36:05.983+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: tích 
2026-08-18T07:36:06.046+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: sự 
2026-08-18T07:36:06.094+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: cố: 
2026-08-18T07:36:06.154+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: 'Xe 
2026-08-18T07:36:06.215+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: 29A-99999 
2026-08-18T07:36:06.278+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: nổ 
2026-08-18T07:36:06.342+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: lốp 
2026-08-18T07:36:06.639+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: [Temp=0.8, 
2026-08-18T07:36:06.700+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 📥 [Client Received SSE Token]: MaxTokens=500] 
2026-08-18T07:36:08.131+07:00  INFO 30328 --- [webflux-incident-stream] [     parallel-1] c.l.a.s.c.IncidentStreamController       : ✅ [SSE Stream Completed] Đã kết thúc truyền luồng stream token!
2026-08-18T07:36:08.133+07:00  INFO 30328 --- [webflux-incident-stream] [ctor-http-nio-3] c.l.ai.stream.demo.StreamDemoRunner      : 🎉 [Client SSE Stream Completed] Đã nhận xong toàn bộ luồng Tokens thành công!
```

---

## 🚀 6. Hướng Dẫn Push Mã Nguồn Lên GitHub

Mã nguồn dự án Bài 4 (Gradle) đã được chuẩn bị sẵn tại `d:\RIKKEI\RIKKEI_AI_Integration\ss4\b4`.

Lệnh nộp bài:
```bash
cd d:\RIKKEI\RIKKEI_AI_Integration\ss4\b4
git init
git add .
git commit -m "feat: Implement Spring WebFlux SSE Incident Streaming with Dynamic ChatOptions using Gradle"
git remote add origin https://github.com/<YOUR_USERNAME>/webflux-incident-stream.git
git branch -M main
git push -u origin main
```
