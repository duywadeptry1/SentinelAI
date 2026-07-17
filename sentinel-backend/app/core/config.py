from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "Sentinel AI Backend"
    API_V1_STR: str = "/api/v1"
    
    # Security Key cho Client và Admin Board
    STATIC_API_KEY: str
    ADMIN_SECRET_TOKEN: str
    
    # Database (Kết nối thẳng lên MongoDB Atlas Cloud)
    MONGODB_URI: str
    DB_NAME: str = "sentinel_db"
    
    # AI Engine Key (Sử dụng Groq API)
    GROQ_API_KEY: str

    model_config = SettingsConfigDict(
        env_file=".env", 
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore"
    )

settings = Settings()