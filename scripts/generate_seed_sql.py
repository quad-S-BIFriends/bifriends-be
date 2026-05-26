#!/usr/bin/env python3
"""Generate math_seed.sql and korean_seed.sql from JSON source files."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src" / "main" / "resources" / "db"


def escape_sql_string(value: str) -> str:
    return value.replace("'", "''")


def concept_from(content: dict, subject: str) -> str:
    if subject == "korean":
        concepts = content.get("unit_concept", [])
        return ", ".join(concepts)
    return content.get("concept", "")


def build_insert(table: str, grade: int, step_number: int, content: dict, subject: str) -> str:
    title = escape_sql_string(content["step_title"])
    concept = escape_sql_string(concept_from(content, subject))
    json_text = escape_sql_string(json.dumps(content, ensure_ascii=False, separators=(",", ":")))
    return (
        f"INSERT INTO {table} (grade, step_number, step_title, concept, content_json) VALUES\n"
        f"({grade}, {step_number}, '{title}', '{concept}', '{json_text}'::jsonb)\n"
        f"ON CONFLICT (grade, step_number) DO NOTHING;"
    )


def generate_seed_sql(subject: str, table: str) -> str:
    folder = RESOURCES / subject
    lines = [
        f"-- Auto-generated from db/{subject}/*.json — run scripts/generate_seed_sql.py to regenerate",
        "",
    ]
    for grade in range(3, 7):
        for step in range(1, 4):
            path = folder / f"grade{grade}_step{step}.json"
            with path.open(encoding="utf-8") as f:
                content = json.load(f)
            lines.append(build_insert(table, grade, step, content, subject))
            lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> None:
    math_sql = generate_seed_sql("math", "math_step")
    korean_sql = generate_seed_sql("korean", "korean_step")
    (RESOURCES / "math_seed.sql").write_text(math_sql, encoding="utf-8")
    (RESOURCES / "korean_seed.sql").write_text(korean_sql, encoding="utf-8")
    print(f"Wrote {RESOURCES / 'math_seed.sql'} ({len(math_sql):,} bytes)")
    print(f"Wrote {RESOURCES / 'korean_seed.sql'} ({len(korean_sql):,} bytes)")


if __name__ == "__main__":
    main()
