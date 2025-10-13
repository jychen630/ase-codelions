#!/bin/bash

# Test script for Twitter MCP Server
# Usage: ./test-mcp.sh

BASE_URL="http://localhost:18060/mcp"

echo "======================================"
echo "Testing Twitter MCP Server"
echo "======================================"
echo ""

echo "1. Testing Initialize..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"initialize","params":{},"id":1}' | python3 -m json.tool
echo ""
echo ""

echo "2. Testing Tools List..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","params":{},"id":2}' | python3 -m json.tool
echo ""
echo ""

echo "3. Testing Check Quota Status..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"check_quota_status","arguments":{}},"id":3}' | python3 -m json.tool
echo ""
echo ""

echo "4. Testing Echo Test..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"echo_test","arguments":{"message":"Hello from test script!"}},"id":4}' | python3 -m json.tool
echo ""
echo ""

echo "======================================"
echo "All tests completed!"
echo "======================================"

