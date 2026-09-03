package net.seraphina.ninecun_lingtai.mixin.api;

public enum Type {
    /** Disables the mixin when the specified mod is loaded. */
    MOD_LOAD,

    /** Disables the mixin when the specified mod is not loaded. */
    UN_MOD_LOAD,

    /** Always disables the mixin. */
    FOREVER
}
