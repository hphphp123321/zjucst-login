package com.example.zjulogin

import android.net.Network
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

class NetworkBoundSocketFactory(private val network: Network) : SocketFactory() {
    @Throws(IOException::class)
    override fun createSocket(): Socket {
        val socket = Socket()
        network.bindSocket(socket)
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val socket = Socket(InetAddress.getByName(host), port)
        network.bindSocket(socket)
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        val socket = Socket(InetAddress.getByName(host), port, localHost, localPort)
        network.bindSocket(socket)
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        val socket = Socket(host, port)
        network.bindSocket(socket)
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        val socket = Socket(address, port, localAddress, localPort)
        network.bindSocket(socket)
        return socket
    }
}
