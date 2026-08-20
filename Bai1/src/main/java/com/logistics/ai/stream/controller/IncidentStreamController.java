package com.logistics.ai.stream.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * REST Controller WebFlux cung cấp API SSE Streaming phân tích sự cố thời gian thực
 * tích hợp ChatOptions động (temperature & maxTokens).
 */
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

    /**
     * Endpoint API SSE Stream phân tích tin nhắn sự cố theo thời gian thực.
     * 
     * @param rawMessage Tin nhắn thô từ tài xế gửi lên
     * @param temp Tham số ngẫu nhiên / sáng tạo của AI (mặc định 0.5)
     * @param maxTokens Giới hạn số token phản hồi tối đa (mặc định 1000)
     * @return Flux<ServerSentEvent<String>> luồng token gửi về Client dạng Server-Sent Events
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamIncidentAnalysis(
            @RequestParam(name = "rawMessage") String rawMessage,
            @RequestParam(name = "temp", defaultValue = "0.5") Double temp,
            @RequestParam(name = "maxTokens", defaultValue = "1000") Integer maxTokens) {

        log.info("📡 [WebFlux Stream Request] Nhận request stream tin nhắn: '{}' | Temp: {} | MaxTokens: {}",
                rawMessage, temp, maxTokens);

        // 1. Cấu hình ghi đè ChatOptions cấp request
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .temperature(temp)
                .maxTokens(maxTokens)
                .build();


        Prompt prompt = new Prompt(rawMessage, options);

        // 2. Kiểm tra ChatModel và tạo luồng Flux Token
        Flux<String> tokenFlux;
        if (chatModel != null) {
            log.info("🤖 [WebFlux Stream] Kết nối tới AI ChatModel active...");
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
            log.warn("⚠️ [WebFlux Stream] ChatModel chưa được cấu hình Key. Sử dụng Mock Live Token Stream...");
            tokenFlux = generateMockTokenStream(rawMessage, temp, maxTokens);
        }

        // 3. Đóng gói luồng thành Server-Sent Events kèm Header X-Accel-Buffering: no
        return tokenFlux
                .map(token -> ServerSentEvent.<String>builder()
                        .event("incident-token")
                        .data(token)
                        .build())
                .doOnSubscribe(subscription -> log.info("🚀 [SSE Stream Subscribed] Bắt đầu đẩy luồng SSE Tokens cho client..."))
                .doOnComplete(() -> log.info("✅ [SSE Stream Completed] Đã kết thúc truyền luồng stream token!"))
                .doOnError(e -> log.error("❌ [SSE Stream Error] Lỗi truyền luồng: {}", e.getMessage(), e));
    }

    /**
     * Giả lập luồng token đẩy theo thời gian thực (50ms / token) phục vụ thử nghiệm không phụ thuộc API Key ngoài.
     */
    private Flux<String> generateMockTokenStream(String rawMessage, Double temp, Integer maxTokens) {
        String mockAnalysisText = String.format(
                "Phân tích sự cố: '%s' | Config: [Temp=%.1f, MaxTokens=%d] | " +
                "Phát hiện sự cố va chạm xe tải. Mức độ khẩn cấp: HIGH. Đề xuất: Cử đội cứu hộ giao thông khẩn cấp.",
                rawMessage, temp, maxTokens
        );
        String[] tokens = mockAnalysisText.split(" ");

        return Flux.interval(Duration.ofMillis(60))
                .take(tokens.length)
                .map(index -> tokens[index.intValue()] + " ");
    }
}
