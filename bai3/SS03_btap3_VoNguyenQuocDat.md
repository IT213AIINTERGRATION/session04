# BÀI 3: Đọc hiểu & Dò lỗi qua Prompt (Phát hiện lỗi logic lặp)

## Phân tích lý do Prompt thô dễ bỏ sót lỗi

Prompt **"Mã này bị lỗi gì?"** quá chung chung nên AI thường chỉ kiểm tra lỗi cú pháp hoặc các lỗi dễ nhận thấy. Trong trường hợp này, chương trình vẫn biên dịch và chạy bình thường nên AI có thể kết luận rằng mã không có vấn đề.

Thực tế, lỗi nằm ở **logic nghiệp vụ**. Biến `j` được khởi tạo bằng `i`, khiến lần so sánh đầu tiên luôn là `arr[i] == arr[i]`, điều kiện này luôn đúng. Vì vậy, hàm luôn trả về phần tử đầu tiên của mảng, ngay cả khi mảng không có phần tử trùng lặp. Nếu không cung cấp ca kiểm thử cụ thể, AI rất dễ bỏ sót lỗi này.

## Prompt sau khi tối ưu

```text
Hãy đóng vai trò là một Code Auditor có kinh nghiệm về Java.

Phân tích đoạn mã dưới đây dưới góc độ logic nghiệp vụ, không chỉ kiểm tra lỗi cú pháp.

Hãy sử dụng ca kiểm thử sau để đánh giá chương trình:

int[] arr = {1, 2, 3, 4};

Mảng trên không có phần tử trùng lặp nhưng chương trình hiện tại vẫn trả về 1. Hãy giải thích nguyên nhân gây ra lỗi logic, phân tích độ phức tạp của thuật toán hiện tại và sửa lại chương trình bằng cách sử dụng HashSet để giảm độ phức tạp từ O(N²) xuống O(N). Trả về mã nguồn Java hoàn chỉnh.
```

## Mã nguồn Java sau khi sửa

```java
import java.util.HashSet;
import java.util.Set;

public class DuplicateFinder {

    public static Integer findDuplicate(int[] arr) {

        if (arr == null || arr.length == 0) {
            return null;
        }

        Set<Integer> visited = new HashSet<>();

        for (int value : arr) {
            if (!visited.add(value)) {
                return value;
            }
        }

        return null;
    }

    public static void main(String[] args) {

        int[] arr1 = {1, 2, 3, 4};
        int[] arr2 = {5, 8, 3, 8, 10};

        System.out.println(findDuplicate(arr1)); // null
        System.out.println(findDuplicate(arr2)); // 8
    }
}
```