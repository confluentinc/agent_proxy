#!/bin/bash

# Text colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Setting up environment variables for Agent Proxy${NC}\n"

# Required variables
echo -e "${GREEN}Required Environment Variables:${NC}"

read -p "Enter Confluent Cloud broker URL (BROKER_URL): " broker_url
read -p "Enter JAAS username (JAAS_USERNAME): " jaas_username
read -p "Enter JAAS password (JAAS_PASSWORD): " -s jaas_password
echo # new line after password
read -p "Enter Schema Registry URL (SR_URL): " sr_url
read -p "Enter Schema Registry API key (SR_API_KEY): " sr_api_key
read -p "Enter Schema Registry API secret (SR_API_SECRET): " -s sr_api_secret
echo # new line after password

# Optional variables
echo -e "\n${GREEN}Optional Environment Variables:${NC}"
read -p "Enter MCP registry topic (REGISTRY_TOPIC) [default: _agent_registry]: " registry_topic
registry_topic=${registry_topic:-_agent_registry}

# Create or update .env file
cat > .env << EOF
export BROKER_URL="${broker_url}"
export JAAS_USERNAME="${jaas_username}"
export JAAS_PASSWORD="${jaas_password}"
export SR_URL="${sr_url}"
export SR_API_KEY="${sr_api_key}"
export SR_API_SECRET="${sr_api_secret}"
export REGISTRY_TOPIC="${registry_topic}"
EOF

# Make the script executable
chmod +x .env

echo -e "\n${GREEN}Environment variables have been saved to .env file${NC}"
echo -e "${BLUE}To load these variables, run:${NC}"
echo "source .env"