# Fruit Management dành cho Admin

## Tích hợp

Module chạy là `app`, không phải bản sao `gradle/app`. Tái sử dụng `test2.db`,
`Schema.DB_VERSION = 3`, `Theme.Starter`, `AuthManager` và FragmentManager.
Không thêm bảng/cột, không tăng phiên bản, không dùng destructive migration.
Fruit giữ id, name, category, price, quantity, expiryDate, description.
Giá dùng double theo code hiện có; số lượng int; ngày ISO yyyy-MM-dd.
`FruitEntity.id` dùng Integer để Room khớp khai báo PRIMARY KEY không kèm
NOT NULL của SQLiteOpenHelper hiện tại. Không ALTER bảng.

Luồng đọc: Fragment → ViewModel → FruitRepository → FruitDao → Room.
Repository khởi tạo database dùng chung bằng DBHelper trên ExecutorService trước
khi Room mở, để giữ các bảng Account/Student/Teacher/Score.
Luồng ghi kiểm tra lại phiên đăng nhập và Admin trong tác vụ nền.

## Phần 2 — dữ liệu, validation, hạn sử dụng

File: dal/FruitDao.java, models/FruitEntity.java, utils/FruitValidation.java,
utils/ExpiryPolicy.java, repositories/FruitRepository.java; Fruit.java chỉ bỏ BOM.
`FruitValidation.validate()` dùng chung cho form và Repository. Tên/loại sau trim
không rỗng; giá hữu hạn > 0; quantity int >= 0; ngày đúng lịch và đúng yyyy-MM-dd.
Giá nhập không có dấu phân cách hàng nghìn; nhận dấu chấm hoặc phẩy thập phân.
`ExpiryPolicy.evaluate()` phân biệt ngày trước hôm nay, hôm nay, 1–3 ngày, >3 ngày.
Không lưu trạng thái. Hôm nay theo múi giờ thiết bị; phép trừ ngày dùng UTC để
tránh ngày đổi giờ mùa hè dài/ngắn hơn 24 giờ.

Kiểm thử: ExpiryPolicyTest và FruitValidationTest; thử 0, âm, NaN, infinity,
ngày 30/02, năm nhuận, ngày thiếu số 0 và chuỗi thừa ký tự.
Trình bày: “Em kiểm tra tại giao diện để báo lỗi sớm và tại Repository để bảo vệ
đường ghi. Trạng thái hạn được suy ra từ ngày, không lưu dư thừa.”

## Phần 3 — danh sách và chi tiết

File: activities/FruitManagementActivity.java, fragments/FruitListFragment.java,
fragments/FruitDetailFragment.java, viewmodels/FruitListViewModel.java,
viewmodels/FruitDetailViewModel.java, adapters/FruitAdapter.java,
utils/ExpiryRefreshScheduler.java, layout/fragment_fruit_list.xml,
layout/fragment_fruit_detail.xml.
`getFruitRepository()` khởi tạo lười để Fragment được khôi phục sau xoay màn hình
vẫn có Repository. `observeAll()`/`observeById()` trả LiveData trạng thái.
`render()` hiển thị tải, rỗng/không tìm thấy, lỗi hoặc nội dung.
Observe bằng getViewLifecycleOwner(); binding giải phóng trong onDestroyView().
Scheduler làm mới nhãn khi sang ngày và được dừng khi màn hình không hoạt động.

Chạy thử: Admin → Quản lý trái cây → chạm một dòng → Quay lại; thử xoay máy,
background/foreground, danh sách trống và bản ghi bị xóa.
Trình bày: “ViewModel giữ dữ liệu màn hình; Fragment chỉ quan sát và render.
Truy vấn nằm trên ExecutorService, không chặn luồng giao diện.”

## Phần 4 — thêm trái cây

File mới: fragments/FruitFormFragment.java, viewmodels/FruitEditorViewModel.java,
layout/fragment_fruit_form.xml. Cập nhật FruitRepository, FruitDao và điều hướng.
`openFruitForm(0)` mở form; `FruitFormFragment.save()` parse và validate;
`FruitEditorViewModel.save()` chặn thao tác khi busy; Repository.save() kiểm tra
Admin rồi gọi DAO.insert(). Thành công thông báo và popBackStack().
Form XML có nhãn trường, trạng thái tải, lỗi và nút tải lại.

Chạy thử: nhập tên Táo, loại Nội địa, giá 45000, quantity 10, ngày hợp lệ; nhấn Lưu
liên tiếp, kiểm tra chỉ có một bản ghi; thử bỏ trống và nhập số quá lớn.
Trình bày: “Form nhận dữ liệu, ViewModel quản lý đang lưu, Repository kiểm tra
quyền và ghi nền; em không mở trực tiếp database từ Fragment.”

## Phần 5 — sửa trái cây

Dùng lại form và EditorViewModel ở phần 4; DAO.updateInformation() chỉ cập nhật
name, category, price, expiryDate, description theo id. Quantity hiển thị chỉ đọc
khi sửa, để không ghi đè tồn kho đã đổi bởi đơn hàng. Không có tính năng điều
chỉnh tồn kho độc lập trong bản này. Bản ghi không còn tồn tại được báo lỗi.

Chạy thử: mở chi tiết → Sửa thông tin → đổi tên/giá → Lưu. Kiểm tra số lượng
không thay đổi. Test crudPreservesConcurrentStock() mô phỏng quantity 10 → 7
sau khi đọc form rồi sửa tên bằng dữ liệu cũ; kết quả phải vẫn là 7.
Trình bày: “Em dùng UPDATE chọn cột thay vì ghi toàn bộ entity, nên số lượng
cũ trên form không ghi đè số lượng mới do đơn hàng cập nhật.”

## Phần 6 — xóa trái cây

File: FruitDetailFragment, FruitEditorViewModel, FruitRepository, FruitDao,
fragment_fruit_detail.xml. Dialog xác nhận → editor.delete() → repository.delete()
→ deleteById(). Kiểm tra Admin trên worker, chống thao tác lặp, báo kết quả.
Lỗi database/ràng buộc được trả về giao diện; không cascade xóa module khác.
Đường Fruit của EntityListActivity và EditRecordActivity chuyển sang màn hình
mới để không dùng CRUD tổng quát bỏ qua nghiệp vụ Fruit. Các bảng khác giữ nguyên.

Chạy thử: chọn Hủy phải giữ bản ghi; chọn Xóa phải trở lại danh sách đã cập nhật.
Test từ chối ghi khi Customer hoặc đã logout.
Trình bày: “Xác nhận chỉ là bước UI; Repository vẫn kiểm tra lại quyền trước
khi thực sự xóa. Nếu bản ghi đã mất hoặc database từ chối, em báo lỗi.”

## Phần 7 — tìm kiếm, lọc, sắp xếp

File: utils/FruitListPolicy.java, FruitListViewModel, FruitListFragment,
fragment_fruit_list.xml; test FruitValidationTest.
`setSearch()`, `setFilter()`, `setSort()` gọi publishState().
`FruitListPolicy.matches()` tìm chuỗi con theo tên hoặc loại, bỏ dấu và không
phân biệt hoa thường. Lọc: tất cả, đã hết hạn, hôm nay, 1–3 ngày, hết hàng,
tồn thấp 1–5. Không tính hôm nay vào đã hết hạn. Sắp xếp tên A–Z, giá tăng,
hạn gần nhất; cùng giá trị thì theo id. Tên dùng compareToIgnoreCase, không phải
thứ tự từ điển tiếng Việt chuyên dụng.

Chạy thử: tìm “tao” phải khớp “Táo”; tìm theo loại; đổi từng filter/sort.
Trình bày: “Danh sách nguồn lấy từ Room; ViewModel áp dụng điều kiện hiện tại
rồi phát trạng thái mới. Các nút lọc không thay đổi dữ liệu trong database.”
Giới hạn: lọc/sắp xếp trong bộ nhớ phù hợp bài sinh viên và dữ liệu nhỏ; chuyển
sang truy vấn DAO/phân trang nếu số sản phẩm tăng lớn.

## Phần 8 — mở rộng và kiểm thử

Thống kê theo kết quả đang hiển thị: số loại, tổng quantity, tổng price × quantity.
Không thêm bảng thống kê hay cột trạng thái. Ngưỡng tồn thấp cố định 1–5.
Test Kotlin cũ dùng API coroutine không tồn tại đã được thay bằng
androidTest Java FruitRepositoryTest: CRUD, mở bảng do DBHelper tạo bằng Room,
bảo toàn tồn kho, từ chối dữ liệu sai/quyền sai và chống nhấn lưu hai lần.
Test dùng ContextWrapper với tiền tố fruit_management_test_ cho database và preferences,
không xóa test2.db hoặc thay phiên đăng nhập thật.

Chạy bằng Gradle 8.9 với Gradle JDK 17 hoặc JDK 21:

```text
gradle :app:assembleDebug :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
gradle :app:lintDebug
```

Repo chưa có gradlew/gradlew.bat và wrapper jar; có thể dùng Gradle cài sẵn hoặc
Gradle task trong Android Studio. Không nâng AGP/Gradle chỉ để khắc phục JDK mặc
định 24/25. Máy kiểm tra có JDK 21 tại C:/Users/LENOVO/.jdks/jbr-21.0.11.
Báo cáo unit test: app/build/reports/tests/testDebugUnitTest/index.html.
Báo cáo thiết bị: app/build/reports/androidTests/connected/debug/index.html.

Bài chạy thử tổng hợp: đăng nhập admin/123456; thêm các sản phẩm có ngày hôm qua,
hôm nay, +1, +3, +4 ngày và quantity 0,1,5,6; kiểm tra nhãn/lọc/sort/thống kê;
sửa thông tin, hủy xóa, xác nhận xóa, xoay màn hình lúc tải/lưu; đăng nhập
customer/123456 kiểm tra không có menu quản lý. Không đổi ngày hệ thống thiết bị
đang dùng; biên ngày đã được kiểm thử với ngày cố định trong unit test.

Trình bày: “Em dùng lại database, theme và phân quyền của nhóm. Luồng có bốn
lớp rõ ràng, chỉ thêm logic thuộc Fruit. Test tập trung vào validation, biên hạn,
quyền ghi và lỗi ghi đè tồn kho. Thống kê được tính từ dữ liệu đang hiển thị.”

## Những giới hạn cần nói rõ

Chưa có source module Order để kiểm tra chính sách tham chiếu/xóa sản phẩm đã mua.
Cần thống nhất với thành viên Order trước khi kết nối dữ liệu đơn hàng thực tế.
Không hỗ trợ khôi phục tác vụ ghi sau khi tiến trình bị hệ điều hành kết thúc;
ViewModel chỉ giữ tác vụ qua thay đổi cấu hình. Cần kiểm tra lại danh sách sau
khi mở lại ứng dụng. Không tự sửa xác thực demo của module khác.

## Kết quả kiểm tra cuối — 23/10/2026

Lệnh `:app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest
:app:lintDebug` chạy thành công bằng Gradle 8.9/JDK 21.
- Build APK: thành công, app/build/outputs/apk/debug/app-debug.apk.
- 4 unit test: qua (2 expiry, 2 validation/tìm kiếm/lọc).
- 3 instrumented test: qua trên Samsung SM-A566B / Android 16.
- Lint: 0 lỗi, 113 cảnh báo toàn dự án; không tắt lint hoặc tạo baseline.
- git diff --check: qua cho các file được Git theo dõi.

Phần 2 được kiểm chứng bằng unit test và test ghi dữ liệu sai; phần 3 đã build
và kiểm tra mở Room trên database do DBHelper tạo; phần 4 có test chống lưu hai
lần; phần 5 có test tồn kho thay đổi; phần 6 có test xóa và quyền ghi; phần 7 có
test tìm không dấu và biên bộ lọc; phần 8 có lint và toàn bộ kiểm tra tổng hợp.
Chưa chạy kiểm thử tương tác giao diện tự động/thủ công đầy đủ (xoay máy, bàn
phím, TalkBack, đổi ngày qua nửa đêm), chưa kiểm tra migration với file database
thực tế của các thành viên và chưa tích hợp Order. Không coi các bước này đã qua.
