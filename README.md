# Fruit Shop Management

Ứng dụng Android quản lý cửa hàng trái cây, xây dựng bằng **Java, XML và SQLite**. Ứng dụng có hai role `Admin` và `Customer`; phân quyền cần được kiểm tra trong logic xử lý, không chỉ ẩn nút trên giao diện.

> **Trạng thái mã nguồn:** workspace hiện vẫn chứa starter `StudentManager` với các bảng `Student`, `Teacher`, `Score` và các màn hình CRUD tương ứng. README này mô tả mục tiêu, phân công và yêu cầu của đề Fruit Shop Management; cần chuyển schema, model, màn hình và nhãn ứng dụng sang nghiệp vụ Fruit Shop trước khi xem là sản phẩm hoàn chỉnh.

## Chức năng theo role

### Admin

- Đăng nhập với role `Admin`.
- Quản lý trái cây: xem danh sách, thêm, sửa, xóa và tìm kiếm.
- Xem trạng thái tồn kho và hạn sử dụng; trái cây hết hạn hoặc hết hàng không được đặt.
- Xem danh sách đơn hàng và cập nhật trạng thái theo luồng `Pending` → `Confirmed` → `Completed`.

### Customer

- Đăng nhập với role `Customer` và xem thông tin hồ sơ.
- Xem, tìm kiếm trái cây còn hạn và còn hàng.
- Đặt hàng, lưu đơn hàng ở trạng thái `Pending`, và xem lịch sử đơn của chính mình.
- Không được truy cập chức năng quản trị hoặc sửa dữ liệu trái cây.

## Phân công nhóm 4 người

| Thành viên | Phụ trách | Công việc chính | Branch gợi ý |
|---|---|---|---|
| Nguyễn Kim An | Database & Models | Chốt schema; tạo SQLite và 4 bảng; viết CRUD/truy vấn dùng chung; seed dữ liệu mẫu; bảo đảm quan hệ khóa ngoại | `feature/database` |
| Lê Quốc Bảo | Authentication & Customer | Login `Admin`/`Customer`; quản lý phiên bằng `SharedPreferences`; logout; hồ sơ Customer; kiểm tra phân quyền | `feature/auth-customer` |
| Phạm Đình Trường | Fruit Management | Giao diện danh sách/form; Admin CRUD; Customer xem/tìm kiếm; lọc, hiển thị hạn sử dụng và trạng thái tồn kho | `feature/fruit` |
| Trần Đức Lâm | Order Management & Integration | Customer đặt hàng và xem lịch sử; Admin quản lý trạng thái; Dashboard hai role; tích hợp branch và kiểm thử luồng chính | `feature/orders` |

### Người 1 — Database & Models

**Các file/module dự kiến:** `DBHelper.java`, `Schema.java`, `Account.java`, `Fruit.java`, `Customer.java`, `Order.java`.

Chốt tên bảng, cột, kiểu dữ liệu, khóa chính/khóa ngoại và quy ước ngày tháng trước khi các thành viên khác bắt đầu kết nối DB. Cung cấp API dùng chung cho nhóm, tối thiểu gồm:

- Lấy danh sách trái cây, tìm theo tên, lấy theo ID.
- Thêm, sửa, xóa trái cây.
- Tìm Customer theo `accountId`.
- Tạo đơn hàng, lấy đơn theo Customer, lấy toàn bộ đơn cho Admin.
- Cập nhật trạng thái đơn hàng.

Thống nhất chữ ký hàm và kiểu dữ liệu qua một commit sớm. Dữ liệu mẫu chỉ dùng cho demo, không chứa thông tin cá nhân thật.

### Người 2 — Authentication & Customer

**Các màn hình/module dự kiến:** `LoginActivity.java`, `AuthManager.java`, `CustomerProfileActivity.java`.

- Xác thực tài khoản và role `Admin`/`Customer`.
- Lưu trạng thái đăng nhập cần thiết bằng `SharedPreferences`; khôi phục phiên khi mở lại ứng dụng và xóa phiên khi logout.
- Điều hướng đến Dashboard phù hợp với role.
- Chặn truy cập chức năng trái quyền ở cả tầng xử lý, không chỉ ẩn nút.
- Dùng tài khoản mẫu nếu đề không yêu cầu đăng ký; nếu có đăng ký thì thống nhất cách tạo Account và Customer với Người 1.

### Người 3 — Fruit Management

**Các màn hình/module dự kiến:** `FruitListActivity.java`, `FruitFormActivity.java` và layout XML tương ứng.

- Admin được thêm, sửa, xóa và tìm kiếm trái cây.
- Customer chỉ được xem/tìm kiếm; không được sửa dữ liệu.
- Hiển thị giá, số lượng, hạn sử dụng và trạng thái `Expired` / `Out of stock`.
- Không cho đặt trái cây hết hạn hoặc có số lượng không còn.
- Kiểm tra dữ liệu hợp lệ trước khi lưu; giá phải lớn hơn 0.

### Người 4 — Order Management & Integration

**Các màn hình/module dự kiến:** `PlaceOrderActivity.java`, `OrderHistoryActivity.java`, `OrderManagementActivity.java`, `MainActivity.java`.

- Customer tạo đơn, xem lịch sử đơn của mình và không xem được đơn của Customer khác.
- Admin xem đơn và cập nhật trạng thái hợp lệ.
- Ghép các branch theo hợp đồng DB đã chốt; xử lý xung đột và kiểm thử end-to-end.
- Giữ `MainActivity`/Dashboard làm điểm điều hướng theo role, tránh để nhiều thành viên cùng sửa file này.

## Schema và quyết định về chi tiết đơn hàng

Theo phạm vi đề bài, nhóm dự kiến có bốn bảng: `Account`, `Fruit`, `Customer`, `Order`. Tên cột và ràng buộc chính xác phải theo đề/giảng viên; tối thiểu cần thống nhất các định danh và quan hệ Account–Customer, Customer–Order.

**Giới hạn quan trọng:** nếu bảng `Order` chỉ lưu `customerId`, ngày đặt, tổng tiền và trạng thái, không có `fruitId` hoặc bảng chi tiết, dữ liệu sẽ không thể biểu diễn chính xác một đơn gồm những loại trái cây nào và số lượng từng loại. Trước khi triển khai đặt hàng thực tế, hãy hỏi giảng viên có được thêm bảng `OrderDetail(orderId, fruitId, quantity, unitPrice)` hay không.

- **Nếu được phép thêm bảng:** dùng `OrderDetail` để lưu từng mặt hàng, số lượng và đơn giá tại thời điểm đặt.
- **Nếu bắt buộc đúng bốn bảng:** thống nhất cách demo đơn giản hóa với giảng viên và ghi rõ giới hạn; không mô tả ứng dụng là đã lưu chi tiết đơn hàng.

## Quy tắc nghiệp vụ thống nhất

| Điều kiện / thao tác | Kết quả mong muốn |
|---|---|
| `expiryDate < today` | Đánh dấu `Expired`, không cho đặt |
| `expiryDate = today` | Còn hạn trong ngày đó |
| `quantity <= 0` | Đánh dấu `Out of stock`, không cho đặt |
| `price <= 0` | Không cho lưu trái cây |
| Customer đặt hàng | Lưu Customer, ngày đặt, tổng tiền và trạng thái ban đầu `Pending` |
| Admin xác nhận đơn | Chuyển `Pending` → `Confirmed` |
| Admin hoàn thành đơn | Chuyển `Confirmed` → `Completed` |
| Customer xem lịch sử | Chỉ trả về đơn thuộc Customer đang đăng nhập |

So sánh ngày theo cùng định dạng/múi giờ trong toàn ứng dụng. Không cho phép chuyển trạng thái tùy ý nếu không phù hợp với luồng đã thống nhất.

## Hợp đồng tích hợp giữa các module

Trước khi code song song, Người 1 chia sẻ schema và API DB; cả nhóm thống nhất các điểm sau trong commit đầu tiên:

1. Tên cột và kiểu dữ liệu của bốn bảng; bảng `OrderDetail` có được phép hay không.
2. Giá trị role (`Admin`, `Customer`) và trạng thái đơn (`Pending`, `Confirmed`, `Completed`).
3. Định dạng ngày, đơn vị tiền tệ, quy tắc làm tròn tổng tiền.
4. Tên hàm DB, tham số, giá trị trả về và cách báo lỗi.
5. Tên `Activity`, key truyền qua `Intent`, và nơi kiểm tra quyền.

Mỗi thành viên làm trên branch riêng; không tự ý đổi API/schema đã chốt. Merge các thay đổi nhỏ, build sau mỗi lần tích hợp và ưu tiên không để nhiều người chỉnh cùng một file.

## Kế hoạch triển khai nhanh

Thời lượng dưới đây là **ước lượng tham khảo**, cần điều chỉnh theo thời gian thực tế của bài:

| Thời gian | Việc cần làm |
|---|---|
| 0–5 phút | Đọc đề; Người 1 chốt schema/API; Người 4 tạo Dashboard skeleton; Người 2 và 3 dựng khung màn hình |
| 5–30 phút | Bốn thành viên phát triển song song theo module và branch đã phân công |
| 30–45 phút | Merge lần lượt; kết nối màn hình với DB; sửa lỗi build và điều hướng |
| 45–55 phút | Test Admin CRUD, Customer xem/đặt hàng, lọc hết hạn/hết hàng và cập nhật trạng thái |
| 55–60 phút | Sửa lỗi ưu tiên cao, chạy lại luồng chính, commit và chuẩn bị demo |

**Thứ tự ưu tiên tính năng:** Login → Fruit CRUD → Customer xem trái cây → Đặt hàng → Order History → Admin cập nhật trạng thái. Tìm kiếm và kiểm tra hạn sử dụng/tồn kho nên làm cùng module Fruit, không để đến giai đoạn cuối.

## Cài đặt và chạy

Project sử dụng Java/XML, Android SDK và SQLite. Cấu hình hiện có đặt `compileSdk 35`, `minSdk 23`, Java 17 và Android Gradle Plugin 8.7.3.

1. Mở project bằng Android Studio và chờ Gradle Sync.
2. Chọn emulator hoặc thiết bị Android phù hợp rồi chạy cấu hình `app`.
3. Vì workspace hiện là starter StudentManager, hãy thay schema, model, Activity, layout, tên ứng dụng và dữ liệu mẫu theo đề Fruit Shop trước khi demo.
4. Nếu wrapper của project không đầy đủ, dùng Android Studio để tạo/khôi phục Gradle wrapper phù hợp với phiên bản Gradle của project.

### Tài khoản demo

| Username | Password | Role | Ghi chú |
|---|---|---|---|
| `admin` | `123456` | `Admin` | Đăng nhập quản trị |
| `customer` | `123456` | `Customer` | Đăng nhập khách hàng; `accountId` là `2` |

Các tài khoản demo được seed trong `AuthManager` bằng `SharedPreferences`; mật khẩu được lưu dưới dạng SHA-256 hash. Đây chỉ là dữ liệu phục vụ bài tập, không dùng thông tin hoặc mật khẩu thật. Hồ sơ Customer cần bản schema có bảng `Customer` với các cột `accountId`, `fullname`, `phone`, `email` để liên kết tài khoản demo.


