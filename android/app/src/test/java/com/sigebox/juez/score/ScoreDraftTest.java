package com.sigebox.juez.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sigebox.core.model.EstadoRound;
import com.sigebox.juez.net.ConnectionState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** AND-035 (spec 009): el puntaje a medio poner se guarda al tocarlo y vuelve tras un cierre forzado. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ScoreDraftTest {

    /** Borradores falsos: guarda lo último y cuenta los borrados. */
    static final class FakeDrafts implements ScoreViewModel.Drafts {
        final List<List<Object>> saved = new ArrayList<>();
        int cleared;

        @Override
        public void save(String fightId, int seat, int round, Integer red, Integer blue) {
            saved.add(Arrays.asList(fightId, seat, round, red, blue));
        }

        @Override
        public void clear() {
            cleared++;
        }

        List<Object> last() {
            return saved.get(saved.size() - 1);
        }
    }

    private final FakeDrafts drafts = new FakeDrafts();

    private ScoreViewModel vm(EstadoRound status) {
        ScoreViewModel vm = new ScoreViewModel(m -> { }, drafts);
        vm.setSession("F1", 2, "María López");
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 3, status, 0, null));
        return vm;
    }

    @Test
    public void tocarMasOMenosGuardaElBorrador() {
        ScoreViewModel vm = vm(EstadoRound.ABIERTO);
        vm.adjust(ScoreUi.Corner.RED, 1);
        assertEquals(Arrays.asList("F1", 2, 3, 10, null), drafts.last());
        vm.adjust(ScoreUi.Corner.BLUE, -1);
        assertEquals(Arrays.asList("F1", 2, 3, 10, 9), drafts.last());
    }

    @Test
    public void alRecrearLaPantallaVuelveElMismoPuntaje() {
        ScoreViewModel vm = vm(EstadoRound.ABIERTO);
        vm.onDraft("F1", 2, 3, 9, 10);
        assertEquals(Integer.valueOf(9), vm.ui().getValue().red);
        assertEquals(Integer.valueOf(10), vm.ui().getValue().blue);
    }

    @Test
    public void elBorradorPuedeLlegarAntesQueElRound() {
        ScoreViewModel vm = new ScoreViewModel(m -> { }, drafts);
        vm.setSession("F1", 2, "María López");
        vm.onDraft("F1", 2, 3, 9, 10);
        vm.onConnection(new ConnectionState(ConnectionState.Status.CONNECTED, 3, EstadoRound.ABIERTO, 0, null));
        assertEquals(Integer.valueOf(9), vm.ui().getValue().red);
    }

    @Test
    public void unaEsquinaSinTocarSigueVacia() {
        ScoreViewModel vm = vm(EstadoRound.ABIERTO);
        vm.onDraft("F1", 2, 3, 9, null);
        assertEquals(Integer.valueOf(9), vm.ui().getValue().red);
        assertNull("sin valor por defecto", vm.ui().getValue().blue);
    }

    @Test
    public void conElRoundCerradoNoSeRestaura() {
        ScoreViewModel vm = vm(EstadoRound.CERRADO);
        vm.onDraft("F1", 2, 3, 9, 10);
        assertNull(vm.ui().getValue().red);
    }

    @Test
    public void conLaTarjetaYaEnviadaNoSeRestaura() {
        ScoreViewModel vm = vm(EstadoRound.ABIERTO);
        vm.onOwnCards(Collections.singletonMap(3,
                new OwnCards.Row(3, 10, 9, ScoreUi.CardStatus.SENT, "m1")));
        vm.onDraft("F1", 2, 3, 9, 10);
        assertNull(vm.ui().getValue().red);
    }

    @Test
    public void unBorradorDeOtroRoundOPeleaNoSeRestauraYSeBorra() {
        ScoreViewModel vm = vm(EstadoRound.ABIERTO);
        vm.onDraft("F1", 2, 2, 9, 10);
        assertNull(vm.ui().getValue().red);
        vm.onDraft("OTRA", 2, 3, 9, 10);
        assertNull(vm.ui().getValue().red);
        assertTrue(drafts.cleared >= 2);
    }

    @Test
    public void enviarBorraElBorrador() {
        ScoreViewModel vm = vm(EstadoRound.ABIERTO);
        vm.adjust(ScoreUi.Corner.RED, 1);
        vm.adjust(ScoreUi.Corner.BLUE, -1);
        int before = drafts.cleared;
        vm.submit();
        assertEquals(before + 1, drafts.cleared);
    }
}
