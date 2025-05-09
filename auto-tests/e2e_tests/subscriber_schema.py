from pydantic import BaseModel

class SubscriberCreationData(BaseModel):
    msisdn: str
    money: int
    tariff_id_logical: int

    is_restricted: bool = False
    description: str | None = None
    name_prefix: str = "test"

    quant_s_type_id: int = 0
    quant_amount_left: int = 0
