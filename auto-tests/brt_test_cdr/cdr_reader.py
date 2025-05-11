import json
from typing import List, Dict, Any
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def read_cdr_file(file_path: str) -> List[Dict[str, Any]]:
    cdr_records: List[Dict[str, Any]] = []
    try:
        logger.info(f"Попытка чтения CDR файла: {file_path}")
        with open(file_path, 'r', encoding='utf-8') as f:
            for i, line in enumerate(f):
                line = line.strip()
                if not line:
                    logger.warning(f"Пропущена пустая строка {i+1} в файле {file_path}")
                    continue

                try:
                    record = json.loads(line)
                    required_keys = {"callType", "firstSubscriberMsisdn", "secondSubscriberMsisdn", "callStart", "callEnd"}
                    if not required_keys.issubset(record.keys()):
                        logger.error(f"Ошибка в строке {i+1}: Отсутствуют обязательные ключи в записи: {line}")
                        continue

                    cdr_records.append(record)
                except json.JSONDecodeError as e:
                    logger.error(f"Ошибка декодирования JSON в строке {i+1}: {line}. Ошибка: {e}")
                    continue
        logger.info(f"Успешно прочитано {len(cdr_records)} CDR записей из файла {file_path}")

    except FileNotFoundError:
        logger.error(f"Ошибка: Файл не найден по пути {file_path}")
        return []
    except Exception as e:
        logger.error(f"Неожиданная ошибка при чтении файла {file_path}: {e}")
        return []

    return cdr_records