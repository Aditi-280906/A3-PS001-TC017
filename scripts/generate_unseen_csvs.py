import json
from pathlib import Path
from collections import Counter
import pandas as pd
import numpy as np

BASE = Path.home() / "ranscope_final_unseen/RANSCOPE_UNSEEN_TEST/telemetry"
OUT = Path.home() / "ranscope_final_unseen/CSV"
OUT.mkdir(parents=True, exist_ok=True)

WINDOW_MS = 5000

EXTENSIONS = [
    "txt", "csv", "json", "pdf", "jpg",
    "png", "docx", "md", "log", "enc", "locked"
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

for e in EXTENSIONS:
    COMMON_COLUMNS.append(f"{e}_count")


def load_manifest():
    result = {}

    with (BASE / "run_manifest.jsonl").open() as f:
        for line in f:
            if not line.strip():
                continue

            r = json.loads(line)

            if r.get("status") != "completed":
                continue

            result[r["run_id"]] = r

    return result


def safe_mean(values):
    return float(np.mean(values)) if values else 0.0


def safe_max(values):
    return float(np.max(values)) if values else 0.0


def build_common_row(run, events, start_ms, end_ms):

    rows = [
        e for e in events
        if start_ms <= e["timestamp_ms"] < end_ms
    ]

    event_counts = Counter()
    paths = set()

    sizes = []
    depths = []
    entropies = []

    extensions = Counter()

    for e in rows:

        event = e.get("event", "UNKNOWN")
        event_counts[event] += 1

        path = e.get("path", "")
        if path:
            paths.add(path)

        size = e.get("file_size")
        if isinstance(size, (int, float)):
            sizes.append(float(size))

        depth = e.get("depth")
        if isinstance(depth, (int, float)):
            depths.append(float(depth))

        entropy = e.get("entropy")
        if isinstance(entropy, (int, float)):
            entropies.append(float(entropy))

        ext = e.get("extension", "")

        if ext:
            ext = ext.lower().lstrip(".")

            if ext in EXTENSIONS:
                extensions[ext] += 1

    row = {
        "run_id": run["run_id"],
        "window_start_ms": start_ms,
        "window_end_ms": end_ms,
        "scenario": run["scenario"],
        "label": run["label"],

        "file_event_count": len(rows),

        "create_count": event_counts["CREATE"],
        "modify_count": event_counts["MODIFY"],
        "close_write_count": event_counts["CLOSE_WRITE"],
        "delete_count": event_counts["DELETE"],
        "moved_from_count": event_counts["MOVED_FROM"],
        "moved_to_count": event_counts["MOVED_TO"],

        "unique_path_count": len(paths),

        "avg_file_size": safe_mean(sizes),
        "max_file_size": safe_max(sizes),

        "avg_depth": safe_mean(depths),
        "max_depth": safe_max(depths),

        "avg_entropy": safe_mean(entropies),
        "max_entropy": safe_max(entropies),

        "entropy_observation_count": len(entropies),
    }

    for ext in EXTENSIONS:
        row[f"{ext}_count"] = extensions[ext]

    return row


def load_android_events(run_id):

    path = BASE / run_id / "android_events.jsonl"

    events = []

    if not path.exists():
        return events

    with path.open() as f:
        for line in f:
            if line.strip():
                events.append(json.loads(line))

    return events


def load_file_events(run_id):

    path = BASE / run_id / "events.jsonl"

    events = []

    if not path.exists():
        return events

    with path.open() as f:
        for line in f:
            if line.strip():
                events.append(json.loads(line))

    return events


def main():

    manifest = load_manifest()

    print("=" * 60)
    print("RANSCOPE UNSEEN CSV GENERATOR")
    print("=" * 60)
    print("Runs:", len(manifest))

    print()

    run_rows = {
        "SFIM": [],
        "MPVG": [],
        "CAIT": [],
        "EAP": [],
    }

    android_counts = Counter()

    for index, (run_id, run) in enumerate(manifest.items(), 1):

        print(
            f"[{index}/{len(manifest)}] "
            f"{run['scenario']} "
            f"{run_id}"
        )

        file_events = load_file_events(run_id)
        android_events = load_android_events(run_id)

        # --------------------------------------------------
        # Determine window span.
        #
        # The training data uses 5-second windows.
        # R3 can therefore produce many windows.
        # --------------------------------------------------

        duration = max(
            run["end_timestamp_ms"] -
            run["start_timestamp_ms"],
            WINDOW_MS
        )

        n_windows = int(
            np.ceil(duration / WINDOW_MS)
        )

        # Android events are stored with absolute timestamps.
        # Convert them to elapsed milliseconds from the run start.
        normalized_android = []

        for e in android_events:

            timestamp = e.get("timestamp_ms")

            if timestamp is None:
                continue

            elapsed = timestamp - run["start_timestamp_ms"]

            x = dict(e)
            x["_elapsed_ms"] = elapsed

            normalized_android.append(x)

            android_counts[e.get("layer", "UNKNOWN")] += 1

        # --------------------------------------------------
        # Build all 4 layer datasets
        # --------------------------------------------------

        for w in range(n_windows):

            start_ms = w * WINDOW_MS
            end_ms = start_ms + WINDOW_MS

            common = build_common_row(
                run,
                file_events,
                start_ms + run["start_timestamp_ms"],
                end_ms + run["start_timestamp_ms"]
            )

            # ==============================================
            # SFIM
            # ==============================================

            sfim = dict(common)

            # These four fields were produced by Frida in the
            # original training CSVs.
            #
            # They are intentionally NOT fabricated here because
            # the 157 unseen runs were not captured concurrently
            # with the Frida collector.
            sfim["SFIM_file_read_open_count"] = 0
            sfim["SFIM_file_write_open_count"] = 0
            sfim["SFIM_file_delete_count"] = 0
            sfim["SFIM_file_rename_count"] = 0

            run_rows["SFIM"].append(sfim)

            # ==============================================
            # MPVG
            # ==============================================

            mpvg = dict(common)

            mpvg_events = [
                e for e in normalized_android
                if start_ms <= e["_elapsed_ms"] < end_ms
            ]

            mpvg["MPVG_checkSelfPermission_count"] = sum(
                1 for e in mpvg_events
                if e.get("layer") == "MPVG"
                and e.get("event") == "checkSelfPermission"
            )

            mpvg["MPVG_requestPermissions_count"] = sum(
                1 for e in mpvg_events
                if e.get("layer") == "MPVG"
                and e.get("event") == "requestPermissions"
            )

            run_rows["MPVG"].append(mpvg)

            # ==============================================
            # CAIT
            # ==============================================

            cait = dict(common)

            cait["CAIT_camera_id_list_count"] = sum(
                1 for e in mpvg_events
                if e.get("layer") == "CAIT"
                and e.get("event") == "camera_id_list"
            )

            cait["CAIT_camera_open_count"] = sum(
                1 for e in mpvg_events
                if e.get("layer") == "CAIT"
                and e.get("event") == "camera_open"
            )

            run_rows["CAIT"].append(cait)

            # ==============================================
            # EAP
            # ==============================================

            eap = dict(common)

            eap["EAP_cipher_doFinal_count"] = sum(
                1 for e in mpvg_events
                if e.get("layer") == "EAP"
                and e.get("event") == "cipher_doFinal"
            )

            run_rows["EAP"].append(eap)

    # ------------------------------------------------------
    # Write CSVs
    # ------------------------------------------------------

    schemas = {
        "SFIM": [
            *COMMON_COLUMNS,
            "SFIM_file_read_open_count",
            "SFIM_file_write_open_count",
            "SFIM_file_delete_count",
            "SFIM_file_rename_count",
        ],

        "MPVG": [
            *COMMON_COLUMNS,
            "MPVG_checkSelfPermission_count",
            "MPVG_requestPermissions_count",
        ],

        "CAIT": [
            *COMMON_COLUMNS,
            "CAIT_camera_id_list_count",
            "CAIT_camera_open_count",
        ],

        "EAP": [
            *COMMON_COLUMNS,
            "EAP_cipher_doFinal_count",
        ],
    }

    filenames = {
        "SFIM": "SFIM_unseen_dataset.csv",
        "MPVG": "MPVG_unseen_dataset.csv",
        "CAIT": "CAIT_unseen_dataset.csv",
        "EAP": "EAP_unseen_dataset.csv",
    }

    print()
    print("=" * 60)
    print("WRITING DATASETS")
    print("=" * 60)

    for layer in ["SFIM", "MPVG", "CAIT", "EAP"]:

        df = pd.DataFrame(run_rows[layer])

        df = df[schemas[layer]]

        output = OUT / filenames[layer]

        df.to_csv(output, index=False)

        print(
            f"{layer}: {len(df)} rows | "
            f"{df['run_id'].nunique()} runs | "
            f"{output}"
        )

    print()
    print("=" * 60)
    print("ANDROID TELEMETRY")
    print("=" * 60)

    for layer, count in android_counts.items():
        print(f"{layer}: {count}")

    print()
    print("DONE.")


if __name__ == "__main__":
    main()
