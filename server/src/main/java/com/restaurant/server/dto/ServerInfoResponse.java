package com.restaurant.server.dto;

public record ServerInfoResponse(String server, int port, String protocol, String version) {}