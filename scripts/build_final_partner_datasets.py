import json
import math
import zipfile
from pathlib import Path
from collections import Counter

import pandas as pd
import numpy as np


# ============================================================
# PATHS
# ============================================================

BASE = Path.home() / "ranscope_final_unseen/RANSCOPE_UNSEEN_TEST/telemetry"
FRIDA = Path.home() / "ranscope_frida_hooks/frida_events.jsonl"

OUT = Path.home() / "ranscope_final_unseen/PARTNER_DATASETS"
OUT.mkdir(parents=True, exist_ok=True)

WINDOW_MS = 5000


# ============================================================
# EXACT COMMON TRAINING SCHEMA
# ============================================================

EXTENSIONS = [
    "txt",
    "csv",
    "json",
    "pdf",
    "jpg",
    "png",
    "docx",
    "md",
    "log",
    "enc",
    "locked",
]

COMMON_COLUMNS = [
    "run_id",
    "window_start_ms",
    "window_end_ms",
    "scenario",
    "label",

    "file_event_count",
    "create_count",
    "modify_count",
    "close_write_count",
    "delete_count",
    "moved_from_count",
    "moved_to_count",

    "unique_path_count",

    "avg_file_size",
    "max_file_size",

    "avg_depth",
    "max_depth",

    "avg_entropy",
    "max_entropy",
    "entropy_observation_count",
]

COMMON_COLUMNS += [
    f"{ext}_count"
    for ext in EXTENSIONS
]


SCHEMAS = {
    "SFIM": COMMON_COLUMNS + [
        "SFIM_file_read_open_count",
        "SFIM_file_write_open_count",
        "SFIM_file_delete_count",
        "SFIM_file_rename_count",
    ],

    "MPVG": COMMON_COLUMNS + [
        "MPVG_checkSelfPermission_count",
        "MPVG_requestPermissions_count",
    ],

    "CAIT": COMMON_COLUMNS + [
        "CAIT_camera_id_list_count",
        "CAIT_camera_open_count",
    ],

    "EAP": COMMON_COLUMNS + [
        "EAP_cipher_doFinal_count",
    ],
}


FILENAMES = {
    "SFIM": "SFIM_unseen_dataset.csv",
    "MPVG": "MPVG_unseen_dataset.csv",
    "CAIT": "CAIT_unseen_dataset.csv",
    "EAP": "EAP_unseen_dataset.csv",
}


# ============================================================
# LABEL NORMALIZATION
# ============================================================

def normalize_label(label):
    if label == "ransomware_like":
        return "malicious"

    if label == "malicious":
        return "malicious"

    if label == "benign":
        return "benign"

    return label


# ============================================================
# LOAD MANIFEST
# ============================================================

def load_manifest():

    manifest_file = BASE / "run_manifest.jsonl"

    if not manifest_file.exists():
        raise FileNotFoundError(
            f"Missing manifest: {manifest_file}"
        )

    runs = {}

    with manifest_file.open() as f:

        for line in f:

            if not line.strip():
                continue

            run = json.loads(line)

            if run.get("status") != "completed":
                continue

            runs[run["run_id"]] = run

    return runs


# ============================================================
# LOAD FILEMONITOR EVENTS
# ============================================================

def load_file_events(run_id):

    path = BASE / run_id / "events.jsonl"

    if not path.exists():
        raise FileNotFoundError(
            f"Missing events.jsonl for {run_id}"
        )

    events = []

    with path.open() as f:

        for line in f:

            if not line.strip():
                continue

            events.append(json.loads(line))

    return events


# ============================================================
# LOAD ANDROID EVENTS
# ============================================================

def load_android_events(run_id):

    path = BASE / run_id / "android_events.jsonl"

    if not path.exists():
        raise FileNotFoundError(
            f"Missing android_events.jsonl for {run_id}"
        )

    events = []

    with path.open() as f:

        for line in f:

            if not line.strip():
                continue

            events.append(json.loads(line))

    return events


# ============================================================
# LOAD FRIDA EVENTS
#
# IMPORTANT:
# Frida timestamp is epoch seconds.
# Run manifest timestamps are epoch milliseconds.
# ============================================================

def load_frida_events(runs):

    if not FRIDA.exists():
        raise FileNotFoundError(
            f"Missing Frida file: {FRIDA}"
        )

    events_by_run = {
        run_id: []
        for run_id in runs
    }

    matched = 0
    unmatched = 0

    # Sort runs by start time for deterministic matching.
    ordered_runs = sorted(
        runs.values(),
        key=lambda r: r["start_timestamp_ms"]
    )

    with FRIDA.open() as f:

        for line in f:

            if not line.strip():
                continue

            try:
                event = json.loads(line)

                timestamp_ms = (
                    float(event["timestamp"]) * 1000.0
                )

            except Exception:
                continue

            matched_run = None

            for run in ordered_runs:

                if (
                    run["start_timestamp_ms"]
                    <= timestamp_ms
                    <= run["end_timestamp_ms"]
                ):
                    matched_run = run
                    break

            if matched_run is None:

                unmatched += 1

                continue

            event["_timestamp_ms"] = timestamp_ms

            events_by_run[
                matched_run["run_id"]
            ].append(event)

            matched += 1

    print()
    print("FRIDA SYNCHRONIZATION")
    print("=" * 60)
    print("Matched:", matched)
    print("Unmatched:", unmatched)

    return events_by_run


# ============================================================
# COMMON WINDOW FEATURES
# ============================================================

def safe_mean(values):

    if not values:
        return 0.0

    return float(np.mean(values))


def safe_max(values):

    if not values:
        return 0.0

    return float(np.max(values))


def build_common_row(
    run,
    file_events,
    window_start_elapsed,
    window_end_elapsed
):

    run_start = run["start_timestamp_ms"]

    rows = []

    for event in file_events:

        timestamp = event.get("timestamp_ms")

        if timestamp is None:
            continue

        elapsed = timestamp - run_start

        if (
            window_start_elapsed
            <= elapsed
            < window_end_elapsed
        ):
            rows.append(event)

    event_counts = Counter()

    paths = set()

    sizes = []
    depths = []
    entropies = []

    extension_counts = Counter()

    for event in rows:

        event_type = event.get(
            "event",
            "UNKNOWN"
        )

        event_counts[event_type] += 1

        path = event.get(
            "path",
            ""
        )

        if path:
            paths.add(path)

        size = event.get("file_size")

        if isinstance(size, (int, float)):
            sizes.append(float(size))

        depth = event.get("depth")

        if isinstance(depth, (int, float)):
            depths.append(float(depth))

        entropy = event.get("entropy")

        if isinstance(entropy, (int, float)):
            entropies.append(float(entropy))

        extension = event.get(
            "extension",
            ""
        )

        if extension:

            extension = (
                str(extension)
                .lower()
                .lstrip(".")
            )

            if extension in EXTENSIONS:
                extension_counts[extension] += 1

    row = {

        "run_id":
            run["run_id"],

        "window_start_ms":
            window_start_elapsed,

        "window_end_ms":
            window_end_elapsed,

        "scenario":
            run["scenario"],

        "label":
            normalize_label(
                run["label"]
            ),

        "file_event_count":
            len(rows),

        "create_count":
            event_counts["CREATE"],

        "modify_count":
            event_counts["MODIFY"],

        "close_write_count":
            event_counts["CLOSE_WRITE"],

        "delete_count":
            event_counts["DELETE"],

        "moved_from_count":
            event_counts["MOVED_FROM"],

        "moved_to_count":
            event_counts["MOVED_TO"],

        "unique_path_count":
            len(paths),

        "avg_file_size":
            safe_mean(sizes),

        "max_file_size":
            safe_max(sizes),

        "avg_depth":
            safe_mean(depths),

        "max_depth":
            safe_max(depths),

        "avg_entropy":
            safe_mean(entropies),

        "max_entropy":
            safe_max(entropies),

        "entropy_observation_count":
            len(entropies),
    }

    for extension in EXTENSIONS:

        row[
            f"{extension}_count"
        ] = extension_counts[
            extension
        ]

    return row


# ============================================================
# BUILD LAYER DATASETS
# ============================================================

def main():

    print("=" * 70)
    print("RANSCOPE FINAL UNSEEN DATASET BUILDER")
    print("=" * 70)

    runs = load_manifest()

    print(
        "Completed runs:",
        len(runs)
    )

    if len(runs) != 157:

        raise RuntimeError(
            f"Expected exactly 157 final runs, found {len(runs)}"
        )

    scenario_counts = Counter()

    for run in runs.values():

        scenario_counts[
            run["scenario"]
        ] += 1

    print()
    print("SCENARIOS")

    for scenario, count in scenario_counts.items():

        print(
            f"{scenario}: {count}"
        )

    # --------------------------------------------------------
    # Synchronize Frida BEFORE generating SFIM.
    # --------------------------------------------------------

    frida_by_run = load_frida_events(runs)

    # Every final run must have at least one Frida event.
    missing_frida = []

    for run_id in runs:

        if not frida_by_run[run_id]:

            missing_frida.append(
                run_id
            )

    if missing_frida:

        print()
        print(
            "ERROR: These runs have no Frida SFIM events:"
        )

        for run_id in missing_frida:
            print(run_id)

        raise RuntimeError(
            "Cannot create final SFIM dataset."
        )

    print()
    print(
        "All 157 runs have synchronized SFIM events."
    )

    datasets = {
        "SFIM": [],
        "MPVG": [],
        "CAIT": [],
        "EAP": [],
    }

    # --------------------------------------------------------
    # Process each run.
    # --------------------------------------------------------

    ordered_runs = sorted(
        runs.values(),
        key=lambda r: r["start_timestamp_ms"]
    )

    for index, run in enumerate(
        ordered_runs,
        1
    ):

        run_id = run["run_id"]

        print(
            f"[{index}/157] "
            f"{run['scenario']} "
            f"{run_id}"
        )

        file_events = load_file_events(
            run_id
        )

        android_events = load_android_events(
            run_id
        )

        frida_events = frida_by_run[
            run_id
        ]

        duration_ms = (
            run["end_timestamp_ms"]
            - run["start_timestamp_ms"]
        )

        number_of_windows = max(
            1,
            math.ceil(
                duration_ms / WINDOW_MS
            )
        )

        # ----------------------------------------------------
        # Normalize Android timestamps.
        # ----------------------------------------------------

        android_normalized = []

        for event in android_events:

            timestamp = event.get(
                "timestamp_ms"
            )

            if timestamp is None:
                continue

            event_copy = dict(event)

            event_copy["_elapsed_ms"] = (
                timestamp
                - run["start_timestamp_ms"]
            )

            android_normalized.append(
                event_copy
            )

        # ----------------------------------------------------
        # Normalize Frida timestamps.
        # ----------------------------------------------------

        frida_normalized = []

        for event in frida_events:

            timestamp = event[
                "_timestamp_ms"
            ]

            event_copy = dict(event)

            event_copy["_elapsed_ms"] = (
                timestamp
                - run["start_timestamp_ms"]
            )

            frida_normalized.append(
                event_copy
            )

        # ----------------------------------------------------
        # Windows.
        # ----------------------------------------------------

        for window_index in range(
            number_of_windows
        ):

            window_start = (
                window_index
                * WINDOW_MS
            )

            window_end = (
                window_start
                + WINDOW_MS
            )

            common = build_common_row(
                run,
                file_events,
                window_start,
                window_end
            )

            # =================================================
            # SFIM
            # =================================================

            sfim = dict(common)

            frida_window = [

                e
                for e in frida_normalized

                if (
                    window_start
                    <= e["_elapsed_ms"]
                    < window_end
                )
            ]

            sfim[
                "SFIM_file_read_open_count"
            ] = sum(
                1
                for e in frida_window
                if (
                    e.get("layer")
                    == "SFIM"
                    and
                    e.get("event")
                    == "file_read_open"
                )
            )

            sfim[
                "SFIM_file_write_open_count"
            ] = sum(
                1
                for e in frida_window
                if (
                    e.get("layer")
                    == "SFIM"
                    and
                    e.get("event")
                    == "file_write_open"
                )
            )

            sfim[
                "SFIM_file_delete_count"
            ] = sum(
                1
                for e in frida_window
                if (
                    e.get("layer")
                    == "SFIM"
                    and
                    e.get("event")
                    == "file_delete"
                )
            )

            sfim[
                "SFIM_file_rename_count"
            ] = sum(
                1
                for e in frida_window
                if (
                    e.get("layer")
                    == "SFIM"
                    and
                    e.get("event")
                    == "file_rename"
                )
            )

            datasets[
                "SFIM"
            ].append(sfim)

            # =================================================
            # Android-layer events for this window
            # =================================================

            android_window = [

                e
                for e in android_normalized

                if (
                    window_start
                    <= e["_elapsed_ms"]
                    < window_end
                )
            ]

            # =================================================
            # MPVG
            # =================================================

            mpvg = dict(common)

            mpvg[
                "MPVG_checkSelfPermission_count"
            ] = sum(
                1
                for e in android_window
                if (
                    e.get("layer")
                    == "MPVG"
                    and
                    e.get("event")
                    == "checkSelfPermission"
                )
            )

            mpvg[
                "MPVG_requestPermissions_count"
            ] = sum(
                1
                for e in android_window
                if (
                    e.get("layer")
                    == "MPVG"
                    and
                    e.get("event")
                    == "requestPermissions"
                )
            )

            datasets[
                "MPVG"
            ].append(mpvg)

            # =================================================
            # CAIT
            # =================================================

            cait = dict(common)

            cait[
                "CAIT_camera_id_list_count"
            ] = sum(
                1
                for e in android_window
                if (
                    e.get("layer")
                    == "CAIT"
                    and
                    e.get("event")
                    == "camera_id_list"
                )
            )

            cait[
                "CAIT_camera_open_count"
            ] = sum(
                1
                for e in android_window
                if (
                    e.get("layer")
                    == "CAIT"
                    and
                    e.get("event")
                    == "camera_open"
                )
            )

            datasets[
                "CAIT"
            ].append(cait)

            # =================================================
            # EAP
            # =================================================

            eap = dict(common)

            eap[
                "EAP_cipher_doFinal_count"
            ] = sum(
                1
                for e in android_window
                if (
                    e.get("layer")
                    == "EAP"
                    and
                    e.get("event")
                    == "cipher_doFinal"
                )
            )

            datasets[
                "EAP"
            ].append(eap)

    # ========================================================
    # WRITE CSVs
    # ========================================================

    print()
    print("=" * 70)
    print("WRITING PARTNER CSVs")
    print("=" * 70)

    for layer in [
        "SFIM",
        "MPVG",
        "CAIT",
        "EAP",
    ]:

        df = pd.DataFrame(
            datasets[layer]
        )

        # Exact training column order.
        df = df[
            SCHEMAS[layer]
        ]

        output = OUT / FILENAMES[
            layer
        ]

        df.to_csv(
            output,
            index=False
        )

        print()
        print(
            f"{layer}:"
        )

        print(
            "  Rows:",
            len(df)
        )

        print(
            "  Columns:",
            len(df.columns)
        )

        print(
            "  Runs:",
            df["run_id"].nunique()
        )

        print(
            "  Labels:",
            df["label"].value_counts().to_dict()
        )

        print(
            "  Duplicate run/windows:",
            df.duplicated(
                ["run_id", "window_start_ms"]
            ).sum()
        )

        print(
            "  Missing values:",
            int(df.isna().sum().sum())
        )

        layer_columns = [
            c
            for c in df.columns
            if c.startswith(
                layer + "_"
            )
        ]

        for column in layer_columns:

            print(
                f"  {column}: "
                f"sum={df[column].sum()} "
                f"nonzero="
                f"{(df[column] > 0).sum()}"
            )

    # ========================================================
    # FINAL VALIDATION
    # ========================================================

    print()
    print("=" * 70)
    print("FINAL VALIDATION")
    print("=" * 70)

    expected_columns = {
        "SFIM": 35,
        "MPVG": 33,
        "CAIT": 33,
        "EAP": 32,
    }

    validation = {}

    for layer in expected_columns:

        path = OUT / FILENAMES[
            layer
        ]

        df = pd.read_csv(path)

        validation[layer] = {
            "rows": len(df),
            "columns": len(df.columns),
            "runs": df["run_id"].nunique(),
            "duplicates": int(
                df.duplicated(
                    ["run_id", "window_start_ms"]
                ).sum()
            ),
            "missing": int(
                df.isna().sum().sum()
            ),
            "benign": int(
                (
                    df["label"]
                    == "benign"
                ).sum()
            ),
            "malicious": int(
                (
                    df["label"]
                    == "malicious"
                ).sum()
            ),
        }

        assert (
            len(df.columns)
            == expected_columns[layer]
        )

        assert (
            df["run_id"].nunique()
            == 157
        )

        assert (
            validation[layer]["duplicates"]
            == 0
        )

        assert (
            validation[layer]["missing"]
            == 0
        )

    report = (
        OUT
        / "dataset_validation.json"
    )

    report.write_text(
        json.dumps(
            validation,
            indent=2
        )
    )

    # ========================================================
    # ZIP FOR PARTNER
    # ========================================================

    zip_path = (
        OUT
        / "RANSCOPE_FINAL_UNSEEN_PARTNER_DATASETS.zip"
    )

    with zipfile.ZipFile(
        zip_path,
        "w",
        compression=zipfile.ZIP_DEFLATED
    ) as z:

        for filename in FILENAMES.values():

            path = OUT / filename

            z.write(
                path,
                arcname=filename
            )

        z.write(
            report,
            arcname=report.name
        )

    print()
    print("=" * 70)
    print("FINAL PACKAGE")
    print("=" * 70)

    print(zip_path)

    print()
    print("DONE.")


if __name__ == "__main__":
    main()
