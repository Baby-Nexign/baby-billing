from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        extra='ignore'
    )

    test_rabbitmq_host: str
    test_rabbitmq_port: int
    test_rabbitmq_user: str
    test_rabbitmq_pass: str

    test_db_host: str
    test_db_port: int
    test_db_user: str
    test_db_pass: str
    test_db_name: str

    def get_db_url(self) -> str:
        return f"postgresql://{self.test_db_user}:{self.test_db_pass}@{self.test_db_host}:{self.test_db_port}/{self.test_db_name}"

    test_cdr_exchange: str = 'cdr-exchange'
    test_cdr_routing_key: str = 'cdr-routing-key'


@lru_cache
def get_settings() -> Settings:
    return Settings()
