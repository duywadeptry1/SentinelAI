import json
import logging
from fastapi import APIRouter, HTTPException, Depends
from app.schemas.detection import AnalyzeRequest, AnalyzeResponse
from app.core.database import get_database
from app.services.ai_service import analyze_content_with_ai

logger = logging.getLogger("sentinel")
router = APIRouter()

@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_content(payload: AnalyzeRequest, db = Depends(get_database)):
    content = payload.content.strip()
    logger.info(f"Nhận yêu cầu phân tích từ thiết bị: {payload.device_hash}")
    
    # ⚡ BƯỚC 1: KIỂM TRA SIGNATURES TRONG MONGODB TRƯỚC (HOT PATH)
    try:
        signatures_col = db["threat_signatures"]
        
        # Quét nhanh xem nội dung tin nhắn có chứa URL/STK nằm trong danh sách đen không
        async for signature in signatures_col.find():
            val = signature["entity_value"]
            if val in content:
                logger.info(f"🎯 Hot Path Hit! Phát hiện chữ ký đen: {val}")
                return AnalyzeResponse(
                    is_risk=True,
                    risk_score=95,
                    verdict_type=signature["verdict_type"],
                    display_meta={
                        "title": "PHÁT HIỆN THỰC THỂ ĐỘC HẠI",
                        "reason": f"Nội dung chứa thông tin nằm trong danh sách đen toàn cầu: {val}",
                        "recommendation": signature["description"]
                    },
                    extracted_entities={
                        "urls": [val] if signature["entity_type"] == "URL" else [],
                        "bank_accounts": [val] if signature["entity_type"] == "BANK_ACCOUNT" else [],
                        "crypto_wallets": [val] if signature["entity_type"] == "CRYPTO_WALLET" else []
                    }
                )
    except Exception as db_err:
        logger.error(f"⚠️ Lỗi truy vấn database Hot Path: {db_err}")
        pass

    # 🧠 BƯỚC 2: KHÔNG CÓ TRONG DB -> GỌI GROQ AI SERVICE (COLD PATH)
    logger.info("Hot Path Miss. Đang chuyển tiếp yêu cầu tới Groq Cloud...")
    try:
        ai_result = await analyze_content_with_ai(content)
        return ai_result

    except Exception as e:
        logger.error(f"❌ Lỗi xử lý Groq AI: {e}")
        # Fallback an toàn nếu API Groq gặp sự cố
        return AnalyzeResponse(
            is_risk=False,
            risk_score=0,
            verdict_type="UNKNOWN",
            display_meta={
                "title": "Hệ thống bận",
                "reason": "Không thể kết nối tới máy chủ phân tích thời gian thực.",
                "recommendation": "Vui lòng không thực hiện giao dịch chuyển tiền theo tin nhắn này."
            },
            extracted_entities={"urls": [], "bank_accounts": [], "crypto_wallets": []}
        )