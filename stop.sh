#!/bin/bash
# Stop script for accounting-backend

PIDFILE=".quarkus.pid"

if [ ! -f "$PIDFILE" ]; then
    echo "PID file not found. Server may not be running."
    exit 1
fi

PID=$(cat "$PIDFILE")

if ! ps -p $PID > /dev/null 2>&1; then
    echo "Process $PID is not running"
    rm -f "$PIDFILE"
    exit 1
fi

echo "Stopping accounting-backend (PID: $PID)..."
kill $PID

# Wait for graceful shutdown
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "Server stopped successfully"
        rm -f "$PIDFILE"
        exit 0
    fi
    sleep 1
done

# Force kill if still running
if ps -p $PID > /dev/null 2>&1; then
    echo "Forcing shutdown..."
    kill -9 $PID
    sleep 2
fi

rm -f "$PIDFILE"
echo "Server stopped"
