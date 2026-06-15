import requests
import json
import websocket

try:
    res = requests.get("http://localhost:9222/json")
    targets = res.json()
    print("Found targets:", json.dumps(targets, indent=2))
    
    target = next(t for t in targets if t.get("type") == "page" and "index.html" in t.get("url"))
    ws_url = target["webSocketDebuggerUrl"]
    print("Connecting to WebSocket:", ws_url)
    
    ws = websocket.create_connection(ws_url, suppress_origin=True)
    
    # Send a ping or command
    cmd = {
        "id": 1,
        "method": "Runtime.evaluate",
        "params": {
            "expression": "document.title",
            "returnByValue": True
        }
    }
    ws.send(json.dumps(cmd))
    response = ws.recv()
    print("Response:", response)
    ws.close()
except Exception as e:
    print("Error:", e)
