from dataclasses import dataclass
from enum import Enum
from typing import Dict, List

class RegisterState(str, Enum):
    LOW_LOW = "LOW_LOW"
    LOW = "LOW"
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    HIGH_HIGH = "HIGH_HIGH"

class Severity(str, Enum):
    INFO = "INFO"
    WARNING = "WARNING"
    ALARM = "ALARM"

@dataclass
class Thresholds:
    low_low: int
    low: int
    high: int
    high_high: int

@dataclass
class RegisterSpec:
    register: str
    group: str
    normal_min: int
    normal_max: int
    max_step: int
    anomaly_probability: float
    thresholds: Thresholds

@dataclass
class RegisterReading:
    value: int
    state: str
    severity: str
    isAlarm: bool
    isSuspicious: bool
    group: str

@dataclass
class GroupSummary:
    status: str
    severity: str
    counts: Dict[str, int]
    alarms: List[str]
    suspicious: List[str]

@dataclass
class PlcPayload:
    timestamp: str
    line: str
    plcName: str
    deviceType: str
    profile: str
    registers: Dict[str, RegisterReading]
    groupSummary: Dict[str, GroupSummary]