import random
from datetime import datetime, timezone
from typing import Dict, List, Tuple

from .models import (
    RegisterState, Severity, Thresholds, RegisterSpec,
    RegisterReading, GroupSummary, PlcPayload
)

def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

def clamp(value: int, min_value: int, max_value: int) -> int:
    return max(min_value, min(max_value, value))

def worst_state(states: List[RegisterState]) -> RegisterState:
    order = {
        RegisterState.NORMAL: 0,
        RegisterState.LOW: 1,
        RegisterState.HIGH: 1,
        RegisterState.LOW_LOW: 2,
        RegisterState.HIGH_HIGH: 2,
    }
    return max(states, key=lambda s: order[s])

def state_to_severity(state: RegisterState) -> Severity:
    if state in (RegisterState.LOW_LOW, RegisterState.HIGH_HIGH):
        return Severity.ALARM
    if state in (RegisterState.LOW, RegisterState.HIGH):
        return Severity.WARNING
    return Severity.INFO

def classify_value(value: int, thresholds: Thresholds) -> Tuple[RegisterState, Severity, bool, bool]:
    if value <= thresholds.low_low:
        state = RegisterState.LOW_LOW
    elif value < thresholds.low:
        state = RegisterState.LOW
    elif value >= thresholds.high_high:
        state = RegisterState.HIGH_HIGH
    elif value > thresholds.high:
        state = RegisterState.HIGH
    else:
        state = RegisterState.NORMAL

    severity = state_to_severity(state)
    is_alarm = state in (RegisterState.LOW_LOW, RegisterState.HIGH_HIGH)
    is_suspicious = state in (RegisterState.LOW, RegisterState.HIGH)
    return state, severity, is_alarm, is_suspicious

def initialize_state(register_specs: Dict[str, RegisterSpec], rng: random.Random) -> Dict[str, int]:
    state = {}
    for reg, spec in register_specs.items():
        mid = (spec.normal_min + spec.normal_max) // 2
        jitter = rng.randint(-spec.max_step, spec.max_step)
        state[reg] = clamp(mid + jitter, spec.normal_min, spec.normal_max)
    return state

def make_spike(spec: RegisterSpec, rng: random.Random) -> int:
    normal_span = spec.normal_max - spec.normal_min
    extra = max(spec.max_step * 3, int(normal_span * rng.uniform(0.15, 0.35)))
    if rng.random() < 0.5:
        return spec.thresholds.low_low - extra
    return spec.thresholds.high_high + extra

def next_value(current_value: int, spec: RegisterSpec, rng: random.Random) -> int:
    is_anomaly = rng.random() < spec.anomaly_probability
    if is_anomaly:
        return make_spike(spec, rng)
    step = rng.randint(-spec.max_step, spec.max_step)
    return clamp(current_value + step, spec.normal_min, spec.normal_max)

def build_register_readings(register_values: Dict[str, int], register_specs: Dict[str, RegisterSpec]) -> Dict[str, RegisterReading]:
    result: Dict[str, RegisterReading] = {}
    for reg in sorted(register_values.keys(), key=lambda x: int(x[1:])):
        value = register_values[reg]
        spec = register_specs[reg]
        state, severity, is_alarm, is_suspicious = classify_value(value, spec.thresholds)
        result[reg] = RegisterReading(
            value=value,
            state=state.value,
            severity=severity.value,
            isAlarm=is_alarm,
            isSuspicious=is_suspicious,
            group=spec.group,
        )
    return result

def build_group_summary(registers: Dict[str, RegisterReading]) -> Dict[str, GroupSummary]:
    grouped: Dict[str, List[Tuple[str, RegisterReading]]] = {}
    for reg, reading in registers.items():
        grouped.setdefault(reading.group, []).append((reg, reading))

    summary: Dict[str, GroupSummary] = {}
    for group_name, items in grouped.items():
        counts = {
            RegisterState.LOW_LOW.value: 0,
            RegisterState.LOW.value: 0,
            RegisterState.NORMAL.value: 0,
            RegisterState.HIGH.value: 0,
            RegisterState.HIGH_HIGH.value: 0,
        }
        alarms =[]
        suspicious = []
        states_for_group: List[RegisterState] =[]

        for reg, reading in items:
            counts[reading.state] += 1
            states_for_group.append(RegisterState(reading.state))
            if reading.isAlarm:
                alarms.append(reg)
            elif reading.isSuspicious:
                suspicious.append(reg)

        worst = worst_state(states_for_group)
        severity = state_to_severity(worst)
        summary[group_name] = GroupSummary(
            status=worst.value,
            severity=severity.value,
            counts=counts,
            alarms=alarms,
            suspicious=suspicious,
        )
    return summary

def build_payload(line_id: str, runtime: dict, register_values: Dict[str, int]) -> PlcPayload:
    register_readings = build_register_readings(register_values, runtime["register_specs"])
    group_summary = build_group_summary(register_readings)
    return PlcPayload(
        timestamp=utc_now_iso(),
        line=line_id,
        plcName=runtime["plc_name"],
        deviceType=runtime["device_type"],
        profile=runtime["profile_name"],
        registers=register_readings,
        groupSummary=group_summary,
    )