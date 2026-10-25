package com.sigebox.juez.data;

import com.sigebox.core.protocol.JoinUri;

/** El lugar de juez guardado: se escribe al unirse y se marca aprobado con {@code JOIN_APPROVED}. */
public final class SessionStore {
    private final SessionDao dao;

    public SessionStore(SessionDao dao) {
        this.dao = dao;
    }

    /** @return el lugar guardado o {@code null} si el teléfono no se ha unido */
    public SessionEntity load() {
        return dao.load();
    }

    public void saveJoin(JoinUri uri, String judgeName) {
        save(uri.host(), uri.port(), uri.fightId(), uri.seat(), uri.token(), judgeName, uri.certFingerprint());
    }

    public void saveJoin(String host, int port, String fightId, int seat, String token, String judgeName) {
        save(host, port, fightId, seat, token, judgeName, null);
    }

    private void save(String host, int port, String fightId, int seat, String token, String judgeName,
            String certFingerprint) {
        SessionEntity session = new SessionEntity();
        session.host = host;
        session.port = port;
        session.fightId = fightId;
        session.seat = seat;
        session.token = token;
        session.judgeName = judgeName;
        session.certFingerprint = certFingerprint;
        session.approved = false;
        dao.save(session);
    }

    public void markApproved() {
        dao.markApproved();
    }

    public void clear() {
        dao.clear();
    }
}
