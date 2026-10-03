package dev.ua.ikeepcalm.coi.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The namespace and the length caps every {@code coi-client} payload is built from, stated once
 * because the plugin's own payload classes must state the same numbers: {@code writeUtf} defaults
 * to 32767 characters, and both ends have to agree on any cap above that or the packet is
 * rejected mid-flight.
 */
public class CoiPayloads {

    public static final String NAMESPACE = "coi-client";

    /**
     * 1 MiB: the v2 ability list, the character sheet, a menu screen.
     */
    public static final int MAX_DOCUMENT = 1_048_576;

    /** 32 KiB: a click going back to the server. */
    public static final int MAX_ACTION = 32_768;

    private CoiPayloads() {
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NAMESPACE, path));
    }

    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> text(
            Function<T, String> getter, Function<String, T> factory) {
        return StreamCodec.ofMember(
                (value, buf) -> buf.writeUtf(getter.apply(value)),
                buf -> factory.apply(buf.readUtf()));
    }

    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> text(
            Function<T, String> getter, Function<String, T> factory, int max) {
        return StreamCodec.ofMember(
                (value, buf) -> buf.writeUtf(getter.apply(value), max),
                buf -> factory.apply(buf.readUtf(max)));
    }

    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> text2(
            Function<T, String> first, Function<T, String> second, BiFunction<String, String, T> factory) {
        return StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeUtf(first.apply(value));
                    buf.writeUtf(second.apply(value));
                },
                buf -> factory.apply(buf.readUtf(), buf.readUtf()));
    }
}
