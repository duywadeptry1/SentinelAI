# app/schemas/report.py
from pydantic import BaseModel, Field

# Người dùng gửi report lên
class ReportRequest(BaseModel):
    device_hash: str
    reported_text: str = Field(..., min_length=10)

# Server phản hồi nhanh
class ReportResponse(BaseModel):
    status: str = "success"
    message: str