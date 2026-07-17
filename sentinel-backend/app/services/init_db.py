# app/services/init_db.py
import logging
from app.core.database import get_database

logger = logging.getLogger("sentinel")

async def init_db():
    db = get_database()
    col = db["threat_signatures"]
    
    # Kiểm tra xem đã có dữ liệu chưa
    count = await col.count_documents({})
    if count > 0:
        logger.info(f"Database đã có {count} chữ ký độc hại. Bỏ qua seeding.")
        return

    # Danh sách đen mẫu (Mock Data cho MVP)
    initial_signatures = [
        {
            "entity_type": "URL",
            "entity_value": "fake-vietcombank.xyz",
            "risk_score": 100,
            "verdict_type": "CREDENTIAL_PHISHING",
            "description": "Trang web giả mạo ngân hàng Vietcombank để đánh cắp mật khẩu."
        },
        {
            "entity_type": "BANK_ACCOUNT",
            "risk_score": 95,
            "entity_value": "999888777666",
            "verdict_type": "FINANCIAL_SCAM",
            "description": "Số tài khoản bị báo cáo nhiều lần trong các vụ lừa đảo chuyển tiền."
        },
        {
            "entity_type": "CRYPTO_WALLET",
            "risk_score": 98,
            "entity_value": "0xABC1234567890DEF",
            "verdict_type": "CRYPTO_SCAM",
            "description": "Ví Crypto liên quan đến các vụ lừa đảo sàn đầu tư ảo."
        }
    ]
    
    await col.insert_many(initial_signatures)
    logger.info("✅ Database đã được nạp dữ liệu mẫu thành công!")# app/services/init_db.py
import logging
from app.core.database import get_database

logger = logging.getLogger("sentinel")

async def init_db():
    db = get_database()
    col = db["threat_signatures"]
    
    # Kiểm tra xem đã có dữ liệu chưa
    count = await col.count_documents({})
    if count > 0:
        logger.info(f"Database đã có {count} chữ ký độc hại. Bỏ qua seeding.")
        return

    # Danh sách đen mẫu (Mock Data cho MVP)
    initial_signatures = [
        {
            "entity_type": "URL",
            "entity_value": "fake-vietcombank.xyz",
            "risk_score": 100,
            "verdict_type": "CREDENTIAL_PHISHING",
            "description": "Trang web giả mạo ngân hàng Vietcombank để đánh cắp mật khẩu."
        },
        {
            "entity_type": "BANK_ACCOUNT",
            "risk_score": 95,
            "entity_value": "999888777666",
            "verdict_type": "FINANCIAL_SCAM",
            "description": "Số tài khoản bị báo cáo nhiều lần trong các vụ lừa đảo chuyển tiền."
        },
        {
            "entity_type": "CRYPTO_WALLET",
            "risk_score": 98,
            "entity_value": "0xABC1234567890DEF",
            "verdict_type": "CRYPTO_SCAM",
            "description": "Ví Crypto liên quan đến các vụ lừa đảo sàn đầu tư ảo."
        }
    ]
    
    await col.insert_many(initial_signatures)
    logger.info("✅ Database đã được nạp dữ liệu mẫu thành công!")