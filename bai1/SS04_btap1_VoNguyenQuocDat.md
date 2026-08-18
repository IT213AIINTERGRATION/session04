# BÀI 1: Phân tích & Lựa chọn (Thực hành thiết kế prompt tối ưu hóa mã nguồn)

## Đáp án lựa chọn

**Phương án B**

## Phân tích

Phương án B là lựa chọn tối ưu nhất vì đáp ứng đầy đủ **5 thành phần của một Prompt hiệu quả**:

1. **Vai trò (Role):** AI được yêu cầu đóng vai một **Java Senior Developer**, giúp định hướng cách trả lời theo kinh nghiệm của một lập trình viên chuyên nghiệp.

2. **Mục tiêu (Task):** Xác định rõ nhiệm vụ là **tái cấu trúc (refactor)** lớp `DiscountService` bằng cách chuyển các điều kiện lồng nhau sang **guard clauses (return sớm)**.

3. **Ngữ cảnh (Context):** Prompt cung cấp bối cảnh cụ thể là xử lý logic rẽ nhánh phức tạp trong lớp `DiscountService`, giúp AI hiểu đúng mục đích của việc refactor.

4. **Ràng buộc (Constraints):** Yêu cầu giữ nguyên logic nghiệp vụ, không thay đổi kiểu dữ liệu đầu vào và đầu ra, đồng thời sử dụng **Java 11**. Các ràng buộc này giúp tránh làm thay đổi chức năng của chương trình.

5. **Định dạng đầu ra (Output Format):** Prompt yêu cầu AI trả về **mã nguồn Java hoàn chỉnh** kèm **giải thích ngắn bằng tiếng Việt**, giúp người dùng dễ đọc, kiểm tra và áp dụng.

Nhờ có đầy đủ các thành phần trên, Prompt B giúp AI tạo ra kết quả chính xác, đúng yêu cầu và giảm nguy cơ phát sinh lỗi trong quá trình tái cấu trúc.

## Phân tích các phương án còn lại

### Phương án A

Prompt A chỉ yêu cầu "tái cấu trúc code cho đẹp hơn", không nêu rõ mục tiêu, ràng buộc hay định dạng đầu ra. AI có thể hiểu theo nhiều cách khác nhau như đổi tên biến, thay đổi cách trình bày hoặc áp dụng nhiều kỹ thuật khác nhau mà không đảm bảo sử dụng guard clauses hay giữ nguyên logic nghiệp vụ.

### Phương án C

Prompt C yêu cầu sử dụng **Java Stream API** để thay thế các câu lệnh `if-else`. Tuy nhiên, Stream API được thiết kế chủ yếu để xử lý tập hợp dữ liệu (Collection, List, Set...), không phù hợp với bài toán điều khiển luồng bằng nhiều điều kiện rẽ nhánh. Việc ép sử dụng Stream API có thể làm mã nguồn khó hiểu hơn, giảm khả năng bảo trì và thậm chí làm thay đổi logic nghiệp vụ ban đầu.