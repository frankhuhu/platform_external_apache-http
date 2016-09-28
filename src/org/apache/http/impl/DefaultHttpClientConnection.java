/*
 * $HeadURL: http://svn.apache.org/repos/asf/httpcomponents/httpcore/trunk/module-main/src/main/java/org/apache/http/impl/DefaultHttpClientConnection.java $
 * $Revision: 561083 $
 * $Date: 2007-07-30 11:31:17 -0700 (Mon, 30 Jul 2007) $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import libcore.valera.ValeraIOManager;
import libcore.valera.ValeraUtil;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * Default implementation of a client-side HTTP connection.
 *
 * @author <a href="mailto:oleg at ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 561083 $
 * 
 * @since 4.0
 */
public class DefaultHttpClientConnection extends SocketHttpClientConnection {

    public DefaultHttpClientConnection() {
        super();
    }
    
    public void bind(
            final Socket socket, 
            final HttpParams params) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        assertNotOpen();
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(params));
        socket.setSoTimeout(HttpConnectionParams.getSoTimeout(params));
        
        int linger = HttpConnectionParams.getLinger(params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
        super.bind(socket, params);
    }
    
    /* valera begin */
    private RequestWrapper wrapRequest(
            final HttpRequest request) throws ProtocolException {
        if (request instanceof HttpEntityEnclosingRequest) {
            return new EntityEnclosingRequestWrapper(
                    (HttpEntityEnclosingRequest) request);
        } else {
            return new RequestWrapper(
                    request);
        }
    }
    
    // DefaultHttpClientConnection can send request here. Need to map url and connId.
    @Override
    public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
    	HttpRequest orig = request;
        RequestWrapper origWrapper = wrapRequest(orig);
        String addr = this.socket.getInetAddress().toString();
        
    	int connId = ValeraIOManager.getInstance().getUniqueConnId();
        ValeraIOManager.getInstance().establishConnectionMap(connId, origWrapper.getMethod(),
        		origWrapper.getURI(), "NA_DHCC");
        
        ValeraUtil.valeraAssert(this.inbuffer instanceof SocketInputBuffer, 
				"DefaultHttpClientConnection's inbuffer should be SocketInputBuffer");
		SocketInputBuffer inbuffer = (SocketInputBuffer) this.inbuffer;
		InputStream socketIn = inbuffer.instream;
		ValeraIOManager.getInstance().setConnIdForSocketIn(socketIn, connId);
		
		ValeraUtil.valeraAssert(this.outbuffer instanceof SocketOutputBuffer, 
				"DefaultHttpClientConnection's outbuffer should be SocketOutputBuffer");
		SocketOutputBuffer outbuffer = (SocketOutputBuffer) this.outbuffer;
		OutputStream socketOut = outbuffer.outstream;
		ValeraIOManager.getInstance().setConnIdForSocketOut(socketOut, connId);
		
        super.sendRequestHeader(request);
    }
    /* valera end */

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        if (isOpen()) {
            buffer.append(getRemotePort());
        } else {
            buffer.append("closed");
        }
        buffer.append("]");
        return buffer.toString();
    }
    
}
