#!/usr/bin/env python3
"""
MCP protocol bridge for Space game.

This server implements the Model Context Protocol (MCP) and bridges to the
Space game's HTTP REST API running on localhost.

Usage:
    python scripts/mcp_bridge.py

Environment variables:
    SPACE_MCP_PORT: Port where the Space HTTP server is running (default: 18080)
"""

import os
from urllib.request import urlopen
from urllib.parse import urlencode
from urllib.error import URLError, HTTPError
import json

from mcp.server.fastmcp import FastMCP

# Configuration
SPACE_PORT = int(os.environ.get("SPACE_MCP_PORT", "18080"))
SPACE_HOST = "127.0.0.1"

# Create MCP server
mcp = FastMCP(
    name="Space",
    instructions="""
    Control a space probe in a physics-based orbital simulation.
    
    The game runs in real-time - the probe continues moving while you think and act.
    You must account for processing delays when planning maneuvers.

    Available actions:
    - observe: Get current state (ship pose, velocity, fuel, hull, nearest planet, altitude, gravity)
    - world: Get static/dynamic world info (star + planets with positions and metadata)
    - act: Apply thrust at a specific angle
    - reset: Start a new simulation with a specific seed

    Goal: Navigate the probe to land safely on planets while managing fuel and momentum.
    """
)


def _make_request(endpoint: str, params: dict | None = None) -> dict:
    """Make HTTP request to Space game server."""
    url = f"http://{SPACE_HOST}:{SPACE_PORT}{endpoint}"
    if params:
        url += f"?{urlencode(params)}"
    
    try:
        with urlopen(url, timeout=5) as response:
            return json.loads(response.read().decode())
    except HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}: {e.reason}")
    except URLError as e:
        raise RuntimeError(f"Failed to connect to Space server at {SPACE_HOST}:{SPACE_PORT}. "
                         f"Make sure the server is running. Error: {e.reason}")
    except Exception as e:
        raise RuntimeError(f"Request failed: {e}")


@mcp.tool()
def observe(nearest_n: int | None = None) -> dict:
    """
    Get the current state of the space probe and environment.

    Args:
        nearest_n: Optional number of closest planets to include (default server-side = 3)

    Returns:
        Dictionary containing:
        - now: Current simulation time (seconds)
        - ship: {x, y, vx, vy, angle, fuel, fuelCapacity, hull, hullCapacity}
        - nearestPlanet: {id, x, y, radius, mass, canLand} | null
        - nearestPlanetId: index into world.planets or -1
        - nearestPlanets: list of closest planets, each {id, x, y, radius, distance, altitude, gx, gy, vx, vy, mass}
        - landing: {planetId, planetName, angle, text} | null
        - altitude: Distance to nearest planet surface (meters, null if unknown)
        - gravity: {gx, gy} - gravity vector at probe position
        - bodies: Total number of celestial bodies in the system

    Note: The game runs in real-time. By the time you act on this observation,
    the probe will have moved. Account for your processing delay.
    """
    params = {"nearestN": nearest_n} if nearest_n is not None else None
    return _make_request("/observe", params)


@mcp.tool()
def act(thrust: float, angle: float) -> dict:
    """
    Apply thrust to the space probe.
    
    Args:
        thrust: Thrust magnitude (0.0 to 1.0, where 1.0 is maximum thrust)
        angle: Thrust direction in radians (0 = right, π/2 = up, π = left, 3π/2 = down)
    
    Returns:
        Confirmation of the command being applied
    
    Note: In real-time mode, this command is applied immediately but the probe
    continues moving. There's no need to call step() - the simulation advances
    automatically.
    """
    if not 0.0 <= thrust <= 1.0:
        raise ValueError("thrust must be between 0.0 and 1.0")
    
    return _make_request("/act", {"thrust": thrust, "angle": angle})


@mcp.tool()
def reset(seed: int) -> dict:
    """
    Reset the simulation with a new random seed.
    
    Args:
        seed: Random seed for world generation (integer)
    
    Returns:
        Confirmation of reset and initial state
    
    Note: This creates a new universe with different planet positions and
    characteristics. Use the same seed to get reproducible scenarios.
    """
    return _make_request("/reset", {"seed": seed})


@mcp.tool()
def world() -> dict:
    """
    Get the world description.

    Returns:
        Dictionary containing:
        - now
        - universeRange
        - config: {realtime, timeScale}
        - star: {name, x, y, vx, vy, radius, mass, class, deadly, collides, canLand:false}
        - planets: [{id, name, x, y, vx, vy, radius, mass, collides, canLand, explored, description, atmosphere, flora, fauna}]
        - bodies: count (planets + 1 star)
    """
    return _make_request("/world")

@mcp.tool()
def health() -> dict:
    """
    Check if the Space game server is running and responsive.

    Returns:
        Server health status and version information
    """
    return _make_request("/health")


if __name__ == "__main__":
    # Run the MCP server via stdio
    mcp.run()

