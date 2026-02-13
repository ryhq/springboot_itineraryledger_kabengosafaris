# Log SSE Controller API Documentation

## Overview

The Log SSE (Server-Sent Events) Controller provides real-time access log streaming capabilities. It allows clients to receive live log updates as they occur, with automatic heartbeat and connection management.

## Base URL

```
/api/logs/stream
```

## Authentication

All endpoints require authentication with a valid JWT token and the `PERM_READ_LOG` permission.

### Headers Required

```
Authorization: Bearer <jwt-token>
```

---

## What is Server-Sent Events (SSE)?

Server-Sent Events (SSE) is a server-push technology that enables a server to send real-time updates to clients over a single HTTP connection. Unlike WebSockets, SSE is unidirectional (server → client only) and works over standard HTTP.

### SSE vs WebSockets

| Feature | SSE | WebSockets |
|---------|-----|------------|
| Direction | Server → Client | Bidirectional |
| Protocol | HTTP | WS/WSS |
| Auto-reconnect | Yes (built-in) | No (manual) |
| Event Types | Named events | Binary/Text |
| Browser Support | All modern browsers | All modern browsers |
| Best For | Real-time updates, logs, notifications | Chat, gaming, bidirectional data |

---

## Response Format

The `/status` endpoint returns responses wrapped in the standardized `ApiResponse` format:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Stream status retrieved successfully",
  "data": {
    // Status data here
  },
  "timestamp": "2026-02-10T14:23:45.123"
}
```

The `/stream` endpoint uses Server-Sent Events format (not ApiResponse). Events are sent as:

```
event: log
data: {"logId": "abc123", "remoteAddress": "192.168.1.1", ...}

event: heartbeat
data: ping
```

---

## Endpoints

### 1. Stream Access Logs

Open a real-time stream of access logs as they occur.

**Endpoint:** `GET /api/logs/stream`

**Permission Required:** `PERM_READ_LOG`

**Content-Type:** `text/event-stream`

#### Connection Lifecycle

```
Client                          Server
  |                               |
  |--- GET /api/logs/stream ----->|
  |<-- 200 OK (SSE stream) -------|
  |                               |
  |<-- event: connected ----------|
  |<-- event: heartbeat ----------|
  |<-- event: log ----------------|
  |<-- event: log ----------------|
  |<-- event: heartbeat ----------|
  |<-- event: log ----------------|
  |                               |
  |--- (client disconnect) ------>|
  |                               |
```

#### Event Types

The server sends three types of events:

##### 1. Connected Event

Sent immediately upon successful connection.

```
event: connected
data: Connected to access log stream
```

##### 2. Heartbeat Event

Sent every 15 seconds to keep the connection alive.

```
event: heartbeat
data: ping
```

##### 3. Log Event

Sent for each new access log entry.

```
event: log
data: {
  "logId": "encoded_id",
  "remoteAddress": "192.168.1.100",
  "timestamp": "09/Feb/2026:14:30:45 +0300",
  "timestampEpoch": 1707484245000,
  "requestMethod": "GET",
  "requestUri": "/api/parks",
  "status": 200,
  "responseSizeBytes": 15420,
  "timeTakenMillis": 245,
  "userAgent": "Mozilla/5.0...",
  "isSuspicious": false,
  "isBot": false,
  "browserName": "Chrome",
  "operatingSystem": "Windows"
}
```

#### Client Implementation

##### JavaScript (Browser)

```javascript
const eventSource = new EventSource('/api/logs/stream', {
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  }
});

// Handle connected event
eventSource.addEventListener('connected', (event) => {
  console.log('Connected:', event.data);
});

// Handle heartbeat event
eventSource.addEventListener('heartbeat', (event) => {
  console.log('Heartbeat received');
});

// Handle log events
eventSource.addEventListener('log', (event) => {
  const logEntry = JSON.parse(event.data);
  console.log('New log:', logEntry);

  // Display in UI
  displayLog(logEntry);
});

// Handle errors
eventSource.onerror = (error) => {
  console.error('SSE error:', error);
  // EventSource will automatically reconnect
};

// Close connection when done
// eventSource.close();
```

##### JavaScript (Fetch API - Manual)

```javascript
async function streamLogs() {
  const response = await fetch('/api/logs/stream', {
    headers: {
      'Authorization': 'Bearer YOUR_JWT_TOKEN'
    }
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();

    if (done) break;

    const text = decoder.decode(value);
    const lines = text.split('\n');

    for (const line of lines) {
      if (line.startsWith('event:')) {
        const eventType = line.substring(6).trim();
        // Handle event type
      } else if (line.startsWith('data:')) {
        const data = line.substring(5).trim();
        // Handle event data
      }
    }
  }
}
```

##### curl

```bash
curl -N -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  https://api.kabengosafaris.com/api/logs/stream
```

##### Python

```python
import requests
import json

def stream_logs(token):
    headers = {
        'Authorization': f'Bearer {token}',
        'Accept': 'text/event-stream'
    }

    with requests.get(
        'https://api.kabengosafaris.com/api/logs/stream',
        headers=headers,
        stream=True
    ) as response:

        for line in response.iter_lines():
            if line:
                line_str = line.decode('utf-8')

                if line_str.startswith('event:'):
                    event_type = line_str[6:].strip()

                elif line_str.startswith('data:'):
                    data = line_str[5:].strip()

                    if event_type == 'log':
                        log_entry = json.loads(data)
                        print(f"New log: {log_entry['requestUri']}")

                    elif event_type == 'heartbeat':
                        print("Heartbeat received")

                    elif event_type == 'connected':
                        print(f"Connected: {data}")

# Usage
stream_logs('your-jwt-token')
```

---

### 2. Get Stream Status

Get current streaming status and statistics.

**Endpoint:** `GET /api/logs/stream/status`

**Permission Required:** `PERM_READ_LOG`

#### Response

```json
{
  "streamingActive": true,
  "activeConnections": 3,
  "lastFilePosition": 2048576,
  "logsProcessed": 15234
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `streamingActive` | Boolean | Whether streaming service is currently active |
| `activeConnections` | Integer | Number of connected clients |
| `lastFilePosition` | Long | Current position in log file (bytes) |
| `logsProcessed` | Long | Total number of logs processed since streaming started |

#### Examples

**Check streaming status:**
```bash
GET /api/logs/stream/status
```

**Response when no clients connected:**
```json
{
  "streamingActive": false,
  "activeConnections": 0,
  "lastFilePosition": 0,
  "logsProcessed": 0
}
```

---

### 3. Disconnect All Connections

Force-close all active SSE connections. Useful for maintenance, emergency shutdown, or clearing stale connections.

**Endpoint:** `DELETE /api/logs/stream/disconnect`

**Permission Required:** `PERM_READ_LOG`

#### Response

**Success (connections closed):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "All connections disconnected successfully",
  "data": {
    "disconnectedConnections": 3,
    "message": "All connections successfully closed"
  },
  "timestamp": "2026-02-10T14:23:45.123"
}
```

**Success (no connections):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "No active connections found",
  "data": {
    "disconnectedConnections": 0,
    "message": "No active connections to disconnect"
  },
  "timestamp": "2026-02-10T14:23:45.123"
}
```

#### Response Fields (within `data` object)

| Field | Type | Description |
|-------|------|-------------|
| `disconnectedConnections` | Integer | Number of connections that were closed |
| `message` | String | Human-readable status message |

#### Examples

**Disconnect all clients:**
```bash
DELETE /api/logs/stream/disconnect
Authorization: Bearer YOUR_JWT_TOKEN
```

**Using curl:**
```bash
curl -X DELETE -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  https://api.kabengosafaris.com/api/logs/stream/disconnect
```

**Using JavaScript:**
```javascript
async function disconnectAllClients(token) {
  const response = await fetch('/api/logs/stream/disconnect', {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  const result = await response.json();
  console.log(`Disconnected ${result.data.disconnectedConnections} client(s)`);
}
```

#### Use Cases

1. **Maintenance Mode**: Disconnect all clients before server maintenance
2. **Emergency Shutdown**: Force-close all connections in case of system issues
3. **Clear Stale Connections**: Remove connections that may not have properly closed
4. **Security**: Revoke access immediately if needed

#### Important Notes

- This endpoint closes **ALL** active connections, not just the caller's connection
- Streaming service is automatically stopped when all connections are closed
- Clients will receive a connection close event and can reconnect if needed
- For client-side disconnection, use `eventSource.close()` instead

---

## Technical Details

### Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Heartbeat Interval | 15 seconds | Frequency of heartbeat events |
| Poll Interval | 1 second | Frequency of log file checking |
| Connection Timeout | Infinite | SSE connections don't timeout |

### Streaming Behavior

1. **File Tailing**
   - Server polls log file every 1 second
   - Reads new lines since last check
   - Parses and enriches each log entry
   - Broadcasts to all connected clients

2. **File Rotation Handling**
   - Detects when log file is rotated (new day)
   - Automatically resets file position
   - Continues streaming from new file

3. **Connection Management**
   - Streaming starts when first client connects
   - Streaming stops when last client disconnects
   - Automatic cleanup of disconnected clients

4. **Heartbeat Mechanism**
   - Keeps connection alive through firewalls/proxies
   - Detects broken connections
   - Sent every 15 seconds

### Performance Characteristics

| Metric | Value |
|--------|-------|
| Latency | ~1-2 seconds |
| Memory per client | ~1-2 MB |
| CPU overhead | Minimal |
| Network bandwidth | Depends on log rate |
| Max concurrent clients | Limited by server resources |

---

## Use Cases

### 1. Real-Time Monitoring Dashboard

Display live access logs in a web dashboard:

```javascript
const eventSource = new EventSource('/api/logs/stream');

eventSource.addEventListener('log', (event) => {
  const log = JSON.parse(event.data);

  // Update dashboard
  addToLogTable(log);
  updateMetrics({
    totalRequests: incrementCounter(),
    avgResponseTime: calculateAverage(log.timeTakenMillis),
    errorRate: calculateErrorRate(log.status)
  });

  // Highlight security threats
  if (log.isSuspicious) {
    showSecurityAlert(log);
  }
});
```

### 2. Security Monitoring

Alert on security threats in real-time:

```javascript
eventSource.addEventListener('log', (event) => {
  const log = JSON.parse(event.data);

  if (log.isSuspicious && log.threatScore >= 80) {
    // Send immediate alert
    sendSlackNotification({
      title: 'Critical Security Threat Detected',
      ip: log.remoteAddress,
      threatType: log.threatType,
      endpoint: log.requestUri
    });
  }
});
```

### 3. Performance Monitoring

Track slow requests in real-time:

```javascript
eventSource.addEventListener('log', (event) => {
  const log = JSON.parse(event.data);

  if (log.isSlowRequest) {
    console.warn(`Slow request: ${log.requestUri} (${log.timeTakenMillis}ms)`);

    // Trigger investigation
    if (log.timeTakenMillis > 5000) {
      notifyDevOps({
        endpoint: log.requestUri,
        responseTime: log.timeTakenMillis
      });
    }
  }
});
```

### 4. Traffic Analysis

Analyze traffic patterns in real-time:

```javascript
const trafficStats = {
  total: 0,
  byMethod: {},
  byEndpoint: {},
  byStatus: {}
};

eventSource.addEventListener('log', (event) => {
  const log = JSON.parse(event.data);

  trafficStats.total++;
  trafficStats.byMethod[log.requestMethod] =
    (trafficStats.byMethod[log.requestMethod] || 0) + 1;
  trafficStats.byEndpoint[log.requestUri] =
    (trafficStats.byEndpoint[log.requestUri] || 0) + 1;
  trafficStats.byStatus[log.status] =
    (trafficStats.byStatus[log.status] || 0) + 1;

  // Update charts
  updateTrafficCharts(trafficStats);
});
```

---

## Error Handling

### Client-Side Errors

#### Connection Failed

```javascript
eventSource.onerror = (error) => {
  if (eventSource.readyState === EventSource.CONNECTING) {
    console.log('Reconnecting...');
  } else if (eventSource.readyState === EventSource.CLOSED) {
    console.error('Connection closed');
    // Manual reconnect if needed
  }
};
```

#### Authentication Failure

If JWT token is invalid or expired:
- Server returns 401 Unauthorized
- EventSource triggers error event
- Connection closes
- Refresh token and reconnect

```javascript
eventSource.onerror = async (error) => {
  if (error.status === 401) {
    // Refresh token
    const newToken = await refreshAuthToken();

    // Close old connection
    eventSource.close();

    // Reconnect with new token
    reconnectWithNewToken(newToken);
  }
};
```

### Server-Side Errors

#### Log File Not Found

- Server continues streaming
- No events sent until log file appears
- Logs warning in server logs

#### Log File Read Error

- Server logs error
- Continues polling
- Recovers automatically when error resolved

#### Parsing Error

- Malformed log lines are skipped
- Error logged server-side
- Streaming continues

---

## Best Practices

### 1. Implement Reconnection Logic

```javascript
function connectToLogStream() {
  const eventSource = new EventSource('/api/logs/stream');

  eventSource.addEventListener('log', handleLog);

  eventSource.onerror = () => {
    console.log('Connection lost, reconnecting in 5s...');
    setTimeout(connectToLogStream, 5000);
  };

  return eventSource;
}
```

### 2. Limit UI Updates

```javascript
let logBuffer = [];
let updateScheduled = false;

eventSource.addEventListener('log', (event) => {
  const log = JSON.parse(event.data);
  logBuffer.push(log);

  if (!updateScheduled) {
    updateScheduled = true;
    requestAnimationFrame(() => {
      updateUI(logBuffer);
      logBuffer = [];
      updateScheduled = false;
    });
  }
});
```

### 3. Handle Tab Visibility

```javascript
document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    // Pause or reduce updates when tab hidden
    eventSource.close();
  } else {
    // Resume when tab visible
    eventSource = new EventSource('/api/logs/stream');
  }
});
```

### 4. Monitor Connection Health

```javascript
let lastHeartbeat = Date.now();

eventSource.addEventListener('heartbeat', () => {
  lastHeartbeat = Date.now();
});

// Check every 30 seconds
setInterval(() => {
  const timeSinceHeartbeat = Date.now() - lastHeartbeat;

  if (timeSinceHeartbeat > 30000) {
    console.warn('No heartbeat for 30s, connection may be stale');
    eventSource.close();
    connectToLogStream();
  }
}, 30000);
```

### 5. Properly Disconnect When Done

**Client-side disconnection (recommended for individual clients):**
```javascript
// Always close EventSource when done
const eventSource = new EventSource('/api/logs/stream');

// Later, when user navigates away or component unmounts
eventSource.close();
console.log('Disconnected from log stream');
```

**Server-side disconnection (for administrators):**
```javascript
// Disconnect all clients (requires PERM_READ_LOG permission)
async function disconnectAllClients(token) {
  try {
    const response = await fetch('/api/logs/stream/disconnect', {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    const result = await response.json();
    console.log(`Disconnected ${result.data.disconnectedConnections} client(s)`);
  } catch (error) {
    console.error('Failed to disconnect clients:', error);
  }
}

// Use cases:
// - Before server maintenance
// - Emergency shutdown
// - Clearing stale connections
// - Security revocation
```

**React example with cleanup:**
```javascript
useEffect(() => {
  const eventSource = new EventSource('/api/logs/stream', {
    headers: { 'Authorization': `Bearer ${token}` }
  });

  eventSource.addEventListener('log', handleLog);
  eventSource.addEventListener('heartbeat', handleHeartbeat);

  // Cleanup function runs on unmount
  return () => {
    eventSource.close();
    console.log('Component unmounted, connection closed');
  };
}, [token]);
```

---

## Limitations

1. **Unidirectional**: Server → Client only (no client → server messages)
2. **HTTP Only**: Uses standard HTTP (no custom protocols)
3. **Text Only**: Only text data (JSON must be stringified)
4. **Browser Limits**: Some browsers limit concurrent SSE connections (usually 6 per domain)
5. **No Binary**: Cannot send binary data (use WebSockets for that)
6. **Proxy Issues**: Some corporate proxies may buffer SSE responses

---

## Comparison with Polling

### SSE Advantages

| Feature | SSE | Polling |
|---------|-----|---------|
| Latency | Low (1-2s) | High (depends on interval) |
| Server Load | Low | High (constant requests) |
| Network Efficiency | High | Low (many requests) |
| Real-time | Yes | Delayed |
| Connection | Persistent | Many short connections |

### When to Use SSE vs Polling

**Use SSE when:**
- Real-time updates are important
- Server load is a concern
- Network efficiency matters
- Updates are frequent

**Use Polling when:**
- Updates are infrequent
- Client compatibility issues
- Simpler implementation needed
- Behind problematic proxies

---

## Security Considerations

1. **Authentication**: JWT token required for all connections
2. **Authorization**: READ_LOG permission enforced
3. **Rate Limiting**: Consider implementing connection limits per user
4. **Data Filtering**: Only current day's logs are streamed
5. **Connection Limits**: Monitor active connections to prevent resource exhaustion

---

## Monitoring

### Server-Side Metrics

Monitor these metrics:

```java
// Active connections
int activeConnections = emitters.size();

// Logs processed
long logsProcessed = logCounter.get();

// File position
long filePosition = lastFilePosition;

// Streaming status
boolean isActive = streamingActive;
```

### Client-Side Metrics

Track these on the client:

```javascript
const metrics = {
  connected: false,
  logsReceived: 0,
  heartbeatsReceived: 0,
  lastEventTime: null,
  connectionTime: null,
  reconnectCount: 0
};
```

---

## Troubleshooting

### Issue: No events received

**Possible causes:**
- No new logs being generated
- Log file doesn't exist
- Streaming not started

**Solution:**
- Check `/api/logs/stream/status`
- Generate test traffic
- Verify log file configuration

### Issue: Frequent disconnections

**Possible causes:**
- Network issues
- Proxy timeout
- Firewall blocking

**Solution:**
- Check network stability
- Configure proxy timeouts
- Verify firewall rules

### Issue: Missing log entries

**Possible causes:**
- Logs written before connection
- File rotation during connection
- Parsing errors

**Solution:**
- Stream only captures new logs
- Server handles rotation automatically
- Check server logs for parsing errors

---

## Notes

- Streaming only includes logs written AFTER connection is established
- Historical logs are not streamed (use GET `/api/logs` for historical data)
- All security analysis and enrichment is performed in real-time
- Connection automatically recovers from log file rotation
- Multiple clients can connect simultaneously
- Each client receives all log events

---

**Last Updated:** 2026-02-09
**API Version:** 1.0
**Controller:** LogSSEController.java
