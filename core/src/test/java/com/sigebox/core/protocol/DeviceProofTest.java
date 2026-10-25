package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import org.junit.jupiter.api.Test;

class DeviceProofTest {

    static KeyPair keyPair(String curve) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec(curve));
        return gen.generateKeyPair();
    }

    @Test
    void firmaYVerificaHello() throws Exception {
        KeyPair kp = keyPair("secp256r1");
        String pub = DeviceProof.encodePublicKey(kp.getPublic());
        String nonce = DeviceProof.newNonce(new SecureRandom());
        String text = DeviceProof.helloText("f-1", 2, "dev-1", nonce);
        String sig = DeviceProof.sign(kp.getPrivate(), text);
        assertTrue(DeviceProof.verify(pub, text, sig));
        assertFalse(DeviceProof.verify(pub, DeviceProof.helloText("f-1", 3, "dev-1", nonce), sig));
        assertFalse(DeviceProof.verify(pub, DeviceProof.helloText("f-1", 2, "dev-1", DeviceProof.newNonce(new SecureRandom())), sig));
    }

    @Test
    void otraLlaveNoVerifica() throws Exception {
        KeyPair a = keyPair("secp256r1");
        KeyPair b = keyPair("secp256r1");
        String text = DeviceProof.joinText("f-1", 1, "token-token-token-token", "dev", "n".repeat(22));
        assertFalse(DeviceProof.verify(DeviceProof.encodePublicKey(b.getPublic()), text, DeviceProof.sign(a.getPrivate(), text)));
    }

    @Test
    void rechazaCurvaDistintaYBasura() throws Exception {
        KeyPair p384 = keyPair("secp384r1");
        String text = "x";
        assertFalse(DeviceProof.verify(DeviceProof.encodePublicKey(p384.getPublic()), text, DeviceProof.sign(p384.getPrivate(), text)));
        KeyPair ok = keyPair("secp256r1");
        String pub = DeviceProof.encodePublicKey(ok.getPublic());
        assertFalse(DeviceProof.verify("no-base64!!", text, DeviceProof.sign(ok.getPrivate(), text)));
        assertFalse(DeviceProof.verify(pub, text, "AAAA"));
        assertFalse(DeviceProof.verify(pub, text, "no-base64!!"));
    }

    @Test
    void nonceDe128BitsEnBase64Url() {
        String a = DeviceProof.newNonce(new SecureRandom());
        assertEquals(22, a.length());
        assertTrue(a.matches("[A-Za-z0-9_-]{22}"), a);
        assertNotEquals(a, DeviceProof.newNonce(new SecureRandom()));
    }

    @Test
    void lasFirmasDeLosEjemplosVerificanConLaLlaveDelJoin() {
        Join join = (Join) Messages.decode(ProtocolSchemaTest.fixture(MessageType.JOIN).toString());
        Hello hello = (Hello) Messages.decode(ProtocolSchemaTest.fixture(MessageType.HELLO).toString());
        Challenge challenge = (Challenge) Messages.decode(ProtocolSchemaTest.fixture(MessageType.CHALLENGE).toString());
        SubmitFinalCard fin = (SubmitFinalCard) Messages.decode(ProtocolSchemaTest.fixture(MessageType.SUBMIT_FINAL_CARD).toString());
        assertEquals(challenge.nonce(), join.nonce());
        assertTrue(DeviceProof.verify(join.publicKey(),
                DeviceProof.joinText(join.fightId(), join.seat(), join.token(), join.deviceId(), join.nonce()), join.sig()));
        assertTrue(DeviceProof.verify(join.publicKey(),
                DeviceProof.helloText(hello.fightId(), hello.seat(), hello.deviceId(), hello.nonce()), hello.sig()));
        assertTrue(DeviceProof.verify(join.publicKey(), DeviceProof.finalCardText(fin.fightId(), fin.seat(),
                FinalCardHashTest.JUEZ_2), fin.sig()));
        assertEquals(FinalCardHash.of(fin.fightId(), fin.seat(), FinalCardHashTest.JUEZ_2), fin.hash());
    }

    @Test
    void huellaEsSha256DeLaLlavePublica() {
        Join join = (Join) Messages.decode(ProtocolSchemaTest.fixture(MessageType.JOIN).toString());
        assertTrue(DeviceProof.fingerprint(join.publicKey()).matches("[0-9a-f]{64}"));
        assertEquals(FinalCardHash.sha256Hex(java.util.Base64.getDecoder().decode(join.publicKey())),
                DeviceProof.fingerprint(join.publicKey()));
    }
}
