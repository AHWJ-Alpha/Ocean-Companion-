package com.projectocean.oceancompanion.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class WavVoiceRecorder(private val context: Context) {
    private val sampleRate = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var outputFile: File? = null

    fun start(): Result<File> = runCatching {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            error("麦克风权限不足")
        }
        stop()
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(sampleRate)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            minBuffer * 2
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            error("录音器初始化失败")
        }
        val file = File(context.cacheDir, "ocean_voice_${System.currentTimeMillis()}.wav")
        writeWavHeader(file, 0)
        outputFile = file
        recorder = audioRecord
        audioRecord.startRecording()
        recordingThread = Thread {
            FileOutputStream(file, true).use { stream ->
                val buffer = ByteArray(minBuffer)
                while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) stream.write(buffer, 0, read)
                }
            }
        }.also { it.start() }
        file
    }

    fun stop(): File? {
        val audioRecord = recorder
        recorder = null
        if (audioRecord != null) {
            runCatching { if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) audioRecord.stop() }
            runCatching { audioRecord.release() }
        }
        val file = outputFile
        outputFile = null
        runCatching { recordingThread?.join(900) }
        recordingThread = null
        if (file != null && file.exists()) updateWavSizes(file)
        return file
    }

    private fun writeWavHeader(file: File, dataLength: Int) {
        FileOutputStream(file, false).use { out ->
            out.write("RIFF".toByteArray())
            out.writeIntLE(36 + dataLength)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.writeIntLE(16)
            out.writeShortLE(1)
            out.writeShortLE(1)
            out.writeIntLE(sampleRate)
            out.writeIntLE(sampleRate * 2)
            out.writeShortLE(2)
            out.writeShortLE(16)
            out.write("data".toByteArray())
            out.writeIntLE(dataLength)
        }
    }

    private fun updateWavSizes(file: File) {
        val dataLength = (file.length() - 44).coerceAtLeast(0).toInt()
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(4)
            raf.writeIntLE(36 + dataLength)
            raf.seek(40)
            raf.writeIntLE(dataLength)
        }
    }

    private fun FileOutputStream.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        ))
    }

    private fun FileOutputStream.writeShortLE(value: Int) {
        write(byteArrayOf((value and 0xff).toByte(), ((value shr 8) and 0xff).toByte()))
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        ))
    }
}
