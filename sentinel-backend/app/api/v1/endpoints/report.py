# app/api/v1/endpoints/report.py
import logging
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from app.core.database import get_database
from app.schemas.report import ReportRequest, ReportResponse

logger = logging.getLogger("sentinel")
router = APIRouter()

@router.post("/", response_model=ReportResponse)
async def submit_report(payload: ReportRequest, db = Depends(get_database)):
    try:
        report_col = db["community_reports"]
        
        # Tạo document báo cáo mới
        new_report = {
            "device_hash": payload.device_hash,
            "reported_text": payload.reported_text,
            "status": "PENDING", # Chờ Admin duyệt
            "created_at": datetime.utcnow()
        }
        
        await report_col.insert_one(new_report)
        logger.info(f"Đã ghi nhận báo cáo mới từ thiết bị: {payload.device_hash}")
        
        return ReportResponse(
            status="success",
            message="Cảm ơn bạn! Báo cáo đã được gửi tới hệ thống phân tích."
        )
    except Exception as e:
        logger.error(f"Lỗi khi lưu báo cáo: {e}")
        raise HTTPException(status_code=500, detail="Lỗi hệ thống khi lưu báo cáo.")