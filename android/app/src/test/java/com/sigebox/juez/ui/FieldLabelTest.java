package com.sigebox.juez.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.R.attr;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.sigebox.juez.R;
import com.sigebox.juez.terms.TermsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** DESIGN.md v4 §5.4: todo campo lleva etiqueta arriba siempre visible y un ejemplo adentro. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class FieldLabelTest {

    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String APP = "http://schemas.android.com/apk/res-auto";
    private static final int[] FIELDS = {R.id.join_name, R.id.join_code, R.id.join_host};

    private static Document parse(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file);
    }

    private static List<Element> elements(Document document) {
        List<Element> all = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            all.add((Element) nodes.item(i));
        }
        return all;
    }

    private static boolean isEdit(Element element) {
        return element.getTagName().endsWith("EditText") || element.getTagName().endsWith("AutoCompleteTextView");
    }

    private static Element previousElement(Node node) {
        Node previous = node.getPreviousSibling();
        while (previous != null && !(previous instanceof Element)) {
            previous = previous.getPreviousSibling();
        }
        return (Element) previous;
    }

    private static String idName(String reference) {
        return reference.substring(reference.indexOf('/') + 1);
    }

    private static void checkField(String layout, Element edit, List<String> failures) {
        String where = layout + " " + edit.getAttributeNS(ANDROID, "id");
        Node box = edit.getParentNode();
        if (!((Element) box).getTagName().endsWith("TextInputLayout")) {
            failures.add(where + ": sin TextInputLayout");
            return;
        }
        Element til = (Element) box;
        if (!"false".equals(til.getAttributeNS(APP, "hintEnabled"))) {
            failures.add(where + ": etiqueta flotante");
        }
        if (til.hasAttributeNS(ANDROID, "hint") || edit.hasAttributeNS(ANDROID, "hint")) {
            failures.add(where + ": android:hint");
        }
        // AND-025: el Nombre y el Código van sin ejemplo; la IP lo conserva.
        if (til.getAttributeNS(APP, "placeholderText").isEmpty()
                && !where.endsWith("/join_name") && !where.endsWith("/join_code")) {
            failures.add(where + ": sin ejemplo");
        }
        Element label = previousElement(til.getParentNode());
        if (label == null || !label.getTagName().equals("TextView")
                || !idName(label.getAttributeNS(ANDROID, "labelFor")).equals(idName(edit.getAttributeNS(ANDROID, "id")))
                || label.getAttributeNS(ANDROID, "text").isEmpty()) {
            failures.add(where + ": sin etiqueta arriba con labelFor");
        }
    }

    @Test
    public void todosLosCamposDeLosLayoutsLlevanEtiquetaYEjemplo() throws Exception {
        File[] layouts = new File("src/main/res/layout").listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull(layouts);
        Arrays.sort(layouts);
        int fields = 0;
        List<String> failures = new ArrayList<>();
        for (File layout : layouts) {
            for (Element element : elements(parse(layout))) {
                if (isEdit(element)) {
                    fields++;
                    checkField(layout.getName(), element, failures);
                }
            }
        }
        assertTrue("el recorrido no encontró los campos: " + fields, fields >= FIELDS.length);
        assertTrue(failures.toString(), failures.isEmpty());
    }

    private static View join(TermsActivity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.activity_join, null);
        activity.setContentView(view);
        return view;
    }

    private static TextInputLayout box(View field) {
        View parent = (View) field.getParent();
        while (!(parent instanceof TextInputLayout)) {
            parent = (View) parent.getParent();
        }
        return (TextInputLayout) parent;
    }

    private static boolean shows(View view, CharSequence text) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (view instanceof TextView && !(view instanceof EditText)) {
            return text.equals(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (shows(group.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void elEjemploSeVeSinFocoYNoRepiteLaEtiqueta() {
        TermsActivity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        View root = join(activity);
        for (int id : FIELDS) {
            TextInputLayout box = box(root.findViewById(id));
            assertNull(box.getHint());
            if (id == R.id.join_name || id == R.id.join_code) {
                continue; // sin ejemplo
            }
            CharSequence example = box.getPlaceholderText();
            assertNotNull(example);
            assertTrue("ejemplo visible: " + example, shows(box, example.toString()));
            TextView label = labelOf(root, id);
            assertEquals(View.VISIBLE, label.getVisibility());
            assertFalse(example.toString().contains(label.getText()));
        }
    }

    private static TextView labelOf(View root, int fieldId) {
        View frame = (View) box(root.findViewById(fieldId)).getParent();
        ViewGroup parent = (ViewGroup) frame.getParent();
        return (TextView) parent.getChildAt(parent.indexOfChild(frame) - 1);
    }

    @Test
    public void elFocoEnciendeElAnilloYLaEtiquetaSigue() {
        TermsActivity activity = Robolectric.buildActivity(TermsActivity.class).setup().get();
        View root = join(activity);
        View field = root.findViewById(R.id.join_host);
        View frame = (View) box(field).getParent();
        assertFalse(contains(frame.getDrawableState(), attr.state_focused));
        field.requestFocus();
        assertTrue(contains(frame.getDrawableState(), attr.state_focused));
        assertEquals(View.VISIBLE, labelOf(root, R.id.join_host).getVisibility());
    }

    private static boolean contains(int[] states, int state) {
        for (int value : states) {
            if (value == state) {
                return true;
            }
        }
        return false;
    }
}
