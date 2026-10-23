# Hướng dẫn demo dự án

## Chuẩn bị

Mở thư mục E:/MobileLab/Lab_Mobile trên nhánh dev. Chọn Gradle JDK 17 hoặc 21
trong Android Studio (máy kiểm tra dùng JDK 21). Không còn đường dẫn JDK cố định
C:/Users/Admin trong gradle.properties. Dùng module app, không dùng gradle/app.
Database vẫn là fruit_shop.db version 1; không đổi schema, không Clear Data.

1. Chạy app và đăng nhập admin / 123456.
2. Mở Quản lý Trái Cây → Nạp dữ liệu demo → xác nhận Nạp dữ liệu.
3. Lần đầu thêm 15 bản ghi: 10 Fruit + 1 Account + 1 Customer + 3 Order.
4. Tìm `[DEMO yyyy-MM-dd]` với ngày hôm nay trên thiết bị để xem riêng bộ mới.
   Ví dụ ngày 23/10/2026 tìm `DEMO 2026-10-23` (không cần dấu ngoặc).
5. Nạp lại cùng ngày không đặt lại tồn, giá hoặc trạng thái đơn đã chỉnh sửa.
   Bộ mới ngày khác có prefix ngày mới; bộ cũ được giữ để tránh mất dữ liệu.

Nút chỉ dành cho Admin, chạy qua ViewModel → Repository → ExecutorService →
DemoData.insert(). Ghi toàn bộ mẫu trong transaction: lỗi sẽ hoàn lại cả bộ.
Không tự nạp khi mở app và không đụng tài khoản/profile/dữ liệu thật đã có.
Nếu username demo_customer đã dùng cho Admin, nạp sẽ báo lỗi và không ghi một
phần bộ mẫu. Tài khoản demo_customer có sẵn được giữ nguyên mật khẩu/profile;
mật khẩu 123456 chỉ được đặt khi tạo mới tài khoản này.

## Bộ Fruit mẫu (D là ngày nạp)

Tất cả tên có tiền tố `[DEMO D]`. Ngày hạn lưu yyyy-MM-dd, nhãn tính khi hiển thị.

| Tên | Loại | Giá | Tồn | Hạn | Kết quả mong đợi |
|---|---|---:|---:|---|---|
| Táo hết hạn | Nhập khẩu | 120.000 | 20 | D−1 | Đã hết hạn, không mua được |
| Cam hết hạn hôm nay | Nội địa | 45.000 | 10 | D | Hết hạn hôm nay, mua được |
| Dâu còn 1 ngày | Nội địa | 180.000 | 1 | D+1 | Sắp hết hạn, tồn thấp |
| Xoài còn 2 ngày | Nội địa | 75.000 | 5 | D+2 | Sắp hết hạn, biên tồn thấp |
| Chuối còn 3 ngày | Nội địa | 35.000 | 6 | D+3 | Sắp hết hạn, không tồn thấp |
| Ổi còn 4 ngày | Nội địa | 50.000 | 6 | D+4 | Còn hạn, không sắp hết hạn |
| Nho hết hàng | Nhập khẩu | 90.000 | 0 | D+14 | Còn hạn nhưng không mua được |
| Chanh giá lẻ | Nội địa | 25.000,5 | 1 | D+14 | Giá thập phân, tồn thấp |
| Lê tồn thấp | Nhập khẩu | 60.000 | 5 | D+14 | Tồn thấp |
| Thanh long ruột đỏ loại đặc biệt có tên dài để kiểm tra giao diện | Nội địa | 99.000 | 30 | D+14 | Tên dài, mô tả trống |

Nếu tìm riêng bộ hôm nay và chưa chỉnh sửa/đặt đơn:
- Tất cả: 10 loại, 84 sản phẩm, giá trị tồn **7.210.000,5 đ**.
- Đã hết hạn: 1; hôm nay: 1; sắp hết hạn: 3; hết hàng: 1; tồn thấp: 4.
- Tìm `tao`: Táo hết hạn; tìm `noi dia`: 7 loại; tìm `nhap khau`: 3 loại.
- Sắp giá tăng: Chanh → Chuối → Cam → Ổi → Lê → Xoài → Nho → Thanh long → Táo → Dâu.
- Sắp hạn: Táo → Cam → Dâu → Xoài → Chuối → Ổi → nhóm D+14 (cùng hạn theo id).
- Tìm `khong-co-trai-cay-nay` để trình diễn trạng thái rỗng.

Lưu ý: nhận diện mẫu theo tên gốc có ngày. Nếu đổi tên hoặc xóa trái mẫu rồi nạp
lại, bản gốc được tạo lại. Không có cột seed/status phụ trong schema.

## Đăng nhập và Order

| Tài khoản | Mật khẩu mặc định | Dùng để demo |
|---|---|---|
| admin | 123456 | Fruit CRUD, nạp mẫu, quản lý trạng thái đơn |
| customer | 123456 | Khách có sẵn của dự án, giữ nguyên đơn cũ |
| demo_customer | 123456 | Hồ sơ Khách hàng Demo, đơn mẫu mới; chỉ có sau khi nạp |

Ba đơn của demo_customer trong ngày D:
- 09:00 / Pending / 240.000 đ.
- 10:00 / Confirmed / 150.000 đ.
- 11:00 / Completed / 90.000 đ.

Order hiện chỉ có tổng tiền, không có bảng dòng hàng nên các đơn mẫu là dữ liệu
minh họa trạng thái, không trừ tồn Fruit và không giả lập liên kết dòng hàng.

Luồng demo:
1. Admin lọc Pending, chuyển đơn demo Pending → Confirmed → Completed; mở lại
   để kiểm tra trạng thái đã lưu. Nạp lại mẫu không tạo lại trạng thái ban đầu.
2. Đăng xuất, đăng nhập demo_customer; xem Hồ sơ và Lịch sử đơn hàng. Chỉ thấy
   đơn liên kết đúng Customer; không thấy đơn của customer.
3. Vào Đặt mua: Táo hết hạn và Nho hết hàng phải bị khóa; Cam hôm nay vẫn mua được.
4. Chọn Dâu tồn 1, thử nhập 2 (không cho vượt tồn); mua 1 và mở lại Fruit để thấy
   Dâu về 0. Demo này làm thay đổi thống kê, nên trình bày thống kê trước khi mua.
5. Tổng tiền lúc xác nhận được kiểm tra lại. Nếu giá đổi, thiếu tồn hoặc tạo đơn
   lỗi, transaction hoàn lại toàn bộ số lượng. Việc ghi đơn chạy trên executor.

## CRUD và dữ liệu nhập sai

- Thêm một bản ghi riêng tên `Bảo vệ - Trái cây mới`, giá 45000, tồn 2 và ngày hợp lệ.
- Bỏ trống tên/loại; nhập giá 0/âm/chữ, quantity âm/thập phân/quá lớn, ngày 30/02:
  phải báo lỗi, không thêm dữ liệu bẩn vào database. Với bàn phím số, có thể dán
  chuỗi sai để thử validation. Không nạp sẵn bản ghi sai vào database.
- Nhấn Lưu liên tiếp: một bản ghi. Sửa tên/giá/hạn: giữ nguyên tồn kho hiện tại;
  trường quantity chỉ đọc trong form sửa để tránh ghi đè số lượng của Order.
- Xóa: Hủy thì còn dữ liệu, Xóa thì mất bản ghi và có thông báo.
- Đăng nhập Customer: không có menu CRUD/nạp mẫu của Admin.
- Thử xoay màn hình danh sách/chi tiết/form; kiểm tra quay lại và dữ liệu đang nhập.
- Lỗi database và phân quyền đã có test riêng; không làm hỏng file database thật
  để cố tạo màn lỗi. Trạng thái tải có thể quá nhanh để quan sát với bộ dữ liệu nhỏ.

## Sửa lỗi sau rà soát

- Workspace chính đã chuyển sang dev; không còn conflict marker trong source.
- Bỏ org.gradle.java.home gắn với máy khác; dùng Gradle JDK của IDE/JAVA_HOME.
- Bỏ onUpgrade xóa bảng: nếu chưa có migration cụ thể, báo lỗi và giữ dữ liệu.
- reduceFruitQuantity() dùng UPDATE có điều kiện, không đọc rồi ghi số cũ.
- placeOrder() kiểm tra Customer sở hữu hồ sơ, tính lại giá, transaction trừ tồn
  và tạo đơn; lỗi bất kỳ sẽ rollback. Thông báo LiveData sau khi commit.
- Ngày không hợp lệ không được coi là sản phẩm có thể đặt mua; dùng chung ExpiryPolicy.
- Không nâng version, không đổi cột hoặc cấu trúc Order/Customer.

## Trình bày khi bảo vệ

“Em nạp dữ liệu mẫu theo ngày thực tế nên các biên hết hạn không bị lỗi khi đổi
ngày bảo vệ. Mẫu có tiền tố riêng, nạp bằng quyền Admin trên luồng nền và giữ
nguyên dữ liệu cũ. Em dùng chung quy tắc ngày cho Admin và kiểm tra đặt mua.
Với đơn hàng, cập nhật tồn kho và tạo đơn nằm trong cùng transaction để khi
thất bại không mất hàng. Các ca biên và trường hợp rollback có kiểm thử thiết bị.”

## Kết quả kiểm tra — 23/10/2026

- assembleDebug: thành công.
- 4 unit test: qua.
- 7 instrumented test trên Samsung SM-A566B / Android 16: qua.
- lintDebug: 0 lỗi, 220 cảnh báo toàn dự án (không tắt lint).
- git diff --check và quét conflict marker: không phát hiện lỗi.
- Đã mở dashboard và màn Fruit trên thiết bị: thấy dữ liệu [DEMO 2026-10-23],
  nhãn hết hạn hôm nay và giá thập phân hiển thị đúng. Chưa kiểm thử thủ công
  trọn bộ luồng đặt đơn, xoay máy và TalkBack; dùng checklist ở trên để chạy tiếp.

Báo cáo: app/build/reports/tests/testDebugUnitTest/index.html và
app/build/reports/androidTests/connected/debug/index.html.
APK: app/build/outputs/apk/debug/app-debug.apk.

Hai test Kotlin cũ chưa được Git theo dõi, dùng API không còn tồn tại/trùng tên
với test Java, đã được giữ nguyên trong .gradle/legacy-fruit-tests ở máy này.
Chúng không được đưa vào source hoặc bản build Java.
