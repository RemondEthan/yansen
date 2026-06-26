package com.glodon.mordor.yansen.tool;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: SPI contract for tool beans. Implementations are discovered via
 * {@link java.util.ServiceLoader} from {@code META-INF/services/com.glodon.mordor.yansen.tool.ToolProvider}
 * and registered under the id returned by {@link #toolId()}.
 *
 * <p>The provider itself is usually the {@link io.agentscope.core.tool.Tool @Tool}-annotated
 * bean — in that case {@link #toolBean()} can be left to its default, which returns
 * {@code this}. Override it only when the bean must be a different object (e.g. a proxy
 * or a stateful child constructed from the provider).</p>
 */
public interface ToolProvider {

    /** Stable id used in YAML {@code agents[*].tools} to reference this tool. Must be unique. */
    String toolId();

    /** The bean whose {@code @Tool}-annotated methods are registered on the Toolkit. */
    default Object toolBean() {
        return this;
    }
}
