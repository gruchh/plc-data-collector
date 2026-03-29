import yaml
from typing import Dict
from .models import Thresholds, RegisterSpec

def load_config(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)

def build_runtime_config(config: dict, line_id: str, env_topic: str = None) -> dict:
    defaults = config.get("defaults", {})
    groups = defaults.get("groups", {})
    line_profiles = config.get("line_profiles", {})
    lines = config.get("lines", {})

    if line_id not in lines:
        raise ValueError(f"Brak konfiguracji dla linii: {line_id}")

    line_cfg = lines[line_id]
    profile_name = line_cfg.get("profile")

    if profile_name not in line_profiles:
        raise ValueError(f"Nieznany profil linii {line_id}: {profile_name}")

    profile_cfg = line_profiles[profile_name]
    register_specs: Dict[str, RegisterSpec] = {}

    for group_name, group_cfg in groups.items():
        for idx in range(group_cfg["from"], group_cfg["to"] + 1):
            reg = f"D{idx}"
            thresholds = Thresholds(
                low_low=float(group_cfg["low_low"]),
                low=float(group_cfg["low"]),
                high=float(group_cfg["high"]),
                high_high=float(group_cfg["high_high"]),
            )
            register_specs[reg] = RegisterSpec(
                register=reg,
                group=group_name,
                normal_min=float(group_cfg["normal_min"]),
                normal_max=float(group_cfg["normal_max"]),
                max_step=max(0.01, round(
                    group_cfg["max_step"] * profile_cfg.get("step_multiplier", 1.0), 4
                )),
                anomaly_probability=float(group_cfg["anomaly_probability"]) * float(
                    profile_cfg.get("anomaly_multiplier", 1.0)
                ),
                thresholds=thresholds,
            )

    return {
        "topic": env_topic or defaults.get("topic", "plc.data.raw"),
        "send_interval_ms": int(profile_cfg.get("send_interval_ms", defaults.get("send_interval_ms", 1000))),
        "device_type": defaults.get("device_type", "Mitsubishi PLC Simulator"),
        "include_group_summary": bool(defaults.get("include_group_summary", True)),
        "include_register_details": bool(defaults.get("include_register_details", True)),
        "key_by_line": bool(defaults.get("key_by_line", True)),
        "plc_name": line_cfg.get("plc_name", f"MITSUBISHI_SIM_{line_id}"),
        "profile_name": profile_name,
        "seed": int(line_cfg.get("seed", 1)),
        "register_specs": register_specs,
    }