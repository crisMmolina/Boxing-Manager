package com.sigebox.juez.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sigebox.juez.R;

/** Hoja con la tarjeta en grande y «Mantén para enviar» (DESIGN.md v2 §5.6, spec 009). */
public final class ConfirmSheet {

    private ConfirmSheet() {
    }

    /** Muestra la hoja; {@code onSend} corre solo si el juez mantiene el botón o usa la acción «Enviar round». */
    public static BottomSheetDialog show(Context context, int round, int red, int blue, Runnable onSend) {
        var dialog = new BottomSheetDialog(context);
        var view = LayoutInflater.from(context).inflate(R.layout.view_confirm_sheet, null, false);
        ((TextView) view.findViewById(R.id.confirm_red)).setText(String.valueOf(red));
        ((TextView) view.findViewById(R.id.confirm_blue)).setText(String.valueOf(blue));
        ((TextView) view.findViewById(R.id.confirm_round)).setText(context.getString(R.string.score_round, round));
        view.setContentDescription(context.getString(R.string.confirm_a11y, round, red, blue));
        view.findViewById(R.id.confirm_send).setOnClickListener(v -> {
            dialog.dismiss();
            onSend.run();
        });
        view.findViewById(R.id.confirm_review).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
        return dialog;
    }
}
