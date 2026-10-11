import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path



# ============================================================
# CONFIGURATION
# ============================================================

FRIDA_HOST = "127.0.0.1:27042"

HOOK_DIRECTORY = Path.home() / "ranscope_frida_hooks"

HOOK_FILE = HOOK_DIRECTORY / "collector_hooks.js"

OUTPUT_FILE = HOOK_DIRECTORY / "frida_events.jsonl"

EVENT_MARKER = "[RANSCOPE_EVENT]"

BACKEND_URL = "http://127.0.0.1:8000/api/telemetry/event"

DEVICE_ID = "ranscope-android"


# ============================================================
# FIND RANSCOPE
# ============================================================

def find_ranscope_pid():

    result = subprocess.run(
        [
            "frida-ps",
            "-H",
            FRIDA_HOST
        ],
        capture_output=True,
        text=True,
        check=True
    )

    for line in result.stdout.splitlines():

        parts = line.split()

        if len(parts) < 2:
            continue

        try:
            pid = int(parts[0])
        except ValueError:
            continue

        name = " ".join(parts[1:])

        if name.lower() == "ranscope":
            return pid

    return None


# ============================================================
# EXTRACT JSON OBJECT FROM FRIDA OUTPUT
# ============================================================

def extract_event(line):

    if EVENT_MARKER not in line:
        return None

    json_text = line.split(
        EVENT_MARKER,
        1
    )[1].lstrip()

    if not json_text:
        return None

    decoder = json.JSONDecoder()

    try:

        record, _ = decoder.raw_decode(
            json_text
        )

        return record

    except json.JSONDecodeError:

        return None


# ============================================================
# SAVE EVENT
# ============================================================

def save_event(record):

    if not isinstance(record, dict):
        return

    record["collector_timestamp"] = time.time()

    OUTPUT_FILE.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    with OUTPUT_FILE.open(
        "a",
        encoding="utf-8"
    ) as output:

        output.write(
            json.dumps(
                record,
                separators=(",", ":")
            )
        )

        output.write("\n")

    print(
        "[SAVED] "
        + json.dumps(
            record,
            separators=(",", ":")
        ),
        flush=True
    )

def forward_event(record):
    """Forward an observed Frida event to FastAPI without stopping collection."""
    payload = {
        "device_id": DEVICE_ID,
        "timestamp": record.get("timestamp"),
        "telemetry": record,
    }

    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        BACKEND_URL,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=1.5) as response:
            if response.status >= 400:
                print(
                    f"[BACKEND] Rejected event: HTTP {response.status}",
                    flush=True,
                )
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        print(f"[BACKEND] Forwarding unavailable: {exc}", flush=True)

# ============================================================
# MAIN
# ============================================================

def main():

    print(
        "[RANSCOPE] Starting Frida collector...",
        flush=True
    )

    # --------------------------------------------------------
    # Check hook file
    # --------------------------------------------------------

    if not HOOK_FILE.exists():

        print(
            "[ERROR] collector_hooks.js not found:"
        )

        print(
            HOOK_FILE
        )

        sys.exit(1)

    # --------------------------------------------------------
    # Find RANSCOPE
    # --------------------------------------------------------

    pid = find_ranscope_pid()

    if pid is None:

        print(
            "[ERROR] RANSCOPE process not found."
        )

        print(
            "Start RANSCOPE in the emulator first."
        )

        sys.exit(1)

    print(
        "[RANSCOPE] Found process: "
        + str(pid),
        flush=True
    )

    # --------------------------------------------------------
    # Prepare output
    # --------------------------------------------------------

    OUTPUT_FILE.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    print(
        "[RANSCOPE] Output file:"
    )

    print(
        OUTPUT_FILE
    )

    # --------------------------------------------------------
    # Start Frida CLI
    # --------------------------------------------------------

    command = [
        "frida",
        "-H",
        FRIDA_HOST,
        "-p",
        str(pid),
        "-l",
        str(HOOK_FILE)
    ]

    print()
    print(
        "[RANSCOPE] Starting Frida:"
    )

    print(
        " ".join(command)
    )

    print()

    process = subprocess.Popen(
        command,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    print(
        "[RANSCOPE] Collector running.",
        flush=True
    )

    print(
        "[RANSCOPE] Press Ctrl+C to stop.",
        flush=True
    )

    print()

    # --------------------------------------------------------
    # Read Frida output
    # --------------------------------------------------------

    try:

        for line in process.stdout:

            line = line.rstrip()

            if not line:
                continue

            # Show Frida's normal output.
            print(
                line,
                flush=True
            )

            # Try to extract our event.
            record = extract_event(
                line
            )

            if record is not None:
              save_event(record)
              forward_event(record)


    except KeyboardInterrupt:

        print()
        print(
            "[RANSCOPE] Stopping collector..."
        )

    finally:

        if process.poll() is None:

            process.terminate()

        try:

            process.wait(
                timeout=3
            )

        except subprocess.TimeoutExpired:

            process.kill()

        print(
            "[RANSCOPE] Collector stopped.",
            flush=True
        )


# ============================================================
# ENTRY POINT
# ============================================================

if __name__ == "__main__":

    main()
