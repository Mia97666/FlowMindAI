import sys
sys.path.insert(0, "/Users/senga/.workbuddy/skills/weapp-automated-testing/scripts")
from weapp_automation import AutomationConfig, WeappTestRunner

config = AutomationConfig(
    project_path="/Users/senga/Documents/麻将计分/miniapp",
    ws_endpoint="ws://localhost:9420"
)

runner = WeappTestRunner(config)
results = (runner
    .navigate("pages/index/index")
    .wait(3)
    .screenshot("/Users/senga/Documents/麻将计分/screenshots/index.png")
    .get_results())

print(runner.get_summary())
for r in results:
    print(r)
