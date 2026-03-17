package com.example.uf1bridgedemo

import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

private val FLAG_TIME_SRC_PRESENT: UShort = 0x0001u.toUShort()
private val FLAG_TIME_US_IS_RX: UShort    = 0x0002u.toUShort()

internal fun tlv(type: Int, value: ByteArray): ByteArray {
    val bb = ByteBuffer.allocate(3 + value.size).order(ByteOrder.LITTLE_ENDIAN)
    bb.put(type.toByte())
    bb.putShort(value.size.toShort())
    bb.put(value)
    return bb.array()
}

private fun ByteBuffer.putU48LE(v: Long) {
    for (i in 0 until 6) put(((v shr (8 * i)) and 0xFF).toByte())
}

internal fun uf1EncodeFrameStatusPlusBlock(
    deviceId: UInt,
    seq: UInt,
    tUs: Long,
    statusVal: ByteArray,
    blockType: Int,
    blockVal: ByteArray
): ByteArray {
    val flags   = FLAG_TIME_US_IS_RX
    val payload = tlv(0x06, statusVal) + tlv(blockType, blockVal)
    val frameLen = 24 + payload.size

    val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
        put('U'.code.toByte()); put('D'.code.toByte())
        put(1); put(24)
        putShort(frameLen.toShort())
        putShort(flags.toShort())
        putInt(deviceId.toInt())
        put(0); put(0)
        putInt(seq.toInt())
        putU48LE(tUs)
    }.array()

    return header + payload
}

internal fun uf1EncodeFrameStatusEmg(
    deviceId: UInt,
    seq: UInt,
    tUs: Long,
    tSrcSample: UInt,
    sampleRateHz: Int,
    batteryPct: Int,
    rssiDbm: Int,
    mode: Int,
    statusFlags: Int,
    samples: ShortArray
): ByteArray {
    val flags = (FLAG_TIME_SRC_PRESENT.toInt() or FLAG_TIME_US_IS_RX.toInt()).toUShort()

    val statusVal = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).apply {
        putInt(tSrcSample.toInt())
        putShort(sampleRateHz.toShort())
        put(batteryPct.toByte())
        put(rssiDbm.toByte())
        put(mode.toByte())
        put(statusFlags.toByte())
    }.array()

    val emgVal = ByteBuffer.allocate(4 + samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
        put(1)                        // channel_count
        put(samples.size.toByte())    // samples_per_ch
        put(1)                        // sample_format = int16
        put(0)
        for (s in samples) putShort(s)
    }.array()

    val payload  = tlv(0x06, statusVal) + tlv(0x01, emgVal)
    val frameLen = 24 + payload.size

    val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
        put('U'.code.toByte()); put('D'.code.toByte())
        put(1); put(24)
        putShort(frameLen.toShort())
        putShort(flags.toShort())
        putInt(deviceId.toInt())
        put(0); put(0)
        putInt(seq.toInt())
        putU48LE(tUs)
    }.array()

    return header + payload
}

// Block type 0x07: device name announcement, UTF-8, 1–32 bytes, no null terminator.
// Sent once per session on first data notification so the workbench can label the stream.
internal fun uf1EncodeDeviceName(devId: UInt, seq: UInt, name: String): ByteArray {
    val nameBytes = name.toByteArray(Charsets.UTF_8)
        .let { if (it.size > 32) it.copyOf(32) else it }
        .let { if (it.isEmpty()) byteArrayOf('?'.code.toByte()) else it }

    val tUs = (SystemClock.elapsedRealtimeNanos() / 1000L) and ((1L shl 48) - 1)

    val statusVal = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).apply {
        putInt(0); putShort(0)      // no t_src, no sample_rate
        put(255.toByte()); put(0)   // battery unknown, rssi unknown
        put(0); put(0)
    }.array()

    return uf1EncodeFrameStatusPlusBlock(devId, seq, tUs, statusVal, 0x07, nameBytes)
}

internal fun crc32U32(data: ByteArray): UInt {
    val crc = CRC32()
    crc.update(data)
    return crc.value.toUInt()
}
