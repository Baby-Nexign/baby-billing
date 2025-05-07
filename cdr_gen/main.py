from generators.cdr_generator_correct import main as correct_cdr_generator
from generators.cdr_generator_error_garbage_date import main as error_garbage_generator
from generators.cdr_generator_error_invalid_call_type import main as error_invalid_call_generator
from generators.cdr_generator_error_invalid_msisdn import main as error_invalid_msisdn_generator
from generators.cdr_generator_error_msisdn_self_call import main as error_msisdn_self_call_generator
from generators.cdr_generator_error_start_after_end import main as error_start_after_end
from generators.cdr_generator_error_zero_duration import main as error_zero_duration_generator
from generators.cdr_generator_mixed import main as mixed_cdr_generator


if __name__ == "__main__":
    correct_cdr_generator()
    error_garbage_generator()
    error_invalid_call_generator()
    error_invalid_msisdn_generator()
    error_msisdn_self_call_generator()
    error_start_after_end()
    error_zero_duration_generator()
    mixed_cdr_generator()
