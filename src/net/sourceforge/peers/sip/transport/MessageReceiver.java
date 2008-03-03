/*
    This file is part of Peers.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008 Yohann Martineau 
*/

package net.sourceforge.peers.sip.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.UAS;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipParserException;
import net.sourceforge.peers.sip.transaction.ClientTransaction;
import net.sourceforge.peers.sip.transaction.ServerTransaction;
import net.sourceforge.peers.sip.transaction.TransactionManager;

public abstract class MessageReceiver implements Runnable {

    public static final int BUFFER_SIZE = 2048;//FIXME should correspond to MTU 1024;
    public static final String CHARACTER_ENCODING = "US-ASCII";
    
    protected int port;
    private boolean isListening;

    public MessageReceiver(int port) {
        super();
        this.port = port;
        isListening = true;
    }
    
    public void run() {
        while (isListening) {
            try {
                listen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract void listen() throws IOException;
    
    protected boolean isRequest(byte[] message) {
        String beginning = null;
        try {
            beginning = new String(message, 0,
                    RFC3261.DEFAULT_SIP_VERSION.length(), CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (RFC3261.DEFAULT_SIP_VERSION.equals(beginning)) {
            return false;
        }
        return true;
    }
    
    protected void processMessage(byte[] message, InetAddress sourceIp)
            throws IOException {
        SipMessage sipMessage = null;
        try {
            sipMessage = TransportManager.getInstance().sipParser
                    .parse(new ByteArrayInputStream(message));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SipParserException e) {
            e.printStackTrace();
        }
        if (sipMessage == null) {
            return;
        }
        TransactionManager transactionManager = TransactionManager.getInstance();
        if (sipMessage instanceof SipRequest) {
            SipRequest sipRequest = (SipRequest)sipMessage;
            
            
            SipHeaderFieldValue topVia = Utils.getInstance().getTopVia(sipRequest);
            String sentBy =
                topVia.getParam(new SipHeaderParamName(RFC3261.PARAM_SENTBY));
            if (sentBy != null) {
                int colonPos = sentBy.indexOf(RFC3261.TRANSPORT_PORT_SEP);
                if (colonPos < 0) {
                    colonPos = sentBy.length();
                }
                sentBy = sentBy.substring(0, colonPos);
            }
            if (InetAddress.getByName(sentBy).equals(sourceIp)) {
                topVia.addParam(new SipHeaderParamName(RFC3261.PARAM_RECEIVED),
                        sourceIp.getHostAddress());
            }
            
            
            
            ServerTransaction serverTransaction =
                transactionManager.getServerTransaction(sipRequest);
            if (serverTransaction == null) {
                UAS.getInstance().messageReceived(sipMessage);
            } else {
                serverTransaction.receivedRequest(sipRequest);
            }
        } else {
            SipResponse sipResponse = (SipResponse)sipMessage;
            ClientTransaction clientTransaction =
                transactionManager.getClientTransaction(sipResponse);
            if (clientTransaction == null) {
                UAS.getInstance().messageReceived(sipMessage);
            } else {
                clientTransaction.receivedResponse(sipResponse);
            }
        }
    }
    
    public synchronized void setListening(boolean isListening) {
        this.isListening = isListening;
    }

    public synchronized boolean isListening() {
        return isListening;
    }
    
}