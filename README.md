🛡️ HƯỚNG DẪN CÀI ĐẶT VÀ THỬ NGHIỆM SENTINEL AI
Thực hiện bởi: Phạm Minh Duy - Sinh viên Đại học Nguyễn Tất Thành.

Tài liệu này hướng dẫn Ban Giám Khảo các bước nhanh nhất để khởi chạy hệ thống và trải nghiệm trực tiếp các tính năng của Sentinel AI - Hệ thống phân tầng phát hiện lừa đảo trực tuyến.

⚙️ YÊU CẦU HỆ THỐNG (PREREQUISITES)
Để chạy toàn bộ hệ thống (Kiến trúc phân tầng), Ban Giám Khảo cần chuẩn bị:

Môi trường Backend: Python 3.8 trở lên.

Môi trường Mobile: Điện thoại Android vật lý (Android 8.0+) hoặc Máy ảo Android (Emulator) có hỗ trợ nhận tin nhắn SMS.

Cấp quyền ứng dụng: Yêu cầu quyền đọc SMS và quyền hiển thị trên các ứng dụng khác (Overlay).

🚀 PHẦN 1: KHỞI ĐỘNG BACKEND AI (FASTAPI)
Tầng Backend chịu trách nhiệm phân tích ngữ cảnh chuyên sâu (Context Analysis) thông qua mô hình AI.

Bước 1: Mở Terminal/Command Prompt tại thư mục backend/.
Bước 2: Cài đặt các thư viện cần thiết:

Bash
pip install -r requirements.txt
Bước 3: Khởi chạy Server cục bộ:

Bash
uvicorn app.main:app --reload
(Server sẽ mặc định chạy tại địa chỉ: [http://127.0.0.1:8000](http://127.0.0.1:8000). Đảm bảo file dữ liệu huấn luyện training_data.json đã nằm trong thư mục data/)

📱 PHẦN 2: CÀI ĐẶT ỨNG DỤNG ANDROID (CLIENT)
Tầng Local hoạt động ngầm trên thiết bị, sử dụng bộ luật (keywords.csv trong thư mục assets) để chấm điểm rủi ro siêu tốc.

Bước 1: Mở thư mục sentinel-android/ bằng Android Studio.
Bước 2: Kết nối thiết bị Android hoặc khởi động máy ảo (Emulator).
Bước 3: Bấm nút Run (Shift + F10) để cài đặt ứng dụng lên thiết bị.
Bước 4 (Quan trọng): Ở lần mở đầu tiên, ứng dụng sẽ yêu cầu 2 quyền thiết yếu. Vui lòng bấm Chấp nhận:

Quyền truy cập SMS (Để quét nội dung tin nhắn đến).

Quyền hiển thị trên ứng dụng khác (Draw over other apps - Để hiển thị popup cảnh báo đỏ).

🎯 PHẦN 3: KỊCH BẢN THỬ NGHIỆM (TEST CASES)
Đây là phần trình diễn luồng hoạt động thực tế của Sentinel AI. Giám khảo vui lòng sử dụng một thiết bị khác (hoặc trình giả lập SMS trên máy ảo) để gửi tin nhắn đến thiết bị đang cài đặt Sentinel AI.

Kịch bản 1: Đánh giá tốc độ Local Scan (Chế độ Offline)
Mục tiêu: Kiểm tra khả năng phát hiện siêu tốc (<50ms) không cần Internet.

Thao tác: Tắt Wifi/4G trên thiết bị cài Sentinel AI. Gửi một tin nhắn chứa từ khóa rủi ro cao.

Nội dung SMS test: "Bạn đã trúng thưởng 1 chiếc iPhone 15. Vui lòng chuyển khoản phí nhận thưởng vào STK 123456."

Kết quả mong đợi: Ngay khi tin nhắn đến, màn hình Overlay màu đỏ lập tức bật lên cảnh báo nguy hiểm trước khi người dùng kịp mở app tin nhắn.

Kịch bản 2: Đánh giá khả năng phân tích ngữ cảnh (AI Backend)
Mục tiêu: Kiểm tra khả năng phân biệt ngữ cảnh của AI để tránh báo cáo nhầm (False Positive).

Thao tác: Bật Wifi/4G. Gửi 2 tin nhắn khác nhau nhưng có chung từ khóa (VD: "chuyển khoản").

SMS 1 (Bình thường): "Ê chiều nay đi ăn nhớ chuyển khoản tiền bún bò cho tao nha." -> Kết quả: Hệ thống bỏ qua, không cảnh báo.

SMS 2 (Lừa đảo tinh vi): "Tai khoan Ngan Hang cua ban bi tam khoa. Vui long truy cap duong link http://fake-bank-login.com de xac thuc và chuyen khoan phi mo khoa." -> Kết quả: AI nhận diện ngữ cảnh đe dọa/giả mạo, gửi lệnh kích hoạt màn hình cảnh báo đỏ.

Kịch bản 3: Tình báo cộng đồng (Report)
Thao tác: Mở ứng dụng Sentinel AI, chọn tab Lịch sử quét.

Bấm vào một tin nhắn bất kỳ và chọn "Báo cáo mã độc mới".

Kết quả: Hệ thống ghi nhận luồng dữ liệu gửi về Backend để bổ sung vào tập dữ liệu học tập (Dataset).

Cần hỗ trợ kỹ thuật trong quá trình chấm thi?
Vui lòng liên hệ: Phạm Minh Duy (SĐT: ... | Email: ...)
