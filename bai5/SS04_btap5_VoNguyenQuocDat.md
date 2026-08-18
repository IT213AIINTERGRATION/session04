# BÀI 5: Sáng tạo (Thiết kế Quy trình & Prompt Kiểm tra Giao dịch đáng ngờ - Fraud Detector)

## 1. Mô tả ý đồ thiết kế quy trình

Quy trình được chia thành hai bước nhằm nâng cao chất lượng mã nguồn. Ở bước đầu, AI đóng vai trò lập trình viên để sinh mã nguồn đáp ứng đúng yêu cầu nghiệp vụ. Ở bước thứ hai, AI đóng vai trò chuyên gia tối ưu thuật toán để kiểm tra hiệu năng, phát hiện điểm nghẽn và đề xuất thuật toán tốt hơn. Cách tiếp cận này giúp vừa đảm bảo tính đúng đắn của nghiệp vụ, vừa cải thiện hiệu năng khi xử lý lượng dữ liệu lớn.

---

## 2. Bước 1 - Prompt sinh mã nguồn

```text
Hãy đóng vai trò là một Java Senior Developer.

Viết lớp FraudDetector với phương thức:

public List<Transaction> detectFraud(List<Transaction> transactions)

Yêu cầu:

- Gắn cờ giao dịch có số tiền lớn hơn 100000000 VND.
- Nếu hai giao dịch liên tiếp của cùng một cardId xảy ra ở hai địa điểm khác nhau trong thời gian dưới 10 phút thì cả hai giao dịch đều bị gắn cờ.
- Sử dụng Java 8 Date Time API (LocalDateTime và Duration).
- Kiểm tra các trường hợp đầu vào null, danh sách rỗng hoặc thiếu thông tin địa điểm.
- Trả về danh sách các giao dịch bị gắn cờ.
- Viết mã nguồn Java hoàn chỉnh và có chú thích.
```

---

## 3. Bước 2 - Prompt kiểm chứng hiệu năng

```text
Hãy đóng vai trò là chuyên gia tối ưu thuật toán Java.

Đây là mã nguồn FraudDetector đã được sinh ở bước trước.

Hãy phân tích độ phức tạp thời gian của thuật toán.

Giả sử hệ thống phải xử lý 10000 giao dịch, hãy chỉ ra điểm nghẽn nếu thuật toán đang sử dụng hai vòng lặp lồng nhau O(N²).

Đề xuất và viết lại thuật toán tối ưu hơn bằng cách:

- Gom nhóm giao dịch theo cardId.
- Sắp xếp theo thời gian.
- Chỉ so sánh các giao dịch liền kề.
- Đưa độ phức tạp xuống O(NlogN).

Giải thích lý do tối ưu.
```

---

# Minh chứng hội thoại

## Lượt chat 1

### Prompt

```text
Hãy viết lớp FraudDetector theo các yêu cầu ở trên.
```

### AI trả lời

```java
public class FraudDetector {

    public List<Transaction> detectFraud(List<Transaction> transactions) {

        List<Transaction> result = new ArrayList<>();

        if (transactions == null || transactions.isEmpty()) {
            return result;
        }

        for (Transaction t : transactions) {

            if (t.getAmount() > 100000000) {
                result.add(t);
            }

        }

        for (int i = 0; i < transactions.size(); i++) {

            for (int j = i + 1; j < transactions.size(); j++) {

                Transaction a = transactions.get(i);
                Transaction b = transactions.get(j);

                if (a.getCardId().equals(b.getCardId())
                        && !a.getLocation().equals(b.getLocation())) {

                    long minutes =
                            Duration.between(a.getTime(), b.getTime()).toMinutes();

                    if (Math.abs(minutes) < 10) {

                        result.add(a);
                        result.add(b);

                    }

                }

            }

        }

        return result;
    }

}
```

---

## Lượt chat 2

### Prompt

```text
Hãy kiểm tra hiệu năng của đoạn mã trên và tối ưu nếu cần.
```

### AI trả lời

```text
Thuật toán hiện tại sử dụng hai vòng lặp lồng nhau để so sánh các giao dịch.

Độ phức tạp thời gian là O(N²).

Với khoảng 10000 giao dịch sẽ phải thực hiện gần 100 triệu lần so sánh, làm giảm đáng kể hiệu năng.

Giải pháp tốt hơn là:

- Gom nhóm giao dịch theo cardId.
- Sắp xếp từng nhóm theo thời gian.
- Chỉ so sánh các giao dịch liền kề.

Độ phức tạp giảm xuống O(NlogN), phù hợp hơn với hệ thống thực tế.
```

### Mã nguồn sau khi tối ưu

```java
Map<String, List<Transaction>> groups =
        transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCardId));

for (List<Transaction> list : groups.values()) {

    list.sort(Comparator.comparing(Transaction::getTime));

    for (int i = 1; i < list.size(); i++) {

        Transaction prev = list.get(i - 1);
        Transaction curr = list.get(i);

        long minutes =
                Duration.between(prev.getTime(), curr.getTime()).toMinutes();

        if (Math.abs(minutes) < 10
                && !prev.getLocation().equals(curr.getLocation())) {

            suspicious.add(prev);
            suspicious.add(curr);

        }

    }

}
```