import json
from pathlib import Path
from collections import Counter

manifest_file = Path(
    "~/Downloads/ranscope_android_telemetry/run_manifest_latest.jsonl"
).expanduser()

frida_file = Path(
    "~/ranscope_frida_hooks/frida_events.jsonl"
).expanduser()

output_file = Path(
    "~/ranscope_frida_hooks/frida_events_enriched.jsonl"
).expanduser()


# ------------------------------------------------------------
# Load completed Android runs
# ------------------------------------------------------------

runs = []

with manifest_file.open() as f:
    for line in f:
        line = line.strip()

        if not line:
            continue

        run = json.loads(line)

        if run.get("status") != "completed":
            continue

        runs.append(run)


runs.sort(
    key=lambda r: r["start_timestamp_ms"]
)


print(f"Completed Android runs: {len(runs)}")


# ------------------------------------------------------------
# Match Frida event timestamp to Android run
# ------------------------------------------------------------

matched = 0
unmatched = 0

scenario_counts = Counter()
layer_counts = Counter()
event_counts = Counter()

with frida_file.open() as fin, output_file.open("w") as fout:

    for line in fin:

        line = line.strip()

        if not line:
            continue

        try:
            event = json.loads(line)
        except Exception:
            continue

        # Frida timestamp = seconds
        event_ms = int(
            float(event["timestamp"]) * 1000
        )

        matched_run = None

        for run in runs:

            if (
                run["start_timestamp_ms"]
                <= event_ms
                <= run["end_timestamp_ms"]
            ):
                matched_run = run
                break

        if matched_run:

            event["run_id"] = matched_run["run_id"]
            event["scenario"] = matched_run["scenario"]
            event["label"] = matched_run["label"]

            matched += 1

            scenario_counts[
                matched_run["scenario"]
            ] += 1

            layer_counts[
                event.get("layer", "UNKNOWN")
            ] += 1

            event_counts[
                event.get("event", "UNKNOWN")
            ] += 1

        else:

            event["run_id"] = None
            event["scenario"] = None
            event["label"] = None

            unmatched += 1

        fout.write(
            json.dumps(event) + "\n"
        )


# ------------------------------------------------------------
# Report
# ------------------------------------------------------------

print()
print("======================================")
print("FRIDA RUN ASSOCIATION")
print("======================================")

print(f"Total Frida events : {matched + unmatched}")
print(f"Matched events     : {matched}")
print(f"Unmatched events   : {unmatched}")

print()
print("Events by scenario:")

for scenario, count in scenario_counts.items():
    print(f"  {scenario}: {count}")

print()
print("Events by layer:")

for layer, count in layer_counts.items():
    print(f"  {layer}: {count}")

print()
print("Top event types:")

for event, count in event_counts.most_common(15):
    print(f"  {event}: {count}")

print()
print("Output:")
print(output_file)
