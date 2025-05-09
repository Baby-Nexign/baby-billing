from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        extra='ignore'
    )

    rabbitmq_host: str
    rabbitmq_port: int
    rabbitmq_user: str
    rabbitmq_pass: str

    brt_db_host: str
    brt_db_port: int
    brt_db_user: str
    brt_db_pass: str
    brt_db_name: str

    hrs_db_host: str
    hrs_db_port: int
    hrs_db_user: str
    hrs_db_pass: str
    hrs_db_name: str

    def get_brt_db_url(self) -> str:
        return f"postgresql://{self.brt_db_user}:{self.brt_db_pass}@{self.brt_db_host}:{self.brt_db_port}/{self.brt_db_name}"

    def get_hrs_db_url(self) -> str:
        return f"postgresql://{self.hrs_db_user}:{self.hrs_db_pass}@{self.hrs_db_host}:{self.hrs_db_port}/{self.hrs_db_name}"


@lru_cache
def get_settings() -> Settings:
    return Settings()
