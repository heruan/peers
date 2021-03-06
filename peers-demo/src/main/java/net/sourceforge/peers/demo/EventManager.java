package net.sourceforge.peers.demo;

import java.net.SocketException;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.media.AbstractSoundManagerFactory;
import net.sourceforge.peers.media.javaxsound.JavaxSoundManager;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.rtp.RFC4733;
import net.sourceforge.peers.sip.core.useragent.SipListener;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;

public class EventManager implements SipListener, AbstractSoundManagerFactory {

    private UserAgent userAgent;
    private SipRequest sipRequest;
    private CommandsReader commandsReader;
    private AbstractSoundManager soundManager;
    
    public EventManager() throws SocketException {
        Config config = new CustomConfig();
        Logger logger = new FileLogger(null);
        soundManager = new JavaxSoundManager(false, logger, null);
        userAgent = new UserAgent(this, config, logger);
        new Thread() {
            public void run() {
                try {
                    userAgent.register();
                } catch (SipUriSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        commandsReader = new CommandsReader(this);
        commandsReader.start();
    }
    
    
    // commands methods
    public void call(final String callee) {
        new Thread() {
            @Override
            public void run() {
                try {
                    sipRequest = userAgent.invite(callee, null);
                } catch (SipUriSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    
    public void hangup() {
        new Thread() {
            @Override
            public void run() {
                userAgent.terminate(sipRequest);
            }
        }.start();
    }
    
    
    // SipListener methods
    
    @Override
    public void registering(SipRequest sipRequest) { }

    @Override
    public void registerSuccessful(SipResponse sipResponse) { }

    @Override
    public void registerFailed(SipResponse sipResponse) { }

    @Override
    public void incomingCall(SipRequest sipRequest, SipResponse provResponse) { }

    @Override
    public void remoteHangup(SipRequest sipRequest) { }

    @Override
    public void ringing(SipResponse sipResponse) { }

    @Override
    public void calleePickup(SipResponse sipResponse) { }

    @Override
    public AbstractSoundManager getSoundManager() {
        return soundManager;
    }

    @Override
    public void error(SipResponse sipResponse) { }

    @Override
    public void dtmfEvent(RFC4733.DTMFEvent dtmfEvent, int duration) {
        //TODO implement
    }

    public static void main(String[] args) {
        try {
            new EventManager();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
