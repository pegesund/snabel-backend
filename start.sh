#!/bin/bash
# Start script for accounting-backend

PIDFILE=".quarkus.pid"

# Check if already running
if [ -f "$PIDFILE" ]; then
    PID=$(cat "$PIDFILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "Server is already running with PID $PID"
        exit 1
    else
        echo "Removing stale PID file"
        rm -f "$PIDFILE"
    fi
fi

echo "Starting accounting-backend..."
nohup ./mvnw quarkus:dev > quarkus.log 2>&1 &
echo $! > "$PIDFILE"

echo "Started with PID $(cat $PIDFILE)"
echo "Waiting for server to start..."
sleep 30

# Check if it's actually running
if ps -p $(cat "$PIDFILE") > /dev/null 2>&1; then
    echo "Server started successfully!"
    echo "Logs: tail -f quarkus.log"
else
    echo "Failed to start server"
    rm -f "$PIDFILE"
    exit 1
fi
