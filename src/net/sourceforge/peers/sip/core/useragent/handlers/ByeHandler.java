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

package net.sourceforge.peers.sip.core.useragent.handlers;

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.MidDialogRequestManager;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
import net.sourceforge.peers.sip.transaction.ServerTransaction;
import net.sourceforge.peers.sip.transaction.ServerTransactionUser;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;

public class ByeHandler extends DialogMethodHandler
        implements ServerTransactionUser {

    ////////////////////////////////////////////////
    // methods for UAC
    ////////////////////////////////////////////////
    
    public void preprocessBye(SipRequest sipRequest, Dialog dialog) {

        // 15.1.1
        
        String addrSpec = sipRequest.getRequestUri().toString();
        UserAgent.getInstance().getPeers().remove(addrSpec);
        
        dialog.receivedOrSentBye();
        
        DialogManager.getInstance().removeDialog(dialog.getId());
        Logger.getInstance().debug("removed dialog " + dialog.getId());
    }
    
    

    
    
    
    ////////////////////////////////////////////////
    // methods for UAS
    ////////////////////////////////////////////////
    
    public void handleBye(SipRequest sipRequest, Dialog dialog) {
        dialog.receivedOrSentBye();
        //String remoteUri = dialog.getRemoteUri();

        String addrSpec = sipRequest.getRequestUri().toString();
        UserAgent.getInstance().getPeers().remove(addrSpec);
        DialogManager.getInstance().removeDialog(dialog.getId());
        Logger.getInstance().debug("removed dialog " + dialog.getId());
        UserAgent.getInstance().getCaptureRtpSender().stop();
        UserAgent.getInstance().setCaptureRtpSender(null);
        
        SipResponse sipResponse =
            MidDialogRequestManager.generateMidDialogResponse(
                    sipRequest,
                    dialog,
                    RFC3261.CODE_200_OK,
                    RFC3261.REASON_200_OK);
        
        // TODO determine port and transport for server transaction>transport
        // from initial invite
        // FIXME determine port and transport for server transaction>transport
        ServerTransaction serverTransaction =
            TransactionManager.getInstance().createServerTransaction(
                    sipResponse,
                    Utils.getInstance().getSipPort(),
                    RFC3261.TRANSPORT_UDP,
                    this,
                    sipRequest);
        
        serverTransaction.start();
        
        serverTransaction.receivedRequest(sipRequest);
        
        serverTransaction.sendReponse(sipResponse);
        
        DialogManager.getInstance().removeDialog(dialog.getId());
        
//        setChanged();
//        notifyObservers(sipRequest);
    }

    ///////////////////////////////////////
    //ServerTransactionUser methods
    ///////////////////////////////////////
    public void transactionFailure() {
        // TODO Auto-generated method stub
        
    }


    
    
}