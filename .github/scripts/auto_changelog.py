#!/usr/bin/env python3
"""
Automate CHANGELOG.md updates and GitHub RELEASE_NOTES.md generation
from app/build.gradle.kts version metadata and Git commit history.
Uses Python standard library only.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import os
from pathlib import Path
import re
import subprocess
import sys


CONVENTIONAL_RE = re.compile(
    r"^(?P<type>[a-zA-Z]+)(?:\((?P<scope>[^)]+)\))?(?P<breaking>!)?: \s*(?P<desc>.+)$"
)

TYPE_LABELS = {
    "feat": "Fitur Baru",
    "fix": "Perbaikan",
    "perf": "Performa",
    "refactor": "Peningkatan Kode",
    "ui": "Tampilan",
    "style": "Tampilan",
    "build": "Build & Rilis",
    "ci": "Otomatisasi CI",
    "docs": "Dokumentasi",
    "test": "Pengujian",
    "chore": "Pemeliharaan",
}

IGNORED_SUBSTRINGS = (
    "[skip ci]",
    "[ci skip]",
    "chore(changelog)",
    "merge pull request",
    "merge branch",
)


def run_git(args: list[str], cwd: Path) -> str:
    try:
        proc = subprocess.run(
            ["git", *args],
            cwd=str(cwd),
            check=False,
            capture_output=True,
            text=True,
        )
        if proc.returncode != 0:
            return ""
        return proc.stdout.strip()
    except OSError:
        return ""


def parse_gradle_version(gradle_path: Path) -> tuple[str, int]:
    content = gradle_path.read_text(encoding="utf-8")
    ver_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    ver_code_match = re.search(r"versionCode\s*=\s*(\d+)", content)
    version_name = ver_name_match.group(1).strip() if ver_name_match else "1.0"
    version_code = int(ver_code_match.group(1)) if ver_code_match else 1
    return version_name, version_code


def find_existing_section(changelog_text: str, version_name: str) -> str | None:
    lines = changelog_text.splitlines()
    header_pattern = re.compile(
        rf"^##\s+v?{re.escape(version_name)}(?:\s|\(|$)", re.IGNORECASE
    )

    start_idx: int | None = None
    for idx, line in enumerate(lines):
        if header_pattern.match(line.strip()):
            start_idx = idx
            break

    if start_idx is None:
        return None

    end_idx = len(lines)
    for idx in range(start_idx + 1, len(lines)):
        if re.match(r"^##\s+", lines[idx].strip()):
            end_idx = idx
            break

    section_lines = lines[start_idx:end_idx]
    while section_lines and section_lines[-1].strip() in ("", "---"):
        section_lines.pop()

    return "\n".join(section_lines).strip()


def find_previous_tag(repo_root: Path, current_version: str) -> str | None:
    tags_raw = run_git(["tag", "--list", "v*", "--sort=-v:refname"], repo_root)
    if not tags_raw:
        return None
    current_tag = f"v{current_version}"
    for tag in tags_raw.splitlines():
        tag = tag.strip()
        if tag and tag != current_tag:
            return tag
    return None


def collect_commit_subjects(repo_root: Path, prev_tag: str | None) -> list[str]:
    if prev_tag:
        raw = run_git(
            ["log", f"{prev_tag}..HEAD", "--no-merges", "--pretty=format:%s"],
            repo_root,
        )
    else:
        raw = run_git(
            ["log", "-n", "20", "--no-merges", "--pretty=format:%s"],
            repo_root,
        )

    if not raw:
        return []

    seen: set[str] = set()
    subjects: list[str] = []
    for line in raw.splitlines():
        msg = line.strip()
        if not msg:
            continue
        lower = msg.lower()
        if any(token in lower for token in IGNORED_SUBSTRINGS):
            continue
        if lower in seen:
            continue
        seen.add(lower)
        subjects.append(msg)
    return subjects


def format_commit_bullet(subject: str) -> tuple[str, str]:
    """Return (priority_type, formatted_markdown_bullet)."""
    match = CONVENTIONAL_RE.match(subject)
    if match:
        ctype = match.group("type").lower()
        scope = (match.group("scope") or "").strip()
        desc = match.group("desc").strip()
        label = TYPE_LABELS.get(ctype, "Pembaruan")
        if scope:
            label = f"{label} ({scope})"
        if desc:
            desc = desc[0].upper() + desc[1:]
        return ctype, f"- **{label}**: {desc}"

    cleaned = subject[0].upper() + subject[1:] if subject else "Pembaruan aplikasi"
    return "other", f"- **Pembaruan**: {cleaned}"


def build_new_section(
    version_name: str,
    version_code: int,
    subjects: list[str],
    date_str: str | None = None,
) -> str:
    today = date_str or datetime.now(timezone.utc).strftime("%d %b %Y")

    if not subjects:
        headline = f"Pembaruan & Peningkatan Stabilitas v{version_name}"
        bullets = [
            f"- **Pemeliharaan & Stabilitas**: Pembaruan build v{version_name} (Build {version_code}) dan optimalisasi performa."
        ]
    else:
        formatted = [format_commit_bullet(s) for s in subjects[:15]]
        first_desc = re.sub(r"^-\s+\*\*[^*]+\*\*:\s*", "", formatted[0][1])
        headline = first_desc if len(first_desc) <= 78 else f"Pembaruan Fitur & Perbaikan v{version_name}"
        bullets = [bullet for _, bullet in formatted]

    lines = [
        f"## v{version_name} (Build {version_code}) — {today}",
        f"**{headline}**",
        "",
        "### What's New",
        *bullets,
    ]
    return "\n".join(lines).strip()


def prepend_to_changelog(changelog_text: str, new_section: str) -> str:
    lines = changelog_text.splitlines()
    title_line = "# Changelog Eco Tobacco"

    body_start = 0
    for idx, line in enumerate(lines):
        if line.strip().startswith("# "):
            title_line = line.strip()
            body_start = idx + 1
            break

    while body_start < len(lines) and lines[body_start].strip() == "":
        body_start += 1

    existing_body = "\n".join(lines[body_start:]).strip()
    if existing_body:
        return f"{title_line}\n\n{new_section}\n\n---\n\n{existing_body}\n"
    return f"{title_line}\n\n{new_section}\n"


def build_release_notes(
    section_md: str,
    version_name: str,
    version_code: int,
    prev_tag: str | None,
) -> str:
    repo = os.environ.get("GITHUB_REPOSITORY", "").strip()
    footer_lines = [
        "",
        "---",
        f"**Package:** `EcoTobacco-v{version_name}.apk` • **Build:** `{version_code}`",
    ]
    if repo and prev_tag:
        current_tag = f"v{version_name}"
        footer_lines.append(
            f"**Full Changelog:** https://github.com/{repo}/compare/{prev_tag}...{current_tag}"
        )
    return section_md.strip() + "\n" + "\n".join(footer_lines) + "\n"


def write_github_output(changelog_updated: bool, headline: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT", "").strip()
    if not output_path:
        return
    with open(output_path, "a", encoding="utf-8") as handle:
        handle.write(f"changelog_updated={'true' if changelog_updated else 'false'}\n")
        handle.write(f"release_headline={headline}\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Auto-generate CHANGELOG.md and RELEASE_NOTES.md")
    parser.add_argument("--gradle-file", default="app/build.gradle.kts")
    parser.add_argument("--changelog", default="CHANGELOG.md")
    parser.add_argument("--release-notes", default="RELEASE_NOTES.md")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    repo_root = Path.cwd()
    gradle_path = (repo_root / args.gradle_file).resolve()
    changelog_path = (repo_root / args.changelog).resolve()
    release_notes_path = (repo_root / args.release_notes).resolve()

    if not gradle_path.exists():
        print(f"ERROR: Gradle file not found: {gradle_path}", file=sys.stderr)
        return 1

    version_name, version_code = parse_gradle_version(gradle_path)
    changelog_text = (
        changelog_path.read_text(encoding="utf-8")
        if changelog_path.exists()
        else "# Changelog Eco Tobacco\n"
    )

    prev_tag = find_previous_tag(repo_root, version_name)
    existing_section = find_existing_section(changelog_text, version_name)

    if existing_section is not None:
        section_md = existing_section
        changelog_updated = False
        print(f"Found existing CHANGELOG.md section for v{version_name}.")
    else:
        subjects = collect_commit_subjects(repo_root, prev_tag)
        section_md = build_new_section(version_name, version_code, subjects)
        updated_changelog = prepend_to_changelog(changelog_text, section_md)
        changelog_updated = True
        print(f"Generated new CHANGELOG.md section for v{version_name} from {len(subjects)} commit(s).")
        if not args.dry_run:
            changelog_path.write_text(updated_changelog, encoding="utf-8")

    release_notes_md = build_release_notes(section_md, version_name, version_code, prev_tag)
    if args.dry_run:
        print("--- DRY RUN RELEASE NOTES ---")
        print(release_notes_md)
    else:
        release_notes_path.write_text(release_notes_md, encoding="utf-8")
        print(f"Wrote release notes to {release_notes_path}.")

    headline = f"Eco Tobacco v{version_name} (Build {version_code})"
    write_github_output(changelog_updated, headline)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
