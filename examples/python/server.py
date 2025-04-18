# server.py
from mcp.server.fastmcp import FastMCP
import json

# Create an MCP server
mcp = FastMCP("Demo")

result = {
    "result": 10
}


# Add an addition tool
@mcp.tool(description="Add two numbers")
def add(a: int, b: int) -> int:
    """Add two numbers"""
    result['result'] = a + b

    return json.dumps(result)


@mcp.tool(name="sub", description="Subtract two numbers")
def subtract(a: int, b: int) -> int:
    return a - b


if __name__ == "__main__":
    # Initialize and run the server
    mcp.run(transport='stdio')
