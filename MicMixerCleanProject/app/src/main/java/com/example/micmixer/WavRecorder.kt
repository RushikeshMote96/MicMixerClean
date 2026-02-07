package com.example.micmixer

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class WavRecorder(private val file: File, private val sampleRate: Int) {
    private var out: FileOutputStream? = null
    private var totalAudioLen = 0

    fun start() {
        out = FileOutputStream(file)
        writeHeader(out!!)
    }

    fun write(data: ByteArray, len: Int) {
        out?.write(data, 0, len)
        totalAudioLen += len
    }

    fun stop() {
        out?.close()
        val raf = RandomAccessFile(file, "rw")
        raf.seek(4)
        raf.writeIntLE(totalAudioLen + 36)
        raf.seek(40)
        raf.writeIntLE(totalAudioLen)
        raf.close()
    }

    private fun writeHeader(out: FileOutputStream) {
        val header = ByteArray(44)
        header[0]='R'.code.toByte();header[1]='I'.code.toByte();header[2]='F'.code.toByte();header[3]='F'.code.toByte()
        header[8]='W'.code.toByte();header[9]='A'.code.toByte();header[10]='V'.code.toByte();header[11]='E'.code.toByte()
        header[12]='f'.code.toByte();header[13]='m'.code.toByte();header[14]='t'.code.toByte();header[15]=' '.code.toByte()
        header[16]=16; header[20]=1; header[22]=1
        out.write(header,0,44)
    }

    private fun RandomAccessFile.writeIntLE(v:Int){
        write(byteArrayOf((v and 0xff).toByte(),((v shr 8)and 0xff).toByte(),((v shr 16)and 0xff).toByte(),((v shr 24)and 0xff).toByte()))
    }
}
