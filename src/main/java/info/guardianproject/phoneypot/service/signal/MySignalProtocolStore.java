package info.guardianproject.phoneypot.service.signal;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.util.List;

/**
 * Created by n8fr8 on 11/1/17.
 */

public class MySignalProtocolStore implements SignalProtocolStore {
    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return null;
    }

    @Override
    public int getLocalRegistrationId() {
        return 0;
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        return false;
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
        return false;
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        return null;
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {

    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return false;
    }

    @Override
    public void removePreKey(int preKeyId) {

    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        return null;
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        return null;
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {

    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        return false;
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {

    }

    @Override
    public void deleteAllSessions(String name) {

    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        return null;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        return null;
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {

    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        return false;
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {

    }
}
