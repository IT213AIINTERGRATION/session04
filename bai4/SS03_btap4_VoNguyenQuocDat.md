# BÀI 4: Phân tích & Lựa chọn (Kỹ thuật Prompt lặp tối ưu hóa thuật toán)

## Đáp án lựa chọn

**Phương án B**

## Phân tích

Phương án B là lựa chọn tối ưu nhất vì áp dụng đúng kỹ thuật **Iterative Prompting**. Prompt chỉ rõ vấn đề của lời giải hiện tại (đệ quy có độ phức tạp thời gian rất lớn, khiến chương trình chậm khi `n = 50`), đồng thời đưa ra yêu cầu cụ thể về thuật toán cần sử dụng là **Dynamic Programming (Tabulation hoặc Memoization)**.

Ngoài ra, prompt còn đặt ra các **ràng buộc kỹ thuật** rõ ràng như:
- Giảm độ phức tạp thời gian xuống **O(N)**.
- Độ phức tạp không gian là **O(1)** hoặc **O(N)**.
- Giữ nguyên kiểu dữ liệu trả về là **long**.

Nhờ mô tả đầy đủ mục tiêu và ràng buộc, AI sẽ tạo ra lời giải phù hợp với yêu cầu tối ưu hóa thuật toán mà vẫn đảm bảo chức năng của chương trình.

## Nhược điểm của các phương án còn lại

### Phương án A

Prompt A quá chung chung, chỉ yêu cầu "viết lại bằng thuật toán khác tối ưu hơn" nhưng không chỉ rõ thuật toán nào cần sử dụng hoặc mục tiêu về độ phức tạp. AI có thể đưa ra nhiều cách cài đặt khác nhau mà không đạt được hiệu quả mong muốn hoặc không đảm bảo đúng yêu cầu của bài toán.

### Phương án C

Prompt C yêu cầu sử dụng **Java Stream API** và xử lý song song (parallel). Tuy nhiên, Stream API không phải là giải pháp để tối ưu thuật toán Fibonacci. Việc chạy song song không làm thay đổi bản chất của thuật toán đệ quy và vẫn có thể dẫn đến số lượng lời gọi hàm rất lớn. Vì vậy, cách tiếp cận này không giải quyết được nguyên nhân gốc rễ của vấn đề về hiệu năng.