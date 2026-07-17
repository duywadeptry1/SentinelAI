# app/main.py
from fastapi import FastAPI
from contextlib import asynccontextmanager
from app.core.config import settings
from app.core.database import connect_to_mongo, close_mongo_connection
from app.api.v1.api import api_router
from app.services.init_db import init_db

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. Kết nối DB
    await connect_to_mongo()
    # 2. Seed dữ liệu mẫu (Chỉ chạy nếu DB rỗng)
    await init_db() 
    yield
    # 3. Shutdown
    await close_mongo_connection()

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    lifespan=lifespan # Nhúng lifespan vào đây
)

app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/")
async def root():
    return {"status": "healthy", "message": "Sentinel AI Engine connected to DB!"}