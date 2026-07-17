# app/api/v1/endpoints/admin.py
import logging
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Header
from pydantic import BaseModel
from typing import List
from bson import ObjectId
from app.core.database import get_database
from app.core.config import settings

logger = logging.getLogger("sentinel")
router = APIRouter()

# Schema dữ liệu Admin truyền vào để duyệt báo cáo
class AdminVerifyRequest(BaseModel):
    verdict_type: str  # Ví dụ: "CREDENTIAL_PHISHING", "FINANCIAL_SCAM"
    description: str   # Mô tả lý do lừa đảo
    extracted_urls: List[str] = []
    extracted_banks: List[str] = []

# Hàm check Header bảo mật của Admin
def verify_admin_token(x_admin_token: str = Header(...)):
    if x_admin_token != settings.ADMIN_SECRET_TOKEN:
        raise HTTPException(status_code=403, detail="Sai token quản trị viên!")
    return x_admin_token

@router.put("/reports/{report_id}/verify")
async def verify_report(
    report_id: str, 
    payload: AdminVerifyRequest, 
    db = Depends(get_database),
    token: str = Depends(verify_admin_token) # Bảo vệ API này
):
    try:
        # 1. Đổi trạng thái báo cáo thành VERIFIED
        report_col = db["community_reports"]
        result = await report_col.update_one(
            {"_id": ObjectId(report_id)},
            {"$set": {"status": "VERIFIED", "updated_at": datetime.utcnow()}}
        )
        
        if result.matched_count == 0:
            raise HTTPException(status_code=404, detail="Không tìm thấy báo cáo này.")

        # 2. TỰ ĐỘNG ĐỒNG BỘ: Chèn các thực thể Admin vừa xác nhận vào Blacklist (threat_signatures)
        sig_col = db["threat_signatures"]
        new_signatures = []
        
        # Nạp URL lừa đảo
        for url in payload.extracted_urls:
            new_signatures.append({
                "entity_type": "URL", "entity_value": url,
                "risk_score": 95, "verdict_type": payload.verdict_type,
                "description": payload.description, "created_at": datetime.utcnow()
            })
            
        # Nạp STK lừa đảo
        for bank in payload.extracted_banks:
            new_signatures.append({
                "entity_type": "BANK_ACCOUNT", "entity_value": bank,
                "risk_score": 95, "verdict_type": payload.verdict_type,
                "description": payload.description, "created_at": datetime.utcnow()
            })

        # Insert hàng loạt vào Database nếu có dữ liệu
        if new_signatures:
            await sig_col.insert_many(new_signatures)
            logger.info(f"Auto-Sync: Đã thêm {len(new_signatures)} chữ ký đen vào Hot Path!")

        return {"status": "success", "message": "Đã duyệt và đồng bộ danh sách đen thành công!"}

    except Exception as e:
        logger.error(f"Lỗi Admin Verify: {e}")
        raise HTTPException(status_code=500, detail=str(e))