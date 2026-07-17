from pydantic import BaseModel, Field

# 1. Lớp nhận dữ liệu từ Android gửi lên (Đây là lớp bị thiếu lúc nãy)
class AnalyzeRequest(BaseModel):
    content: str = Field(..., description="Nội dung đoạn chat, tin nhắn hoặc đường link nghi ngờ cần phân tích")
    device_hash: str = Field(..., description="Mã băm định danh thiết bị để phục vụ thống kê, chặn spam")

# 2. Các lớp định dạng dữ liệu AI bóc tách và trả về
class ExtractedEntities(BaseModel):
    urls: list[str] = Field(..., description="Danh sách các đường link URL bóc tách được từ tin nhắn")
    bank_accounts: list[str] = Field(..., description="Danh sách số tài khoản ngân hàng phát hiện được")
    crypto_wallets: list[str] = Field(..., description="Danh sách địa chỉ ví tiền mã hóa (nếu có)")

class DisplayMeta(BaseModel):
    title: str = Field(..., description="Tiêu đề cảnh báo ngắn gọn (Ví dụ: Phát hiện lừa đảo, An toàn)")
    reason: str = Field(..., description="Lý do chi tiết tại sao đưa ra kết luận này")
    recommendation: str = Field(..., description="Lời khuyên cụ thể cho người dùng")

class AnalyzeResponse(BaseModel):
    is_risk: bool = Field(..., description="True nếu nội dung có dấu hiệu lừa đảo, ngược lại là False")
    risk_score: int = Field(..., description="Thang điểm rủi ro từ 0 đến 100")
    verdict_type: str = Field(..., description="Phân loại lừa đảo: SAFE, PHISHING, CRYPTO_SCAM, IMPERSONATION, UNKNOWN")
    display_meta: DisplayMeta
    extracted_entities: ExtractedEntities