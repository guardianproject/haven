package info.guardianproject.phoneypot.service.signal;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.IOException;
import java.util.List;

/**
 * Created by n8fr8 on 10/31/17.
 */

public class SignalSender {

    private final String     URL         = "https://my.signal.server.com";
    private final TrustStore TRUST_STORE = null;//new MyTrustStoreImpl();
    private final String     USERNAME    = "+14151231234";
    private final String     PASSWORD    = null;//generateRandomPassword();
    private final String     USER_AGENT  = "[FILL_IN]";

    private SignalServiceAccountManager accountManager;

    public SignalSender ()
    {
        accountManager = null; //new SignalServiceAccountManager(URL, TRUST_STORE, USERNAME, PASSWORD, USER_AGENT);


    }
    public void generateKeys ()
    {

    }

    public void register () throws IOException {

        accountManager.requestSmsVerificationCode();
    }

    public void verify (String receivedSmsVerificationCode) throws IOException, InvalidKeyException {

     //   accountManager.verifyAccountWithCode(receivedSmsVerificationCode, generateRandomSignalingKey(),
       //         generateRandomInstallId(), false);

       int signedPreKeyId = 1;

        IdentityKeyPair identityKey        = KeyHelper.generateIdentityKeyPair();
        List<PreKeyRecord> oneTimePreKeys     = KeyHelper.generatePreKeys(0, 100);
        SignedPreKeyRecord signedPreKeyRecord = KeyHelper.generateSignedPreKey(identityKey, signedPreKeyId);

        //accountManager.setGcmId(Optional.of(GoogleCloudMessaging.getInstance(this).register(REGISTRATION_ID)));
        accountManager.setPreKeys(identityKey.getPublicKey(), signedPreKeyRecord, oneTimePreKeys);
    }

    public void sendMessage () throws IOException, UntrustedIdentityException {
        SignalServiceMessageSender messageSender = null;

        /**
        new SignalServiceMessageSender(URL, TRUST_STORE, USERNAME, PASSWORD,
                new MySignalProtocolStore(),
                USER_AGENT, Optional.absent());
         **/

        messageSender.sendMessage(new SignalServiceAddress("+14159998888"),
                SignalServiceDataMessage.newBuilder()
                        .withBody("Hello, world!")
                        .build());
    }
}
