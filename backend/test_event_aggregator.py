
from event_aggregator import AndroidEventAggregator


aggregator = AndroidEventAggregator()

aggregator.add_event({
    "layer": "MPVG",
    "event": "checkSelfPermission",
    "permission": "android.permission.CAMERA",
    "granted": True,
})

aggregator.add_event({
    "layer": "SFIM",
    "event": "file_delete",
    "path": "/data/user/0/com.ranscope.android/files/report.txt",
    "success": True,
})

result = aggregator.snapshot()

assert result["features"]["MPVG_checkSelfPermission_count"] == 1
assert result["features"]["SFIM_file_delete_count"] == 1
assert result["features"]["txt_count"] == 1
assert result["features"]["file_event_count"] == 1
assert "avg_entropy" in result["unavailable"]
assert result["complete"] is False

print("Aggregator smoke test passed.")
print(result)
