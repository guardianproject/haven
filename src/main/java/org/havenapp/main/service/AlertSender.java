package org.havenapp.main.service;

import java.util.ArrayList;

public interface AlertSender {

    public void setUsername (String username);

    public void reset ();

    public void register (boolean callEnabled);

    public void verify (final String verificationCode);

    public void stopHeartbeatTimer ();

    public void startHeartbeatTimer (int countMs);

    public void sendMessage (final ArrayList<String> recipients, final String message, final String attachment);

}
