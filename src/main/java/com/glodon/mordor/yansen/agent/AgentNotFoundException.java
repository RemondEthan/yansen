package com.glodon.mordor.yansen.agent;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Thrown when agent configuration or a referenced entity is missing at load time.
 */
public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String message) {
        super(message);
    }
}
