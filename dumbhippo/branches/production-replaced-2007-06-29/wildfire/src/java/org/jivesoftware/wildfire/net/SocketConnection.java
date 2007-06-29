/**
 * $RCSfile$
 * $Revision: 3187 $
 * $Date: 2005-12-11 13:34:34 -0300 (Sun, 11 Dec 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.net;

import org.jivesoftware.wildfire.*;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.interceptor.InterceptorManager;
import org.jivesoftware.wildfire.interceptor.PacketRejectedException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;
import org.xmpp.packet.Packet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

/**
 * An object to track the state of a XMPP client-server session.
 * Currently this class contains the socket channel connecting the
 * client and server.
 *
 * @author Iain Shigeoka
 */
public class SocketConnection implements Connection {

    /**
     * The utf-8 charset for decoding and encoding XMPP packet streams.
     */
    public static final String CHARSET = "UTF-8";

    private static Map<SocketConnection, String> instances =
            new ConcurrentHashMap<SocketConnection, String>();

    /**
     * Milliseconds a connection has to be idle to be closed. Timeout is disabled by default. It's
     * up to the connection's owner to configure the timeout value. Sending stanzas to the client
     * is not considered as activity. We are only considering the connection active when the
     * client sends some data or hearbeats (i.e. whitespaces) to the server.
     * The reason for this is that sending data will fail if the connection is closed. And if
     * the thread is blocked while sending data (because the socket is closed) then the clean up
     * thread will close the socket anyway.
     */
    private long idleTimeout = -1;

    private Map<ConnectionCloseListener, Object> listeners = new HashMap<ConnectionCloseListener, Object>();

    private Socket socket;
    private SocketReader socketReader;

    private Writer writer;

    private PacketDeliverer deliverer;

    private Session session;
    private boolean secure;
	private boolean compressed;
    private org.jivesoftware.util.XMLWriter xmlSerializer;
    private boolean flashClient = false;
    private int majorVersion = 1;
    private int minorVersion = 0;
    private String language = null;
    private TLSStreamHandler tlsStreamHandler;

    private long writeStarted = -1;

    /**
     * TLS policy currently in use for this connection.
     */
    private TLSPolicy tlsPolicy = TLSPolicy.optional;

	/**
	 * Compression policy currently in use for this connection.
	 */
	private CompressionPolicy compressionPolicy = CompressionPolicy.disabled;

    public static Collection<SocketConnection> getInstances() {
        return instances.keySet();
    }

    /**
     * Create a new session using the supplied socket.
     *
     * @param deliverer the packet deliverer this connection will use.
     * @param socket the socket to represent.
     * @param isSecure true if this is a secure connection.
     * @throws NullPointerException if the socket is null.
     */
    public SocketConnection(PacketDeliverer deliverer, Socket socket, boolean isSecure)
            throws IOException {
        if (socket == null) {
            throw new NullPointerException("Socket channel must be non-null");
        }

        this.secure = isSecure;
        this.socket = socket;
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET));
        this.deliverer = deliverer;
        xmlSerializer = new XMLSocketWriter(writer, this);

        instances.put(this, "");
    }

    /**
     * Returns the stream handler responsible for securing the plain connection and providing
     * the corresponding input and output streams.
     *
     * @return the stream handler responsible for securing the plain connection and providing
     *         the corresponding input and output streams.
     */
    public TLSStreamHandler getTLSStreamHandler() {
        return tlsStreamHandler;
    }

    /**
     * Secures the plain connection by negotiating TLS with the client.
     *
     * @param clientMode boolean indicating if this entity is a client or a server.
     * @throws IOException if an error occured while securing the connection.
     */
    public void startTLS(boolean clientMode) throws IOException {
        if (!secure) {
            secure = true;
            tlsStreamHandler = new TLSStreamHandler(socket, clientMode);
            writer = new BufferedWriter(new OutputStreamWriter(tlsStreamHandler.getOutputStream(), CHARSET));
            xmlSerializer = new XMLSocketWriter(writer, this);
        }
    }

    /**
     * Start using compression for this connection. Compression will only be available after TLS
     * has been negotiated. This means that a connection can never be using compression before
     * TLS. However, it is possible to use compression without TLS.
     *
     * @throws IOException if an error occured while starting compression.
     */
    public void startCompression() throws IOException {
        compressed = true;

        if (tlsStreamHandler == null) {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new ZipOutputStream(socket.getOutputStream()), CHARSET));
        }
        else {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new ZipOutputStream(tlsStreamHandler.getOutputStream()), CHARSET));
        }
    }

    public boolean validate() {
        if (isClosed()) {
            return false;
        }
        try {
            synchronized (writer) {
                // Register that we started sending data on the connection
                writeStarted();
                writer.write(" ");
                writer.flush();
            }
        }
        catch (Exception e) {
            Log.warn("Closing no longer valid connection" + "\n" + this.toString(), e);
            close();
        }
        finally {
            // Register that we finished sending data on the connection
            writeFinished();
        }
        return !isClosed();
    }

    public void init(Session owner) {
        session = owner;
    }

    public Object registerCloseListener(ConnectionCloseListener listener, Object handbackMessage) {
        Object status = null;
        if (isClosed()) {
            listener.onConnectionClose(handbackMessage);
        }
        else {
            status = listeners.put(listener, handbackMessage);
        }
        return status;
    }

    public Object removeCloseListener(ConnectionCloseListener listener) {
        return listeners.remove(listener);
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public Writer getWriter() {
        return writer;
    }

    public boolean isClosed() {
        if (session == null) {
            return socket.isClosed();
        }
        return session.getStatus() == Session.STATUS_CLOSED;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public TLSPolicy getTlsPolicy() {
        return tlsPolicy;
    }

    public void setTlsPolicy(TLSPolicy tlsPolicy) {
        this.tlsPolicy = tlsPolicy;
    }

    public CompressionPolicy getCompressionPolicy() {
        return compressionPolicy;
    }

    public void setCompressionPolicy(CompressionPolicy compressionPolicy) {
        this.compressionPolicy = compressionPolicy;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long timeout) {
        this.idleTimeout = timeout;
    }

    public int getMajorXMPPVersion() {
        return majorVersion;
    }

    public int getMinorXMPPVersion() {
        return minorVersion;
    }

    /**
     * Sets the XMPP version information. In most cases, the version should be "1.0".
     * However, older clients using the "Jabber" protocol do not set a version. In that
     * case, the version is "0.0".
     *
     * @param majorVersion the major version.
     * @param minorVersion the minor version.
     */
    public void setXMPPVersion(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language code that should be used for this connection (e.g. "en").
     *
     * @param language the language code.
     */
    public void setLanaguage(String language) {
        this.language = language;
    }

    public boolean isFlashClient() {
        return flashClient;
    }

    /**
     * Sets whether the connected client is a flash client. Flash clients need to
     * receive a special character (i.e. \0) at the end of each xml packet. Flash
     * clients may send the character \0 in incoming packets and may start a
     * connection using another openning tag such as: "flash:client".
     *
     * @param flashClient true if the if the connection is a flash client.
     */
    public void setFlashClient(boolean flashClient) {
        this.flashClient = flashClient;
    }

    public void close() {
        boolean wasClosed = false;
        synchronized (this) {
            if (!isClosed()) {
                try {
                    if (session != null) {
                        session.setStatus(Session.STATUS_CLOSED);
                    }
                    synchronized (writer) {
                        try {
                            // Register that we started sending data on the connection
                            writeStarted();
                            writer.write("</stream:stream>");
                            if (flashClient) {
                                writer.write('\0');
                            }
                            writer.flush();
                        }
                        catch (IOException e) {}
                        finally {
                            // Register that we finished sending data on the connection
                            writeFinished();
                        }
                    }
                }
                catch (Exception e) {
                    Log.error(LocaleUtils.getLocalizedString("admin.error.close")
                            + "\n" + this.toString(), e);
                }
                closeConnection();
                wasClosed = true;
            }
        }
        if (wasClosed) {
            notifyCloseListeners();
        }
    }

    void writeStarted() {
        writeStarted = System.currentTimeMillis();
    }

    void writeFinished() {
        writeStarted = -1;
    }

    void checkHealth() {
        // Check that the sending operation is still active
        if (writeStarted > -1 && System.currentTimeMillis() - writeStarted >
                JiveGlobals.getIntProperty("xmpp.session.sending-limit", 60000)) {
            // Close the socket
            if (Log.isDebugEnabled()) {
                Log.debug("Closing connection: " + this + " that started sending data at: " +
                        new Date(writeStarted));
            }
            forceClose();
        }
        else {
            // Check if the connection has been idle. A connection is considered idle if the client
            // has not been receiving data for a period. Sending data to the client is not
            // considered as activity.
            if (idleTimeout > -1 && socketReader != null &&
                    System.currentTimeMillis() - socketReader.getLastActive() > idleTimeout) {
                // Close the socket
                if (Log.isDebugEnabled()) {
                    Log.debug("Closing connection that has been idle: " + this);
                }
                forceClose();
            }
        }
    }

    void release() {
        writeStarted = -1;
        instances.remove(this);
    }

    /**
     * Forces the connection to be closed immediately no matter if closing the socket takes
     * a long time. This method should only be called from {@link SocketSendingTracker} when
     * sending data over the socket has taken a long time and we need to close the socket, discard
     * the connection and its session.
     */
    private void forceClose() {
        if (session != null) {
            // Set that the session is closed. This will prevent threads from trying to
            // deliver packets to this session thus preventing future locks.
            session.setStatus(Session.STATUS_CLOSED);
        }
        closeConnection();
        // Notify the close listeners so that the SessionManager can send unavailable
        // presences if required.
        notifyCloseListeners();
    }

    private void closeConnection() {
        try {
            if (tlsStreamHandler == null) {
                socket.close();
            }
            else {
                // Close the channels since we are using TLS (i.e. NIO). If the channels implement
                // the InterruptibleChannel interface then any other thread that was blocked in
                // an I/O operation will be interrupted and an exception thrown
                tlsStreamHandler.close();
            }
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error.close")
                    + "\n" + this.toString(), e);
        }
    }

    public void deliver(Packet packet) throws UnauthorizedException, PacketException {
        if (isClosed()) {
            deliverer.deliver(packet);
        }
        else {
            try {
                // Invoke the interceptors before we send the packet
                InterceptorManager.getInstance().invokeInterceptors(packet, session, false, false);
                boolean errorDelivering = false;
                synchronized (writer) {
                    try {
                        xmlSerializer.write(packet.getElement());
                        if (flashClient) {
                            writer.write('\0');
                        }
                        xmlSerializer.flush();
                    }
                    catch (IOException e) {
                        Log.debug("Error delivering packet" + "\n" + this.toString(), e);
                        errorDelivering = true;
                    }
                }
                if (errorDelivering) {
                    close();
                    // Retry sending the packet again. Most probably if the packet is a
                    // Message it will be stored offline
                    deliverer.deliver(packet);
                }
                else {
                    // Invoke the interceptors after we have sent the packet
                    InterceptorManager.getInstance().invokeInterceptors(packet, session, false, true);
                    session.incrementServerPacketCount();
                }
            }
            catch (PacketRejectedException e) {
                // An interceptor rejected the packet so do nothing
            }
        }
    }

    public void deliverRawText(String text) {
        if (!isClosed()) {
            boolean errorDelivering = false;
            synchronized (writer) {
                try {
                    // Register that we started sending data on the connection
                    writeStarted();
                    writer.write(text);
                    if (flashClient) {
                        writer.write('\0');
                    }
                    writer.flush();
                }
                catch (IOException e) {
                    Log.debug("Error delivering raw text" + "\n" + this.toString(), e);
                    errorDelivering = true;
                }
                finally {
                    // Register that we finished sending data on the connection
                    writeFinished();
                }
            }
            if (errorDelivering) {
                close();
            }
        }
    }

    /**
     * Notifies all close listeners that the connection has been closed.
     * Used by subclasses to properly finish closing the connection.
     */
    private void notifyCloseListeners() {
        synchronized (listeners) {
            for (ConnectionCloseListener listener : listeners.keySet()) {
                try {
                    listener.onConnectionClose(listeners.get(listener));
                }
                catch (Exception e) {
                    Log.error("Error notifying listener: " + listener, e);
                }
            }
        }
    }

    public String toString() {
        return super.toString() + " socket: " + socket + " session: " + session;
    }

    public void setSocketReader(SocketReader socketReader) {
        this.socketReader = socketReader;
    }
}