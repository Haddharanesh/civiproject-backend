from pathlib import Path

from pydantic_settings import BaseSettings

BASE_DIR = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    database_url: str = f"sqlite:///{BASE_DIR / 'civic.db'}"
    jwt_secret: str = "mysecretkeymysecretkeymysecretkey"
    jwt_expire_hours: int = 24
    upload_dir: Path = BASE_DIR / "uploads"
    max_upload_mb: int = 10
    # Duplicate detection (configurable add-on)
    duplicate_radius_meters: float = 100.0
    duplicate_text_similarity_threshold: float = 0.35
    duplicate_check_enabled: bool = True

    class Config:
        env_file = BASE_DIR / ".env"


settings = Settings()
settings.upload_dir.mkdir(parents=True, exist_ok=True)
