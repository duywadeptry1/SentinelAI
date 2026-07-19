TÀI LIỆU HƯỚNG DẪN TRIỂN KHAI VÀ KIỂM THỬ SẢN PHẨM (TESTING GUIDE)
Dự án: Sentinel AI - Hệ thống phân tầng phát hiện và cảnh báo lừa đảo trực tuyến trên thiết bị di động.
Sinh viên thực hiện: Phạm Minh Duy
Đơn vị: Trường Đại học Nguyễn Tất Thành

Tài liệu này cung cấp các bước tiêu chuẩn để Hội đồng Giám khảo có thể thiết lập môi trường, khởi chạy hệ thống và đánh giá trực tiếp hiệu năng của ứng dụng Sentinel AI.

I. YÊU CẦU HỆ THỐNG VÀ MÔI TRƯỜNG
1. Môi trường Backend (Server-side)

Ngôn ngữ: Python 3.8 hoặc cao hơn.

Công cụ quản lý gói: pip.

2. Môi trường Client (Thiết bị đầu cuối)

Hệ điều hành: Android 8.0 (API Level 26) trở lên.

Thiết bị: Điện thoại Android vật lý hoặc Trình giả lập (Android Emulator) có hỗ trợ nhận/gửi SMS.

II. HƯỚNG DẪN CÀI ĐẶT VÀ KHỞI CHẠY
1. Khởi chạy Server AI Backend (FastAPI)
Tầng Backend đảm nhiệm vai trò phân tích ngữ cảnh chuyên sâu (Context Analysis) dựa trên tập dữ liệu huấn luyện.

Bước 1: Mở Terminal/Command Prompt và di chuyển vào thư mục backend/.

Bước 2: Cài đặt các thư viện phụ thuộc:

Bash
pip install -r requirements.txt
Bước 3: Khởi chạy server cục bộ:

Bash
uvicorn app.main:app --reload
Lưu ý: Hệ thống sẽ hoạt động tại địa chỉ mặc định [http://127.0.0.1:8000](http://127.0.0.1:8000). Đảm bảo file training_data.json đã tồn tại trong thư mục data/.

2. Cài đặt Ứng dụng Client (Android)
Tầng Client sử dụng kiến trúc Local Filter để chấm điểm rủi ro và phản hồi tức thời.

Bước 1: Mở thư mục sentinel-android/ bằng Android Studio.

Bước 2: Kết nối thiết bị Android hoặc khởi chạy Trình giả lập.

Bước 3: Biên dịch và chạy ứng dụng (Sử dụng tổ hợp phím Shift + F10).

Bước 4 (Thiết lập quyền): Ở lần chạy đầu tiên, ứng dụng yêu cầu cấp 2 quyền thiết yếu để hệ thống cảnh báo có thể hoạt động:

Quyền đọc SMS: Cung cấp dữ liệu đầu vào cho luồng quét.

Quyền hiển thị trên ứng dụng khác (Draw over other apps): Cho phép kích hoạt màn hình Overlay cảnh báo khi phát hiện nguy hiểm.

III. KỊCH BẢN KIỂM THỬ THỰC TẾ (TEST CASES)
Để đánh giá tính hiệu quả của kiến trúc phân tầng, Ban Giám khảo vui lòng sử dụng một thiết bị độc lập (hoặc công cụ gửi SMS của Trình giả lập) để gửi tin nhắn đến thiết bị đang cài đặt Sentinel AI.

Kịch bản 1: Kiểm thử hiệu năng Local Filter (Chế độ Offline)
Mục đích: Đánh giá tốc độ nhận diện rủi ro dựa trên tập luật (Rule-based) mà không cần kết nối mạng.

Thao tác: Ngắt kết nối Internet (Wifi/4G) trên thiết bị cài Sentinel AI. Gửi một tin nhắn chứa từ khóa rủi ro cao.

Dữ liệu đầu vào (Input): "Ban da trung thuong 1 chiec iPhone 15. Vui long chuyen khoan phi nhan thuong vao STK 123456."

Kết quả mong đợi: Hệ thống phản hồi dưới 50ms, màn hình Overlay cảnh báo nguy hiểm màu đỏ lập tức kích hoạt chặn quyền truy cập vào ứng dụng nhắn tin.

Kịch bản 2: Kiểm thử độ chính xác của AI Context Analysis (Chế độ Online)
Mục đích: Đánh giá khả năng phân tích ngữ cảnh của AI nhằm phân biệt tin nhắn an toàn và tin nhắn lừa đảo tinh vi, hạn chế tối đa tỷ lệ cảnh báo nhầm (False Positive).

Thao tác: Kích hoạt kết nối Internet. Lần lượt gửi 2 tin nhắn có chứa từ khóa nhạy cảm ("chuyển khoản") nhưng khác ngữ cảnh.

Dữ liệu đầu vào 1 (Tin nhắn an toàn): "Chiều nay đi ăn nhớ chuyển khoản tiền bún bò cho mình nha."

Kết quả mong đợi: Hệ thống phân tích an toàn, không xuất hiện cảnh báo.

Dữ liệu đầu vào 2 (Tin nhắn đe dọa/giả mạo): "Tai khoan Ngan Hang cua ban bi tam khoa. Vui long truy cap duong link http://fake-bank-login.com de xac thuc và chuyen khoan phi mo khoa."

Kết quả mong đợi: Backend AI nhận diện chính xác ngữ cảnh giả mạo và kích hoạt lệnh hiển thị màn hình cảnh báo khẩn cấp.

Kịch bản 3: Luồng báo cáo dữ liệu (Crowdsourced Threat Intelligence)
Mục đích: Đánh giá luồng thu thập dữ liệu mã độc mới từ phía người dùng để làm giàu tập dữ liệu huấn luyện.

Thao tác: Mở ứng dụng Sentinel AI, truy cập module Lịch sử quét. Chọn một tin nhắn bất kỳ và nhấn chọn "Báo cáo mã độc mới".

Kết quả mong đợi: Ứng dụng ghi nhận phản hồi, tự động đóng gói dữ liệu và gửi luồng log về Server Backend thành công.

THÔNG TIN HỖ TRỢ KỸ THUẬT
Trong trường hợp cần hỗ trợ trong quá trình biên dịch hoặc thiết lập môi trường, Ban Giám khảo vui lòng liên hệ:

Họ và tên: Phạm Minh Duy

Email: 1904duy@gmail.com

Số điện thoại: 0564705149
