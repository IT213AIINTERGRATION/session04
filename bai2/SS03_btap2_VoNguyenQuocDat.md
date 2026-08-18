# BÀI 2: Tối ưu Prompt (Thực hành viết Prompt giải nghĩa Stack Trace và gỡ lỗi)

## Prompt sau khi tối ưu

```text
Hãy đóng vai trò là một Java Debugger có nhiều kinh nghiệm.

Tôi đang gặp lỗi NullPointerException trong chương trình Java dưới đây. Hãy phân tích dựa trên mã nguồn và Stack Trace được cung cấp, xác định nguyên nhân gốc rễ (Root Cause) của lỗi, giải thích vì sao lỗi xảy ra và đề xuất cách sửa an toàn nhất. Không thay đổi thiết kế hướng đối tượng của lớp, không phá vỡ tính bao đóng (Encapsulation), hãy khởi tạo danh sách bằng ArrayList trong lớp UserManager và trả về mã nguồn Java hoàn chỉnh.

Mã nguồn:

import java.util.List;

public class UserManager {

    private List<String> users;

    public void addUser(String user) {
        users.add(user);
    }
}

Stack Trace:

Exception in thread "main" java.lang.NullPointerException:
Cannot invoke "java.util.List.add(Object)" because "this.users" is null
    at UserManager.addUser(UserManager.java:7)
    at Main.main(Main.java:6)
```

## Đoạn mã nguồn Java do AI sinh ra

```java
import java.util.ArrayList;
import java.util.List;

public class UserManager {

    private final List<String> users;

    public UserManager() {
        this.users = new ArrayList<>();
    }

    public void addUser(String user) {
        users.add(user);
    }

    public List<String> getUsers() {
        return new ArrayList<>(users);
    }
}
```