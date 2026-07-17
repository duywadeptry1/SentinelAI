# app/api/v1/api.py
from fastapi import APIRouter
from app.api.v1.endpoints import analyze, report, admin

api_router = APIRouter()

# Nhúng các tính năng vào router chính
api_router.include_router(analyze.router, tags=["1. Detection Engine"])
api_router.include_router(report.router, prefix="/report", tags=["2. Community Report"])
api_router.include_router(admin.router, prefix="/admin", tags=["3. Admin Board"])