# AI Logistics Incident Reporter - Triển Khai Cấu Hình Đa Môi Trường (Profiles)

## 📌 1. Giới Thiệu Tổng Quan
Hệ thống **AI Logistics Incident Reporter** được thiết kế dựa trên kiến trúc **Hybrid AI** linh hoạt:
- **Môi trường Local (Phát triển & Chạy thử):** Kết nối trực tiếp tới mô hình `qwen2.5-coder:7b` chạy cục bộ qua **Ollama** (Port `11434`), giúp bảo mật dữ liệu nội bộ và tiết kiệm chi phí API.
- **Môi trường Cloud / Production:** Tự động chuyển đổi sang sử dụng mô hình `google/gemini-2.5-flash` (hoặc bất kỳ LLM nào) thông qua Gateway Aggregator **OpenRouter** (chuẩn OpenAI API) bằng cách sử dụng API Key từ biến môi trường `${ROUTER_API_KEY}`.

---

## 📁 2. Cấu Trúc Dự Án (Project Structure)

```text
ai-logistics-incident-reporter/
├── src/
│   ├── main/
│   │   ├── java/com/logistics/ai/incident/
│   │   │   ├── AiLogisticsIncidentReporterApplication.java  # Main Class
│   │   │   └── controller/
│   │   │       └── SystemConfigController.java              # REST Controller đối soát profile
│   │   └── resources/
│   │       ├── application.properties                       # Cấu hình mặc định (active=local)
│   │       ├── application-local.properties                 # Cấu hình Ollama Local (qwen2.5-coder:7b)
│   │       └── application-cloud.properties                 # Cấu hình OpenRouter Cloud (gemini-2.5-flash)
│   └── test/
├── pom.xml                                                  # File cấu hình Maven & Dependencies Spring AI
└── README.md                                                # Tài liệu hướng dẫn & giải thích cơ chế
```

---

## ⚙️ 3. Mã Nguồn Cấu Hình & Controller

### 3.1. Các Tệp Tin Cấu Hình Properties

#### 📄 `src/main/resources/application.properties`
```properties
# Main Application Configuration
spring.application.name=ai-logistics-incident-reporter

# Profile hoạt động mặc định khi khởi chạy không truyền tham số
spring.profiles.active=local

# Cấu hình Server Port
server.port=8080
```

#### 📄 `src/main/resources/application-local.properties`
```properties
# Profile: Local (Môi trường phát triển và thử nghiệm cục bộ)
# Mô hình: qwen2.5-coder:7b kết nối qua Ollama local (Port 11434)

# Spring AI Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen2.5-coder:7b
spring.ai.ollama.chat.enabled=true

# Vô hiệu hóa OpenAI Auto-Configuration hoàn toàn ở profile Local
spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration

# Metadata đối soát cấu hình LLM đang hoạt động
app.llm.provider=Ollama (Local)
app.llm.model-name=qwen2.5-coder:7b
app.llm.endpoint=http://localhost:11434
```

#### 📄 `src/main/resources/application-cloud.properties`
```properties
# Profile: Cloud (Môi trường Cloud / Production)
# Mô hình: gemini-2.5-flash kết nối qua OpenRouter (OpenAI-compatible API)

# Spring AI OpenAI Configuration (OpenRouter Endpoint & Environment API Key)
spring.ai.openai.base-url=https://openrouter.ai/api/v1
spring.ai.openai.api-key=${ROUTER_API_KEY:sk-or-v1-mock-router-key-for-testing}
spring.ai.openai.chat.options.model=google/gemini-2.5-flash
spring.ai.openai.chat.enabled=true

# Vô hiệu hóa Ollama Auto-Configuration hoàn toàn ở profile Cloud
spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration

# Metadata đối soát cấu hình LLM đang hoạt động
app.llm.provider=OpenRouter Aggregator (Cloud)
app.llm.model-name=google/gemini-2.5-flash
app.llm.endpoint=https://openrouter.ai/api/v1
```

---

### 3.2. Mã Nguồn REST Controller (`SystemConfigController.java`)

```java
package com.logistics.ai.incident.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller kiểm tra và đối soát cấu hình hệ thống đa môi trường (Profiles).
 * Cung cấp endpoint GET /api/v1/incident/config để xác minh profile LLM hiện tại.
 */
@RestController
@RequestMapping("/api/v1/incident")
public class SystemConfigController {

    private final Environment environment;

    @Value("${spring.application.name:ai-logistics-incident-reporter}")
    private String applicationName;

    @Value("${app.llm.provider:Unknown}")
    private String llmProvider;

    @Value("${app.llm.model-name:Unknown}")
    private String llmModelName;

    @Value("${app.llm.endpoint:Unknown}")
    private String llmEndpoint;

    public SystemConfigController(Environment environment) {
        this.environment = environment;
    }

    /**
     * Endpoint lấy thông tin cấu hình profile và model LLM đang kích hoạt.
     * @return JSON chứa thông tin chi tiết về profile và model AI.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        String[] activeProfiles = environment.getActiveProfiles();
        String currentProfile = (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "default (local)";

        config.put("status", "SUCCESS");
        config.put("applicationName", applicationName);
        config.put("activeProfile", currentProfile);
        config.put("llmProvider", llmProvider);
        config.put("activeModel", llmModelName);
        config.put("endpoint", llmEndpoint);
        config.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(config);
    }
}
```

---

## 🧠 4. Lập Luận Giải Thích Cơ Chế Nạp Profile Động Của Spring Boot

Khi thay đổi tham số `--spring.profiles.active` khi khởi chạy ứng dụng (hoặc qua biến môi trường `SPRING_PROFILES_ACTIVE`), Spring Boot thực hiện quá trình nạp cấu hình và khởi tạo Bean theo các bước tự động sau:

### 1. Quá trình nạp thuộc tính (Property Source Hierarchy)
- **Nạp file cấu hình cơ sở:** Spring Boot nạp tệp `application.properties` trước tiên để thiết lập cấu hình chung và xác định profile mặc định (`spring.profiles.active=local`).
- **Ghi đè thuộc tính theo Profile (Profile-specific overrides):**
  - Khi truyền tham số `--spring.profiles.active=local`, Spring Boot ưu tiên nạp tệp `application-local.properties`. Các giá trị khai báo trong file này sẽ **ghi đè** các giá trị trùng lặp ở `application.properties`.
  - Khi truyền tham số `--spring.profiles.active=cloud`, Spring Boot nạp tệp `application-cloud.properties`, tự động đọc biến môi trường `${ROUTER_API_KEY}` và cập nhật các thuộc tính OpenAI API.

### 2. Khởi tạo Bean dựa trên Điều kiện Auto-Configuration (`ConditionalOnProperty` / `@Profile`)
- Trong Spring AI, các Auto-Configuration (`OllamaAutoConfiguration` và `OpenAiAutoConfiguration`) được điều khiển bằng `@ConditionalOnProperty` và việc kích hoạt các module AutoConfig.
- Bằng cách cấu hình `spring.autoconfigure.exclude` hoặc bật/tắt `chat.enabled`, Spring Boot IoC Container kiểm tra các điều kiện này trong giai đoạn **BeanFactoryPostProcessor**:
  - Đối với **Profile Local:** Spring Boot chỉ khởi tạo Bean `OllamaChatModel` kết nối cổng `11434` với model `qwen2.5-coder:7b`.
  - Đối với **Profile Cloud:** Spring Boot loại bỏ Ollama và khởi tạo Bean `OpenAiChatModel` kết nối endpoint OpenRouter `https://openrouter.ai/api/v1` với model `google/gemini-2.5-flash`.

### 3. Nguyên lý Đảo ngược Phụ thuộc (Dependency Inversion)
Mã nguồn Java (như các Service xử lý sự cố Logistics hay REST Controller) hoàn toàn **không cần sửa bất kỳ dòng mã nào**. Chúng chỉ cần injected interface `ChatModel` của Spring AI hoặc đọc giá trị cấu hình qua `@Value` / `Environment`. Spring Boot sẽ tự động inject Bean đại diện cho Provider tương ứng với Profile đang kích hoạt.

---

## 🧪 5. Minh Chứng Chạy Thực Tế (Log Console & API Response)

### 🟢 Case 1: Chạy ứng dụng với Profile `local`

**Lệnh khởi chạy:**
```bash
java -jar target/ai-logistics-incident-reporter-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**Log Console Khởi Chạy:**
```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.5)

2026-08-18T07:26:57.154+07:00  INFO 27256 --- [ai-logistics-incident-reporter] [           main] i.AiLogisticsIncidentReporterApplication : Starting AiLogisticsIncidentReporterApplication v0.0.1-SNAPSHOT using Java 23.0.2 with PID 27256
2026-08-18T07:26:57.157+07:00  INFO 27256 --- [ai-logistics-incident-reporter] [           main] i.AiLogisticsIncidentReporterApplication : The following 1 profile is active: "local"
2026-08-18T07:26:58.172+07:00  INFO 27256 --- [ai-logistics-incident-reporter] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-08-18T07:26:58.785+07:00  INFO 27256 --- [ai-logistics-incident-reporter] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2026-08-18T07:26:58.804+07:00  INFO 27256 --- [ai-logistics-incident-reporter] [           main] i.AiLogisticsIncidentReporterApplication : Started AiLogisticsIncidentReporterApplication in 2.007 seconds
```

**Kết quả gọi Endpoint GET `http://localhost:8080/api/v1/incident/config`:**
```json
{
  "status": "SUCCESS",
  "applicationName": "ai-logistics-incident-reporter",
  "activeProfile": "local",
  "llmProvider": "Ollama (Local)",
  "activeModel": "qwen2.5-coder:7b",
  "endpoint": "http://localhost:11434",
  "timestamp": "2026-08-18T07:27:06.317839500"
}
```

---

### ☁️ Case 2: Chạy ứng dụng với Profile `cloud`

**Lệnh khởi chạy:**
```bash
java -jar target/ai-logistics-incident-reporter-0.0.1-SNAPSHOT.jar --spring.profiles.active=cloud
```

**Log Console Khởi Chạy:**
```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.5)

2026-08-18T07:27:12.261+07:00  INFO 17880 --- [ai-logistics-incident-reporter] [           main] i.AiLogisticsIncidentReporterApplication : Starting AiLogisticsIncidentReporterApplication v0.0.1-SNAPSHOT using Java 23.0.2 with PID 17880
2026-08-18T07:27:12.264+07:00  INFO 17880 --- [ai-logistics-incident-reporter] [           main] i.AiLogisticsIncidentReporterApplication : The following 1 profile is active: "cloud"
2026-08-18T07:27:13.290+07:00  INFO 17880 --- [ai-logistics-incident-reporter] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-08-18T07:27:13.958+07:00  INFO 17880 --- [ai-logistics-incident-reporter] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2026-08-18T07:27:13.971+07:00  INFO 17880 --- [ai-logistics-incident-reporter] [           main] i.AiLogisticsIncidentReporterApplication : Started AiLogisticsIncidentReporterApplication in 2.029 seconds
```

**Kết quả gọi Endpoint GET `http://localhost:8080/api/v1/incident/config`:**
```json
{
  "status": "SUCCESS",
  "applicationName": "ai-logistics-incident-reporter",
  "activeProfile": "cloud",
  "llmProvider": "OpenRouter Aggregator (Cloud)",
  "activeModel": "google/gemini-2.5-flash",
  "endpoint": "https://openrouter.ai/api/v1",
  "timestamp": "2026-08-18T07:27:20.147886300"
}
```

---

## 🚀 6. Hướng Dẫn Đóng Gói Và Đẩy Lên GitHub

1. Khởi tạo Git repository và commit mã nguồn:
   ```bash
   git init
   git add .
   git commit -m "feat: Implement Spring Boot multi-environment configuration (Profiles) for AI Logistics Incident Reporter"
   ```

2. Tạo repository mới trên GitHub và push mã nguồn:
   ```bash
   git remote add origin https://github.com/<YOUR_USERNAME>/ai-logistics-incident-reporter.git
   git branch -M main
   git push -u origin main
   ```
