package dk.youtec.pos.ecr

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPort.NO_PARITY
import com.fazecast.jSerialComm.SerialPort.ONE_STOP_BIT
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import java.io.IOException
import kotlin.RuntimeException
import kotlin.experimental.xor

/**
 * For simulating an Electronic Cash Register and controlling the Verifone Nordic payment terminal.
 */
class PointFinlandEcrController {

    private val stx: Byte = 0x2
    private val etx: Byte = 0x3
    private val enq: Byte = 0x5
    private val ack: Byte = 0x6

    private var comPort: SerialPort
    private var purchaseInitiated: Boolean = false
    private var amountCents: Int = 0
    private val verboseLogging = true

    init {
        val commPorts = SerialPort.getCommPorts()
        comPort = commPorts.firstOrNull { it.systemPortName.startsWith("tty.usbmodem") }
        ?: throw RuntimeException("No terminal found")

        comPort.setComPortParameters(19200, 8, ONE_STOP_BIT, NO_PARITY)
        if (comPort.openPort()) {
            println("Opened com port ${comPort.systemPortName}")

            comPort.addDataListener(object : SerialPortDataListener {
                override fun serialEvent(event: SerialPortEvent) {
                    if (event.eventType != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return

                    val newData = ByteArray(comPort.bytesAvailable())
                    val numRead = comPort.readBytes(newData, newData.size.toLong())
                    if (verboseLogging) println("Read $numRead byte(s): ${newData.asList()}")
                    sendAck()

                    if (newData.lastOrNull() == ack) {
                        println("Got ack")
                        if (!purchaseInitiated) {
                            sendPurchaseInitiate(amountCents)
                        }
                    }
                }

                override fun getListeningEvents() = SerialPort.LISTENING_EVENT_DATA_AVAILABLE
            })
        } else {
            println("Unable to open com port")
            throw IOException("Unable to open com port")
        }
    }

    fun initiatePurchase(amountCents: Int) {
        this.amountCents = amountCents
        purchaseInitiated = false
        writeBytes(byteArrayOf(enq))
    }

    private fun sendAck() {
        writeBytes(byteArrayOf(ack))
    }

    private fun sendPurchaseInitiate(amountCents: Int) {
        val amountPadded = "$amountCents".padStart(7, '0')
        val stx = byteArrayOf(stx)
        val dataAndEtx = byteArrayOf(0x58, 0x30) + amountPadded.toByteArray() +
                byteArrayOf(0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
                        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x1c, 0x30, 0x30, 0x30, 0x30, 0x30,
                        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
                        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x46, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
                        0x30, 0x30, 0x30, 0x30, etx
                )

        println("Sending: " + String(dataAndEtx))

        writeBytes(stx)
        writeBytes(dataAndEtx)

        val lrc = calculateLrc(dataAndEtx)
        if (verboseLogging) println("Calculated LRC is ${lrc.firstOrNull()}")

        writeBytes(lrc)

        purchaseInitiated = true
        println("Purchase initiate send!")
    }

    fun sendStop() {
        println("Sending stop")

        val stx = byteArrayOf(stx)
        val dataAndEtx = byteArrayOf(0x37, 0x32, etx)

        writeBytes(stx)
        writeBytes(dataAndEtx)

        val lrc = calculateLrc(dataAndEtx)
        if (verboseLogging) println("Calculated LRC is ${lrc.firstOrNull()}")

        writeBytes(lrc)
    }

    /**
     * Sends the ReadersControl message with the "CloseReaders" Control command.
     */
    fun sendCancel() {
        println("Sending cancel")

        val stx = byteArrayOf(stx)
        val dataAndEtx = "o0                         ".toByteArray() + byteArrayOf(etx)

        writeBytes(stx)
        writeBytes(dataAndEtx)

        val lrc = calculateLrc(dataAndEtx)
        if (verboseLogging) println("Calculated LRC is ${lrc.firstOrNull()}")

        writeBytes(lrc)
    }

    private fun writeBytes(data: ByteArray) {
        if (comPort.isOpen) {
            val bytesWritten = comPort.writeBytes(data, data.size.toLong())
            if (bytesWritten == -1) {
                println("Unable to write message")
                throw IOException("Unable to write message")
            }
        } else {
            throw IOException("Com port not open")
        }
    }

    private fun calculateLrc(data: ByteArray): ByteArray {
        return byteArrayOf(
                data.fold(0.toByte()) { accumulated, current ->
                    accumulated xor current
                })
    }

    fun close() {
        comPort.closePort()
    }
}