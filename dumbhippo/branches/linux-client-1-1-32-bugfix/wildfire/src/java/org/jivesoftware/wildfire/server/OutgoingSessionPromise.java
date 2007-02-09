/**
 * $RCSfile: $
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.server;

import org.jivesoftware.wildfire.*;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.xmpp.packet.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An OutgoingSessionPromise provides an asynchronic way for sending packets to remote servers.
 * When looking for a route to a remote server that does not have an existing connection, a session
 * promise is returned.
 *
 * This class will queue packets and process them in another thread. The processing thread will
 * use a pool of thread that will actually do the hard work. The threads in the pool will try
 * to connect to remote servers and deliver the packets. If an error occured while establishing
 * the connection or sending the packet an error will be returned to the sender of the packet.
 *
 * @author Gaston Dombiak
 */
public class OutgoingSessionPromise implements RoutableChannelHandler {

    private static OutgoingSessionPromise instance = new OutgoingSessionPromise();

    /**
     * Queue that holds the packets pending to be sent to remote servers.
     */
    private BlockingQueue<Packet> packets = new LinkedBlockingQueue<Packet>();

    /**
     * Pool of threads that will create outgoing sessions to remote servers and send
     * the queued packets.
     */
    private ThreadPoolExecutor threadPool;

    /**
     * Flag that indicates if the process that consumed the queued packets should stop.
     */
    private boolean shutdown = false;
    private RoutingTable routingTable;

    private OutgoingSessionPromise() {
        super();
        init();
    }

    private void init() {
        routingTable = XMPPServer.getInstance().getRoutingTable();
        // Create a pool of threads that will process queued packets.
        int maxThreads = JiveGlobals.getIntProperty("xmpp.server.outgoing.threads", 20);
        if (maxThreads < 10) {
            // Ensure that the max number of threads in the pool is at least 10
            maxThreads = 10;
        }
        threadPool =
                new ThreadPoolExecutor(Math.round(maxThreads/4), maxThreads, 60, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(),
                        new ThreadPoolExecutor.DiscardOldestPolicy());

        // Start the thread that will consume the queued packets. Each pending packet will
        // be actually processed by a thread of the pool (when available). If an error occurs
        // while creating the remote session or sending the packet then a packet with error 502
        // will be sent to the sender of the packet
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!shutdown) {
                    try {
                        if (threadPool.getActiveCount() < threadPool.getMaximumPoolSize()) {
                            // Wait until a packet is available
                            final Packet packet = packets.take();
                            // Process the packet in another thread
                            threadPool.execute(new Runnable() {
                                public void run() {
                                    try {
                                        createSessionAndSendPacket(packet);
                                    }
                                    catch (Exception e) {
                                        returnErrorToSender(packet);
                                        Log.debug(
                                                "Error sending packet to remote server: " + packet,
                                                e);
                                    }
                                }
                            });
                        }
                        else {
                            // No threads are available so take a nap :)
                            Thread.sleep(200);
                        }
                    }
                    catch (InterruptedException e) {
                    }
                    catch (Exception e) {
                        Log.error(e);
                    }
                }
            }
        }, "Queued Packets Processor");
        thread.setDaemon(true);
        thread.start();

    }

    public static OutgoingSessionPromise getInstance() {
        return instance;
    }

    private void createSessionAndSendPacket(Packet packet) throws Exception {
        // Create a connection to the remote server from the domain where the packet has been sent
        boolean created = OutgoingServerSession
                .authenticateDomain(packet.getFrom().getDomain(), packet.getTo().getDomain());
        if (created) {
            // A connection to the remote server was created so get the route and send the packet
            ChannelHandler route = routingTable.getRoute(packet.getTo());
            if (route != null) {
                route.process(packet);
            }
            else {
                throw new Exception("Failed to create connection to remote server");
            }
        }
        else {
            throw new Exception("Failed to create connection to remote server");
        }
    }

    private void returnErrorToSender(Packet packet) {
        // TODO Send correct error condition: timeout or not_found depending on the real error
        try {
            if (packet instanceof IQ) {
                IQ reply = new IQ();
                reply.setID(((IQ) packet).getID());
                reply.setTo(packet.getFrom());
                reply.setFrom(packet.getTo());
                reply.setChildElement(((IQ) packet).getChildElement().createCopy());
                reply.setError(PacketError.Condition.remote_server_not_found);
                ChannelHandler route = routingTable.getRoute(reply.getTo());
                if (route != null) {
                    route.process(reply);
                }
            }
            else if (packet instanceof Presence) {
                Presence reply = new Presence();
                reply.setID(packet.getID());
                reply.setTo(packet.getFrom());
                reply.setFrom(packet.getTo());
                reply.setError(PacketError.Condition.remote_server_not_found);
                ChannelHandler route = routingTable.getRoute(reply.getTo());
                if (route != null) {
                    route.process(reply);
                }
            }
            else if (packet instanceof Message) {
                Message reply = new Message();
                reply.setID(packet.getID());
                reply.setTo(packet.getFrom());
                reply.setFrom(packet.getTo());
                reply.setType(((Message)packet).getType());
                reply.setThread(((Message)packet).getThread());
                reply.setError(PacketError.Condition.remote_server_not_found);
                ChannelHandler route = routingTable.getRoute(reply.getTo());
                if (route != null) {
                    route.process(reply);
                }
            }
        }
        catch (UnauthorizedException e) {
        }
    }

    /**
     * Shuts down the thread that consumes the queued packets and also stops the pool
     * of threads that actually send the packets to the remote servers.
     */
    public void shutdown() {
        threadPool.shutdown();
        shutdown = true;
    }

    public JID getAddress() {
        // TODO Will somebody send this message to me????
        return null;
    }

    public void process(Packet packet) {
        // Queue the packet. Another process will process the queued packets.
        packets.add(packet.createCopy());
    }
}
