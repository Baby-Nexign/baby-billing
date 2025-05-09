from datetime import datetime
import math

def calculate_billed_minutes(call_start_str: str, call_end_str: str) -> int:
    start_time = datetime.fromisoformat(call_start_str)
    end_time = datetime.fromisoformat(call_end_str)
    duration_timedelta = end_time - start_time
    duration_seconds = duration_timedelta.total_seconds()
    billed_minutes = math.ceil(duration_seconds / 60.0)
    return int(billed_minutes)
