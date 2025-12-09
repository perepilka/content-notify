from functools import lru_cache

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    bot_token: SecretStr
    core_api_url: str = "http://localhost:8080/api/v1"
    internal_service_key: SecretStr | None = None
    webhook_port: int = 8081

    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        env_prefix="",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
