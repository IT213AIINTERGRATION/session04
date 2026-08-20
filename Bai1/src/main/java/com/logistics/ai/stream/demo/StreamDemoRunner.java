package com.logistics.ai.stream.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Runner kiểm chứng tự động kết nối API Stream SSE thông qua WebClient.
 */
@Component
public class StreamDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StreamDemoRunner.class);

    @Override
    public void run(String... args) throws Exception {
        log.info("==========================================================================");
        log.info("🚀 CHẠY DEMO WEBFLUX STREAMING TOKEN SỬ DỤNG WEBCLIENT");
        log.info("==========================================================================");

        // Khởi tạo WebClient gọi tới chính API của ứng dụng
        WebClient client = WebClient.create("http://localhost:8083");

        ParameterizedTypeReference<ServerSentEvent<String>> typeRef =
                new ParameterizedTypeReference<>() {};

        log.info("📡 Đang gửi request SSE tới: /api/v1/incident/stream (rawMessage='Xe 29A-99999 nổ lốp', temp=0.8, maxTokens=500)");

        // Kích hoạt luồng Stream bất đồng bộ
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/incident/stream")
                        .queryParam("rawMessage", "Xe 29A-99999 nổ lốp tại KM20")
                        .queryParam("temp", 0.8)
                        .queryParam("maxTokens", 500)
                        .build())
                .retrieve()
                .bodyToFlux(typeRef)
                .doOnNext(sse -> {
                    if (sse.data() != null) {
                        log.info("📥 [Client Received SSE Token]: {}", sse.data());
                    }
                })
                .doOnComplete(() -> {
                    log.info("==========================================================================");
                    log.info("🎉 [Client SSE Stream Completed] Đã nhận xong toàn bộ luồng Tokens thành công!");
                    log.info("==========================================================================\n");
                })
                .doOnError(e -> log.error("❌ [Client Stream Error]: {}", e.getMessage()))
                .subscribe();
    }
}
