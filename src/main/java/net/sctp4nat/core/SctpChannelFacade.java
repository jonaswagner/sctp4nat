/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.core;

import java.net.InetSocketAddress;
import org.jdeferred.Promise;

import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpSocket;
import net.sctp4nat.origin.SctpSocket.NotificationListener;

/**
 * @author jonaswagner
 * 
 *         This is the user interface for using a {@link SctpChannel}. Since
 *         sctp4nat implements the facade design pattern, a user should not cast
 *         an instance of {@link SctpChannelFacade}.
 */
public interface SctpChannelFacade {

	/**
	 * This method sends data to the connected endpoint. The method is non-blocking
	 * and returns a {@link Promise} object, which fires a callback once send is
	 * executed.
	 * 
	 * @param data
	 *            the data, which is to be sent.
	 * @param offset
	 *            the offset pointer.
	 * @param len
	 *            the length of the data
	 * @param ordered
	 *            true if the stream should send and receive packets ordered
	 * @param sid
	 *            the stream, on which the data is sent.
	 * @param ppid
	 *            the payload protocol id.
	 * @return A {@link Promise} object
	 */
	Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, boolean ordered, int sid, int ppid);

	/**
	 * This method sends data to the connected endpoint. The method is non-blocking
	 * and returns a {@link Promise} object, which fires a callback once send is
	 * executed.
	 * 
	 * @param data
	 *            the data, which is to be sent.
	 * @param ordered
	 *            true if the stream should send and receive packets ordered
	 * @param sid
	 *            the stream, on which the data is sent.
	 * @param ppid
	 *            the payload protocol id.
	 * @return A {@link Promise} object
	 */
	Promise<Integer, Exception, Object> send(byte[] data, boolean ordered, int sid, int ppid);

	/**
	 * This method sends data to the connected endpoint. The method is non-blocking
	 * and returns a {@link Promise} object, which fires a callback once send is
	 * executed.
	 * NotificationL
	 * @param data
	 *            the data, which is to be sent.
	 * @param offset
	 *            the offset pointer.
	 * @param len
	 *            the length of the data
	 * @param config
	 *            A {@link SctpDefaultStreamConfig} instance containing stream parameters.
	 * @return A {@link Promise} object
	 */
	Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, SctpDefaultStreamConfig config);

	/**
	 * This method sends data to the connected endpoint. The method is non-blocking
	 * and returns a {@link Promise} object, which fires a callback once send is
	 * executed.
	 * 
	 * @param data
	 *            the data, which is to be sent.
	 * @param config
	 *            A {@link SctpDefaultStreamConfig} instance containing stream parameters.
	 * @return A {@link Promise} object
	 */
	Promise<Integer, Exception, Object> send(byte[] data, SctpDefaultStreamConfig config);

	/**
	 * This method closes the underlying {@link SctpSocket} and releases its
	 * resources on usrsctp. Additionally, corresponding entries on
	 * {@link SctpMapper} and {@link SctpPorts} are also removed.
	 * 
	 * @return
	 */
	Promise<Object, Exception, Object> close();

	/**
	 * Sets the {@link SctpDataCallback}, which is called whenever a SCTP
	 * arrived.
	 * 
	 * @param cb
	 * 			A {@link SctpDataCallback} instance
	 */
	void setSctpDataCallback(SctpDataCallback cb);

	/**
	 * @return
	 * 			the {@link InetSocketAddress} of the remote endpoint
	 */
	InetSocketAddress getRemote();

	/**
	 * This method initializes the SHUTDOWN INIT sequence on usrsctp. After calling
	 * this method, a user should call close() to release the assigned resources on usrsctp.
	 */
	void shutdownInit();

	/**
	 * Replaces the {@link NotificationListener} with an own {@link NotificationListener} instance.
	 * 
	 * @param listener
	 * 				A {@link NotificationListener} instance.
	 */
	void setNotificationListener(NotificationListener listener);

}
