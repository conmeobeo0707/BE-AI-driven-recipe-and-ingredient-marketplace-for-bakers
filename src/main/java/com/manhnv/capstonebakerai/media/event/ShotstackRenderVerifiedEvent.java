package com.manhnv.capstonebakerai.media.event;

/** Render details fetched from Shotstack, rather than trusted from the callback body. */
public record ShotstackRenderVerifiedEvent(
        String renderId, String status, String videoUrl, String errorMessage) {
}
