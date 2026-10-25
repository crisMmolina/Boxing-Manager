package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void rechazaLlaveSecp256k1() throws Exception {
        // SubjectPublicKeyInfo de secp256k1 con su punto generador G (el JDK 21 ya no genera llaves de esa curva).
        String secp256k1 = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEeb5mfvncu6xVoGKVzocLBwKb/NstzijZWfKBWxb4F5hIOtp3JqPEZV2k+/wOEQio/Re0SKaFVBmcR9CP+xDUuA==";
        KeyPair ok = keyPair("secp256r1");
        assertFalse(DeviceProof.verify(secp256k1, "x", DeviceProof.sign(ok.getPrivate(), "x")));
    }

    @Test
    void firmaDeJoinNoSirveComoHelloNiConOtroNonce() throws Exception {
        KeyPair kp = keyPair("secp256r1");
        String pub = DeviceProof.encodePublicKey(kp.getPublic());
        String nonce = DeviceProof.newNonce(new SecureRandom());
        String joinSig = DeviceProof.sign(kp.getPrivate(), DeviceProof.joinText("f", 1, "123456", "dev", nonce));
        assertFalse(DeviceProof.verify(pub, DeviceProof.helloText("f", 1, "dev", nonce), joinSig));
        assertFalse(DeviceProof.verify(pub,
                DeviceProof.joinText("f", 1, "123456", "dev", DeviceProof.newNonce(new SecureRandom())), joinSig));
    }

    @Test
    void textosRechazanCamposConSaltoDeLinea() {
        String nonce = "n".repeat(22);
        assertThrows(IllegalArgumentException.class, () -> DeviceProof.helloText("f\n1", 1, "dev", nonce));
        assertThrows(IllegalArgumentException.class, () -> DeviceProof.joinText("f", 1, "123456\n", "dev", nonce));
        assertThrows(IllegalArgumentException.class, () -> DeviceProof.helloText("f", 1, "dev", "corto"));
        assertThrows(IllegalArgumentException.class, () -> DeviceProof.helloText("f", 9, "dev", nonce));
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
