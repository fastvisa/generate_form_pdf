#!/bin/bash

# Health check script for manipulate-pdf API
set -e

HOST=${1:-localhost}
PORT=${2:-8080}
TIMEOUT=${3:-10}

echo "🔍 Health checking manipulate-pdf API at $HOST:$PORT..."

# Check if the service is responding
if curl -f -s --max-time $TIMEOUT "http://$HOST:$PORT/health" > /tmp/health_check.json; then
    echo "✅ Service is UP"
    
    # Parse and display the response
    if command -v jq &> /dev/null; then
        echo "📊 Service Details:"
        jq . /tmp/health_check.json
    else
        echo "📋 Raw Response:"
        cat /tmp/health_check.json
    fi
    
    # Cleanup
    rm -f /tmp/health_check.json
    
    echo ""
    echo "🎉 Health check PASSED"
    exit 0
else
    echo "❌ Service is DOWN or not responding"
    echo "🔧 Troubleshooting tips:"
    echo "   1. Check if the service is running: pm2 status"
    echo "   2. Check logs: pm2 logs manipulate-pdf-api"
    echo "   3. Verify port $PORT is open: netstat -tulpn | grep $PORT"
    echo "   4. Check if host $HOST is reachable: ping $HOST"
    
    # Cleanup
    rm -f /tmp/health_check.json
    
    exit 1
fi