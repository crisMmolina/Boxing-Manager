package com.sigebox.juez;

import static org.junit.Assert.assertEquals;

import com.sigebox.juez.data.SessionEntity;
import com.sigebox.juez.join.JoinActivity;
import com.sigebox.juez.join.WaitingActivity;
import com.sigebox.juez.score.ScoreActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class MainActivityTest {

    private static SessionEntity session(boolean approved) {
        SessionEntity s = new SessionEntity();
        s.host = "192.168.43.10";
        s.port = 8765;
        s.fightId = "F1";
        s.seat = 2;
        s.token = "tok123";
        s.judgeName = "María López";
        s.approved = approved;
        return s;
    }

    @Test
    public void lugarAprobadoVuelveALaPuntuacion() {
        assertEquals(ScoreActivity.class, MainActivity.routeFor(session(true)));
    }

    @Test
    public void lugarPendienteVuelveALaEspera() {
        assertEquals(WaitingActivity.class, MainActivity.routeFor(session(false)));
    }

    @Test
    public void sinLugarVaAUnirse() {
        assertEquals(JoinActivity.class, MainActivity.routeFor(null));
    }
}
