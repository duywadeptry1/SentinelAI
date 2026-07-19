# app/schemas/report.py
from pydantic import BaseModel, Field

# Người dùng gửi report lên
class ReportRequest(BaseModel):
    device_hash: str
    reported_text: str = Field(..., min_length=10)
    report_type: str = "fraud"
    user_id: str = "anonymous"
    trust_score: float = 0.5

# Server phản hồi nhanh
class ReportResponse(BaseModel):
    status: str = "success"
    message: str