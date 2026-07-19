import httpx
import json
import logging
from app.core.config import settings

logger = logging.getLogger("sentinel")

SYSTEM_PROMPT = """
Bạn là công cụ phân tích bảo mật cấp cao của hệ thống Sentinel AI. 
Nhiệm vụ của bạn là phân tích nội dung tin nhắn được cung cấp để phát hiện các dấu hiệu lừa đảo (Phishing, Scam, Social Engineering, Đánh cắp thông tin, hoặc Lừa đảo Crypto).

Hãy trả về kết quả dưới định dạng JSON duy nhất, khớp chính xác với cấu trúc sau:
{
  "is_risk": true/false,
  "risk_score": <số nguyên từ 0 đến 100>,
  "verdict_type": "<SAFE | CRYPTO_SCAM | IMPERSONATION | CREDENTIAL_PHISHING | FINANCIAL_SCAM>",
  "display_meta": {
    "title": "<Tiêu đề cảnh báo ngắn gọn, ví dụ: 'Phát hiện lừa đảo tài sản số'>",
    "reason": "<Giải thích ngắn gọn lý do tại sao tin nhắn này nguy hiểm bằng tiếng Việt trong 1 câu>",
    "recommendation": "<Lời khuyên hành động nhanh cho nạn nhân bằng tiếng Việt trong 1 câu>"
  },
  "extracted_entities": {
    "urls": ["danh sách link nghi vấn nếu có"],
    "bank_accounts": ["danh sách STK ngân hàng nếu có"],
    "crypto_wallets": ["danh sách địa chỉ ví crypto nếu có"]
  }
}

Chú ý: Chỉ trả về chuỗi JSON thô, không bao gồm ký tự bọc markdown ```json.
"""

async def analyze_content_with_ai(content: str) -> dict:
    
    url = "https://api.groq.com" + "/openai/v1/chat/completions"
    
    headers = {
        "Authorization": f"Bearer {settings.GROQ_API_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "model": "llama-3.3-70b-versatile",  
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"Phân tích tin nhắn này: '{content}'"}
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.1,
        "max_completion_tokens": 1024 
    }
    
    async with httpx.AsyncClient(timeout=8.0) as client:
        try:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status() 
            
            result_json = response.json()
            raw_text = result_json["choices"][0]["message"]["content"]
            return json.loads(raw_text.strip())
            
        except Exception as groq_error:
            # 1. Ép nó in ra nguyên nhân thực sự từ server Groq
            error_detail = groq_error.response.text if hasattr(groq_error, 'response') else str(groq_error)
            logger.warning(f"⚠️ Groq thất bại (Lỗi: {error_detail}). Bắt đầu chuyển hướng (Fallback) sang Gemini...")
            
            try:
                # LUỒNG DỰ PHÒNG: Gọi Gemini 3.5 Flash
                gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={settings.GEMINI_API_KEY}"
                gemini_payload = {
                    "contents": [{"parts": [{"text": f"{SYSTEM_PROMPT}\n\nPhân tích tin nhắn này: '{content}'"}]}],
                    "generationConfig": {"response_mime_type": "application/json"}
                }
                
                response_gemini = await client.post(gemini_url, json=gemini_payload)
                response_gemini.raise_for_status()
                
                result_gemini = response_gemini.json()
                raw_text_gemini = result_gemini["candidates"][0]["content"]["parts"][0]["text"]
                logger.info("✅ Đã xử lý thành công bằng Gemini (Dự phòng).")
                return json.loads(raw_text_gemini.strip())
                
            except Exception as gemini_error:
                # LUỒNG CHỐT CHẶN (CẢ 2 ĐỀU SẬP): Trả về kết quả an toàn mặc định
                gemini_detail = gemini_error.response.text if hasattr(gemini_error, 'response') else str(gemini_error)
                logger.error(f"❌ Cả Groq và Gemini đều thất bại! Chi tiết lỗi Gemini: {gemini_detail}")
                
                # Giữ nguyên khối fallback cứu trợ ở dưới của ông
                return {
                    "is_risk": False,
                    "risk_score": 0,
                    "verdict_type": "SAFE",
                    "display_meta": {
                        "title": "Kiểm tra thất bại",
                        "reason": "Hệ thống AI đang bảo trì hoặc quá tải.",
                        "recommendation": "Hãy cẩn trọng với các yêu cầu chuyển tiền lạ."
                    },
                    "extracted_entities": {"urls": [], "bank_accounts": [], "crypto_wallets": []}
                }