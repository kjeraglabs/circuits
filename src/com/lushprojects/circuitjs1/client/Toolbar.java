/*
    Copyright (C) Paul Falstad and Iain Sharp

    This file is part of CircuitJS1.

    CircuitJS1 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    CircuitJS1 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CircuitJS1.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

// Quick-select toolbar under the menu (Kjerag Labs addition).
// Part icons come from Paul Falstad's toolbar in pfalstad/circuitjs1 (GPL).
// Click a part to start placing it; click it again to go back to select mode.
// Buttons with several variants show them in a palette on hover or tap.
class Toolbar extends FlowPanel {
    static final int HEIGHT = 36;
    static final String INK = "#3b352d";
    static final String HOVER = "#b04a3e";
    static final String ACTIVE_BG = "#f3d9cf";

    // one button; for variant buttons, classes/icons hold every variant
    class Button {
	Label label;
	String classes[], icons[];
	int current;
	FlowPanel palette;
	boolean active;
	String currentClass() { return classes[current]; }
    }

    CirSim sim;
    Label toleranceButton, breakingButton;
    ArrayList<Button> buttons = new ArrayList<Button>();
    String lastActive = "";
    Button openPalette;
    Timer hideTimer;

    Toolbar(CirSim s) {
	sim = s;
	Style st = getElement().getStyle();
	st.setProperty("display", "flex");
	st.setProperty("alignItems", "center");
	st.setProperty("flexWrap", "nowrap");
	st.setProperty("overflowX", "auto");
	st.setProperty("overflowY", "hidden");
	st.setProperty("background", "#efe7d6");
	st.setProperty("borderBottom", "1px solid #d6cbb4");
	st.setProperty("padding", "0 4px");
	st.setProperty("boxSizing", "border-box");
	st.setProperty("height", HEIGHT + "px");

	addCommand("ccw", "Undo (Ctrl-Z)", "edit", "undo");
	addCommand("cw", "Redo (Ctrl-Y)", "edit", "redo");
	addCommand("scissors", "Cut (Ctrl-X)", "edit", "cut");
	addCommand("copy", "Copy (Ctrl-C)", "edit", "copy");
	addCommand("paste", "Paste (Ctrl-V)", "edit", "paste");
	addCommand("clone", "Duplicate (Ctrl-D)", "edit", "duplicate");
	addSeparator();
	addCommand("zoom-11", "Zoom 100%", "key", "zoom100");
	addCommand("zoom-in", "Zoom in (+)", "key", "zoomin");
	addCommand("zoom-out", "Zoom out (-)", "key", "zoomout");
	addSeparator();
	toleranceButton = makeButtonLabel("Tolerance testing: try worst-case values of parts with a tolerance");
	toleranceButton.getElement().setInnerHTML(svg(toleranceIcon, 24));
	addHover(toleranceButton, null);
	toleranceButton.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent e) {
		sim.menuPerformed("edit", "tolerance");
	    }
	});
	breakingButton = makeButtonLabel("");
	breakingButton.getElement().setInnerHTML(svg(breakingIcon, 24));
	addHover(breakingButton, null);
	breakingButton.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent e) {
		sim.menuPerformed("edit", "breaking");
	    }
	});
	setBreakingActive(sim.breakingEnabled);
	addSeparator();

	addPart("Wire", new String[] { "WireElm" }, new String[] { wireIcon });
	addPart("Resistor", new String[] { "ResistorElm" }, new String[] { resistorIcon });
	addPart("Ground", new String[] { "GroundElm" }, new String[] { groundIcon });
	addPart("Capacitor", new String[] { "CapacitorElm" }, new String[] { capacitorIcon });
	addPart("Inductor", new String[] { "InductorElm" }, new String[] { inductIcon });
	addPart("Diode", new String[] { "DiodeElm" }, new String[] { diodeIcon });
	addPart("LED", new String[] { "LEDElm" }, new String[] { ledIcon });
	addPart("Battery / AC source", new String[] { "DCVoltageElm", "ACVoltageElm" },
		new String[] { voltage2Icon, acSrcIcon });
	addPart("Voltage source (1-terminal)", new String[] { "RailElm" }, new String[] { railIcon });
	addPart("Switches", new String[] { "SwitchElm", "Switch2Elm", "AnalogSwitchElm", "AnalogSwitch2Elm" },
		new String[] { switchIcon, spdtIcon, aswitch1Icon, aswitch2Icon });
	addPart("Op amps", new String[] { "OpAmpElm", "OpAmpSwapElm", "OpAmpSupplyElm" },
		new String[] { opAmpBotIcon, opAmpTopIcon, opAmpSupplyIcon });
	addPart("Transistors", new String[] { "NTransistorElm", "PTransistorElm" },
		new String[] { transistorIcon, pnpTransistorIcon });
	addPart("MOSFETs", new String[] { "NMosfetElm", "PMosfetElm" }, new String[] { fetIcon, fetIcon2 });
	addPart("Inverter", new String[] { "InverterElm" }, new String[] { inverterIcon });
	addPart("Logic gates", new String[] { "AndGateElm", "NandGateElm", "OrGateElm", "NorGateElm", "XorGateElm" },
		new String[] { andIcon, nandIcon, orIcon, norIcon, xorIcon });
	addSeparator();
	addPart("Generic load", new String[] { "GenericLoadElm" }, new String[] { loadIcon });
	addPart("I/O pins", new String[] { "DigitalInputElm", "AnalogInputElm", "DigitalOutputElm", "AnalogOutputElm" },
		new String[] { dinIcon, ainIcon, doutIcon, aoutIcon });
	addPart("Meters", new String[] { "ProbeElm", "AmmeterElm" }, new String[] { voltmeterIcon, ammeterIcon });
	addPart("Net label", new String[] { "LabeledNodeElm" }, new String[] { labelIcon });
    }

    static String svg(String icon, int size) {
	String inner = icon.replaceFirst("^\\s*<svg[^>]*>", "").replaceFirst("</svg>\\s*$", "");
	return "<svg xmlns='http://www.w3.org/2000/svg' width='" + size + "' height='" + size +
	    "' viewBox='0 0 24 24' style='display:block'>" + inner + "</svg>";
    }

    Label makeButtonLabel(String title) {
	final Label l = new Label();
	l.setTitle(title);
	Style st = l.getElement().getStyle();
	st.setProperty("color", INK);
	st.setProperty("cursor", "pointer");
	st.setProperty("padding", "3px");
	st.setProperty("margin", "0 1px");
	st.setProperty("borderRadius", "4px");
	st.setProperty("flex", "0 0 auto");
	st.setProperty("lineHeight", "24px");
	st.setProperty("fontSize", "20px");
	st.setProperty("userSelect", "none");
	add(l);
	return l;
    }

    void addSeparator() {
	Label sep = new Label();
	Style st = sep.getElement().getStyle();
	st.setProperty("width", "1px");
	st.setProperty("height", "22px");
	st.setProperty("background", "#d6cbb4");
	st.setProperty("margin", "0 6px");
	st.setProperty("flex", "0 0 auto");
	add(sep);
    }

    void addCommand(String fontIcon, String title, final String menu, final String item) {
	final Label l = makeButtonLabel(title);
	l.getElement().addClassName("cirjsicon-" + fontIcon);
	addHover(l, null);
	l.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent e) {
		sim.menuPerformed(menu, item);
	    }
	});
    }

    void addHover(final Label l, final Button b) {
	l.addMouseOverHandler(new MouseOverHandler() {
	    public void onMouseOver(MouseOverEvent e) {
		l.getElement().getStyle().setProperty("color", HOVER);
	    }
	});
	l.addMouseOutHandler(new MouseOutHandler() {
	    public void onMouseOut(MouseOutEvent e) {
		l.getElement().getStyle().setProperty("color", (b != null && b.active) ? HOVER : INK);
	    }
	});
    }

    String shortcutFor(String cls) {
	int i;
	for (i = 33; i < 127; i++)
	    if (cls.equals(sim.shortcuts[i]))
		return " (" + (char) i + ")";
	return "";
    }

    void addPart(final String title, String classes[], String icons[]) {
	final Button b = new Button();
	b.classes = classes;
	b.icons = icons;
	b.label = makeButtonLabel(partTitle(title, classes[0], classes.length));
	b.label.getElement().setInnerHTML(svg(icons[0], 24));
	addHover(b.label, b);
	buttons.add(b);

	b.label.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent e) {
		if (b.active)
		    sim.menuPerformed("main", "Select");
		else
		    sim.menuPerformed("main", b.currentClass());
		if (b.palette != null)
		    showPalette(b);
	    }
	});
	if (classes.length == 1)
	    return;

	// variant palette
	b.palette = new FlowPanel();
	b.palette.setVisible(false);
	Style ps = b.palette.getElement().getStyle();
	ps.setProperty("position", "absolute");
	ps.setProperty("zIndex", "1000");
	ps.setProperty("background", "#fbf7ee");
	ps.setProperty("border", "1px solid #d6cbb4");
	ps.setProperty("borderRadius", "6px");
	ps.setProperty("boxShadow", "0 4px 12px rgba(0,0,0,.15)");
	ps.setProperty("padding", "4px");
	ps.setProperty("whiteSpace", "nowrap");
	int i;
	for (i = 0; i != classes.length; i++) {
	    final int n = i;
	    final Label v = new Label();
	    v.setTitle(sim.getLabelTextForClass(classes[i]) + shortcutFor(classes[i]));
	    v.getElement().setInnerHTML(svg(icons[i], 36));
	    Style vs = v.getElement().getStyle();
	    vs.setProperty("color", INK);
	    vs.setProperty("cursor", "pointer");
	    vs.setProperty("padding", "2px");
	    vs.setProperty("borderRadius", "4px");
	    vs.setProperty("display", "inline-block");
	    vs.setProperty("verticalAlign", "top");
	    addHover(v, null);
	    v.addClickHandler(new ClickHandler() {
		public void onClick(ClickEvent e) {
		    b.current = n;
		    b.label.getElement().setInnerHTML(svg(b.icons[n], 24));
		    b.label.setTitle(partTitle(title, b.classes[n], b.classes.length));
		    b.palette.setVisible(false);
		    sim.menuPerformed("main", b.classes[n]);
		}
	    });
	    b.palette.add(v);
	}
	RootPanel.get().add(b.palette);

	b.label.addMouseOverHandler(new MouseOverHandler() {
	    public void onMouseOver(MouseOverEvent e) { showPalette(b); }
	});
	b.label.addMouseOutHandler(new MouseOutHandler() {
	    public void onMouseOut(MouseOutEvent e) { hidePaletteSoon(); }
	});
	b.palette.addDomHandler(new MouseOverHandler() {
	    public void onMouseOver(MouseOverEvent e) { cancelHide(); }
	}, MouseOverEvent.getType());
	b.palette.addDomHandler(new MouseOutHandler() {
	    public void onMouseOut(MouseOutEvent e) { hidePaletteSoon(); }
	}, MouseOutEvent.getType());
    }

    String partTitle(String group, String cls, int variants) {
	String t = sim.getLabelTextForClass(cls) + shortcutFor(cls);
	return variants > 1 ? t + " – hover for more " + group.toLowerCase() : t;
    }

    void showPalette(Button b) {
	cancelHide();
	if (openPalette != null && openPalette != b)
	    openPalette.palette.setVisible(false);
	openPalette = b;
	b.palette.setVisible(true);
	b.palette.getElement().getStyle().setProperty("left", (b.label.getAbsoluteLeft() - 8) + "px");
	b.palette.getElement().getStyle().setProperty("top", (b.label.getAbsoluteTop() + b.label.getOffsetHeight() + 2) + "px");
    }

    void hidePaletteSoon() {
	cancelHide();
	hideTimer = new Timer() {
	    public void run() {
		if (openPalette != null)
		    openPalette.palette.setVisible(false);
		openPalette = null;
	    }
	};
	hideTimer.schedule(250);
    }

    void cancelHide() {
	if (hideTimer != null)
	    hideTimer.cancel();
	hideTimer = null;
    }

    void setBreakingActive(boolean on) {
	breakingButton.setTitle(on ? "Component ratings: ON, parts warn and burn out (click to turn off)"
				: "Component ratings: OFF (click to turn on warnings and burn-out)");
	Style st = breakingButton.getElement().getStyle();
	st.setProperty("background", on ? ACTIVE_BG : "transparent");
	st.setProperty("color", on ? HOVER : INK);
	st.setProperty("opacity", on ? "1" : ".55");
    }

    void setToleranceActive(boolean on) {
	Style st = toleranceButton.getElement().getStyle();
	st.setProperty("background", on ? ACTIVE_BG : "transparent");
	st.setProperty("color", on ? HOVER : INK);
    }

    // highlight the button for the part being placed; called every frame, cheap when nothing changed
    void update(String activeClass) {
	if (activeClass == null)
	    activeClass = "";
	if (activeClass.equals(lastActive))
	    return;
	lastActive = activeClass;
	for (Button b : buttons) {
	    int i, found = -1;
	    for (i = 0; i != b.classes.length; i++)
		if (b.classes[i].equals(activeClass))
		    found = i;
	    // placing a variant chosen elsewhere (menu or key) shows that variant on the button
	    if (found >= 0 && found != b.current) {
		b.current = found;
		b.label.getElement().setInnerHTML(svg(b.icons[found], 24));
	    }
	    b.active = found >= 0;
	    Style st = b.label.getElement().getStyle();
	    st.setProperty("background", b.active ? ACTIVE_BG : "transparent");
	    st.setProperty("color", b.active ? HOVER : INK);
	}
    }

    // ---- icons added for Kjerag Labs parts (24x24) ----
    static final String ledIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linecap='round' stroke-linejoin='round'>" +
	"<path d='M1 15h7M15 15h8'/><path d='M8 10.5v9l7-4.5z' fill='currentColor'/><path d='M15 10.5v9'/>" +
	"<path d='M12 8l3.5-3.5M15.5 10l3.5-3.5'/><path d='M13.5 4.5h2v2M17 6h2v2'/></g></svg>";
    static final String opAmpSupplyIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'>" +
	"<path d='M5 5v14l13-7z'/><path d='M1 9h4M1 15h4M18 12h5'/><path d='M10 1v6.2M10 16.8V23'/>" +
	"<path d='M6.6 9h2M6.6 15h2M7.6 14v2' stroke-width='.9'/></g></svg>";
    static final String voltmeterIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linecap='round'>" +
	"<path d='M12 1v4M12 19v4'/><circle cx='12' cy='12' r='7'/></g>" +
	"<text x='12' y='15.6' font-size='10' font-family='sans-serif' font-weight='bold' text-anchor='middle' fill='currentColor'>V</text></svg>";
    static final String loadIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linejoin='round'>" +
	"<path d='M1 12h3M20 12h3'/><rect x='4' y='7' width='16' height='10' rx='1.5'/></g>" +
	"<text x='12' y='14.3' font-size='6' font-family='sans-serif' font-weight='bold' text-anchor='middle' fill='currentColor'>LOAD</text></svg>";
    static final String dinIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linejoin='round'>" +
	"<path d='M1 12h6'/><path d='M7 6.5h9l5 5.5-5 5.5H7z'/></g>" +
	"<text x='13' y='15' font-size='8' font-family='sans-serif' font-weight='bold' text-anchor='middle' fill='currentColor'>I</text></svg>";
    static final String doutIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linejoin='round'>" +
	"<path d='M1 12h5'/><path d='M6 12l5-5.5h11v11H11z'/></g>" +
	"<text x='15.5' y='15' font-size='8' font-family='sans-serif' font-weight='bold' text-anchor='middle' fill='currentColor'>O</text></svg>";
    static final String ainIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linejoin='round'>" +
	"<path d='M1 12h6'/><path d='M7 6.5h9l5 5.5-5 5.5H7z'/><path d='M9 12.5c1-2.5 2-2.5 3 0s2 2.5 3 0'/></g></svg>";
    static final String aoutIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linejoin='round'>" +
	"<path d='M1 12h5'/><path d='M6 12l5-5.5h11v11H11z'/><path d='M12.5 12.5c1-2.5 2-2.5 3 0s2 2.5 3 0'/></g></svg>";
    // burned X with a small warning triangle
    static final String breakingIcon = "<svg><g fill='none' stroke='currentColor' stroke-linecap='round' stroke-linejoin='round'>" +
	"<path d='M3 5l9 9M12 5l-9 9' stroke-width='2.4'/>" +
	"<path d='M17.5 11.5l5 9h-10z' stroke-width='1.3'/><path d='M17.5 14.6v2.6' stroke-width='1.4'/></g>" +
	"<circle cx='17.5' cy='18.8' r='.8' fill='currentColor'/></svg>";
    static final String toleranceIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linecap='round' stroke-linejoin='round'>" +
	"<path d='M1 15h3l1-2.5 2 5 2-5 2 5 2-5 1 2.5h3'/><path d='M20 3v6M17 6h6M17 12h6'/></g></svg>";
    static final String ammeterIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linecap='round'>" +
	"<path d='M12 1v4M12 19v4'/><circle cx='12' cy='12' r='7'/></g>" +
	"<text x='12' y='15.6' font-size='10' font-family='sans-serif' font-weight='bold' text-anchor='middle' fill='currentColor'>A</text></svg>";
    static final String labelIcon = "<svg><g fill='none' stroke='currentColor' stroke-width='1.3' stroke-linecap='round' stroke-linejoin='round'>" +
	"<path d='M1 12h4'/><path d='M5 8h12l5 4-5 4H5z'/></g></svg>";

    // ---- icons from pfalstad/circuitjs1 Toolbar.java ----
    static final String wireIcon = "<svg><g transform='scale(0.208) translate(7.5, 32)'>" +
           "<line x1='5' y1='45' x2='95' y2='5' stroke='currentColor' stroke-width='8' /> " +
           "<circle cx='5' cy='45' r='10' fill='currentColor' /><circle cx='95' cy='5' r='10' fill='currentColor' /> " +
           "</g></svg>";
    static final String resistorIcon = "<svg> <g transform='scale(.5,.5) translate(-544,-297)'>" +
      "<path stroke='#000000' d=' M 544 320 L 552 320' stroke-width='3'/>" +
      "<path stroke='#000000' d=' M 584 320 L 592 320' stroke-width='3'/>" +
      "<g transform='matrix(1,0,0,1,552,320)'><path fill='none' stroke='currentColor' " +
      "d=' M 0 0 L 2 6 L 6 -6 L 10 6 L 14 -6 L 18 6 L 22 -6 L 26 6 L 30 -6 L 32 0' stroke-width='2'/> </g> </g> </svg>";
    static final String euroResistorIcon = "<svg><g transform='translate(97.71,-28.71) scale(0.428571)'><path fill='none' stroke='currentColor' d=' M -224 96 L -216 96' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M -184 96 L -176 96' stroke-linecap='round' stroke-width='3' /><g transform='matrix(1,0,0,1,-216,96)'><rect fill='none' stroke='currentColor' x='0' y='-6' width='32' height='12' stroke-linecap='round' stroke-width='3' /></g><path fill='currentColor' stroke='currentColor' d=' M -221 96 A 3 3 0 1 1 -221.00026077471009 95.96044522459944 Z' /><path fill='currentColor' stroke='currentColor' d=' M -173 96 A 3 3 0 1 1 -173.00026077471009 95.96044522459944 Z' /></g></svg>";
    static final String groundIcon = "<svg><defs /><g transform='scale(.6) translate(-826.46,-231.31) scale(1.230769)'><path fill='none' stroke='currentColor' d=' M 688 192 L 688 208' stroke-linecap='round' stroke-width='3' /> <path fill='none' stroke='currentColor' d=' M 698 208 L 678 208' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 694 213 L 682 213' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 690 218 L 686 218' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 691 192 A 3 3 0 1 1 690.9997392252899 191.96044522459943 Z' /> </g></svg>";
    static final String capacitorIcon = "<svg><defs /><g transform='translate(-323.76,-71.18) scale(0.470588)'><path fill='none' stroke='currentColor' d=' M 688 176 L 708 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 708 164 L 708 188' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 736 176 L 716 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 716 164 L 716 188' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 691 176 A 3 3 0 1 1 690.9997392252899 175.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 739 176 A 3 3 0 1 1 738.9997392252899 175.96044522459943 Z' /></g></svg>";
    static final String diodeIcon = "<svg><defs /><g transform='translate(-323.76,-72.06) scale(0.470588)'><path fill='none' stroke='currentColor' d=' M 688 176 L 704 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 720 176 L 736 176' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 704 168 L 704 184 L 720 176 Z' /><path fill='none' stroke='currentColor' d=' M 720 168 L 720 184' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 691 176 A 3 3 0 1 1 690.9997392252899 175.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 739 176 A 3 3 0 1 1 738.9997392252899 175.96044522459943 Z' /></g></svg>";
    static final String switchIcon = "<svg><defs /><g transform='translate(55.79,-100.42) scale(0.421053)'><path fill='none' stroke='currentColor' d=' M -128 272 L -120 272' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M -88 272 L -80 272' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M -120 272 L -88 256' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String voltage2Icon = "<svg><defs /><g transform='scale(.8) translate(-122.00,-53.00) scale(0.500000)'><path fill='none' stroke='currentColor' d=' M 272 160 L 272 140' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 132 L 272 112' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 262 140 L 282 140' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 256 132 L 288 132' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 275 160 A 3 3 0 1 1 274.99973922528994 159.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 275 112 A 3 3 0 1 1 274.99973922528994 111.96044522459944 Z' /></g></svg>";
    static final String transistorIcon = "<svg><defs /><g transform='translate(-107.73,-90.40) scale(0.533333)'><path fill='none' stroke='currentColor' d=' M 240 176 L 227 186' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 240 208 L 227 198' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 240 208 L 236 200 L 231 206 Z' /><path fill='none' stroke='currentColor' d=' M 208 192 L 224 192' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 224 176 L 227 176 L 227 208 L 224 208 Z' /><path fill='currentColor' stroke='currentColor' d=' M 211 192 A 3 3 0 1 1 210.99973922528991 191.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 243 176 A 3 3 0 1 1 242.99973922528991 175.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 243 208 A 3 3 0 1 1 242.99973922528991 207.96044522459943 Z' /></g></svg>";
    static final String pnpTransistorIcon = "<svg><defs /><g transform='translate(-116.27,-90.40) scale(0.533333)'><path fill='none' stroke='currentColor' d=' M 256 208 L 243 198' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 256 176 L 243 186' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 245 187 L 253 184 L 248 178 Z' /><path fill='none' stroke='currentColor' d=' M 224 192 L 240 192' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 240 176 L 243 176 L 243 208 L 240 208 Z' /><path fill='currentColor' stroke='currentColor' d=' M 227 192 A 3 3 0 1 1 226.99973922528991 191.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 259 208 A 3 3 0 1 1 258.99973922528994 207.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 259 176 A 3 3 0 1 1 258.99973922528994 175.96044522459943 Z' /></g></svg>";
    static final String fetIcon = "<svg><defs /><g transform='translate(-68.92,-50.27) scale(0.324324)'><path fill='none' stroke='currentColor' d=' M 272 208 L 250 208' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 176 L 250 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 208 L 250 203' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 197 L 250 192' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 192 L 250 187' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 181 L 250 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 208 L 250 213' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 176 L 250 171' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 208 L 272 192' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 192 L 250 192' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 250 192 L 262 197 L 262 187 Z' /><path fill='none' stroke='currentColor' d=' M 224 192 L 244 192' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 244 184 L 244 200' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 176 L 272 160' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 208 L 272 224' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 227 192 A 3 3 0 1 1 226.99973922528991 191.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 275 160 A 3 3 0 1 1 274.99973922528994 159.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 275 224 A 3 3 0 1 1 274.99973922528994 223.96044522459943 Z' /></g></svg>";
    static final String fetIcon2 = "<svg><defs /><g transform='translate(-68.92,-50.27) scale(0.324324)'><path fill='none' stroke='currentColor' d=' M 272 176 L 272 160' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 208 L 272 224' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 208 L 250 208' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 176 L 250 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 208 L 250 203' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 197 L 250 192' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 192 L 250 187' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 181 L 250 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 208 L 250 213' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 250 176 L 250 171' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 176 L 272 192' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 272 192 L 250 192' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 272 192 L 260 187 L 260 197 Z' /><path fill='none' stroke='currentColor' d=' M 224 192 L 244 192' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 244 184 L 244 200' stroke-linecap='round' stroke-width='3' /><path fill='currentColor' stroke='currentColor' d=' M 275 160 A 3 3 0 1 1 274.99973922528994 159.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 275 224 A 3 3 0 1 1 274.99973922528994 223.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 227 192 A 3 3 0 1 1 226.99973922528991 191.96044522459943 Z' /></g></svg>";
    static final String inductIcon = "<svg><g transform='translate(-101.59,-58.18) scale(0.405680)'><path fill='none' stroke='currentColor' d=' M 256 176 L 264 176' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 296 176 L 304 176' stroke-linecap='round' stroke-width='3' /><g transform='matrix(1,0,0,1,264,176) scale(1,1)'><path fill='none' stroke='currentColor' d=' M 0 0 L 0 6.53144959545255e-16 A 5.333333333333333 5.333333333333333 0 0 1 10.666666666666666 0 L 10.666666666666666 0' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 10.666666666666666 0 L 10.666666666666668 6.53144959545255e-16 A 5.333333333333333 5.333333333333333 0 0 1 21.333333333333332 0 L 21.333333333333332 0' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 21.333333333333332 0 L 21.333333333333336 6.53144959545255e-16 A 5.333333333333333 5.333333333333333 0 0 1 32 0 L 32 0' stroke-linecap='round' stroke-width='3' /></g><path fill='currentColor' stroke='currentColor' d=' M 259 176 A 3 3 0 1 1 258.99973922528994 175.96044522459943 Z' /><path fill='currentColor' stroke='currentColor' d=' M 307 176 A 3 3 0 1 1 306.99973922528994 175.96044522459943 Z' /></g></svg>";
    static final String railIcon = "<svg><g><text style='user-select: none;' fill='currentColor' stroke='none' font-family='sans-serif' font-size='10px' font-style='normal' font-weight='normal' text-decoration='normal' x='2' y='11' text-anchor='start' dominant-baseline='central'>+5V</text></g></svg>";
    static final String andIcon = "<svg><g transform='translate(-143.64,-130.55) scale(0.363636)'><path fill='none' stroke='currentColor' d=' M 400 400 L 414 400' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 400 384 L 414 384' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 442 392 L 456 392' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 414 378 L 428 378 A 14 14 0 0 1 428 406 L 414 406 Z' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String orIcon = "<svg><g transform='translate(-143.64,-153.82) scale(0.363636)'><path fill='none' stroke='currentColor' d=' M 400 464 L 414 464' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 400 448 L 414 448' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 442 456 L 456 456' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 413 442 L 422 442 C 434 445 434 445 442 456 C 434 467 434 467 422 470 L 413 470 C 416 456 416 456 413 442 Z' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String xorIcon = "<svg><g transform='translate(-143.64,-180.00) scale(0.363636)'><path fill='none' stroke='currentColor' d=' M 400 536 L 414 536' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 400 520 L 414 520' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 442 528 L 456 528' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 413 514 L 422 514 C 434 517 434 517 442 528 C 434 539 434 539 422 542 L 413 542 C 416 528 416 528 413 514 Z M 408 514 C 411 528 411 528 408 542' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String nandIcon = "<svg><g transform='translate(-143.64,-142.18) scale(0.363636)'><path fill='none' stroke='currentColor' d=' M 400 432 L 414 432' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 400 416 L 414 416' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 450 424 L 456 424' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 414 410 L 428 410 A 14 14 0 0 1 428 438 L 414 438 Z' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 448.94 424 A 2.94 2.94 0 1 1 448.93999853000014 423.99706000049' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String norIcon = "<svg><g transform='translate(-143.64,-165.45) scale(0.363636)'><path fill='none' stroke='currentColor' d=' M 400 496 L 414 496' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 400 480 L 414 480' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 450 488 L 456 488' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 413 474 L 422 474 C 434 477 434 477 442 488 C 434 499 434 499 422 502 L 413 502 C 416 488 416 488 413 474 Z' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 448.94 488 A 2.94 2.94 0 1 1 448.93999853000014 487.99706000049' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String inverterIcon = "<svg><g transform='translate(-288.41,-166.76) scale(0.413793)'><path fill='none' stroke='currentColor' d=' M 704 432 L 712 432' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 746 432 L 752 432' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 712 416 L 712 448 L 739 432 Z' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 744.94 432 A 2.94 2.94 0 1 1 744.9399985300001 431.99706000049' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String aswitch1Icon = "<svg><g transform='translate(-242.27,-122.92) scale(0.324324)'><path fill='none' stroke='currentColor' d=' M 752 416 L 768 416' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 800 416 L 816 416' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 768 416 L 800 400' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 784 432 L 784 424' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String aswitch2Icon = "<svg><g transform='translate(-237.08,-146.27) scale(0.324324)'><path fill='none' stroke='currentColor' d=' M 736 480 L 752 480' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 784 496 L 800 496' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 784 464 L 800 464' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 752 480 L 784 464' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 768 496 L 768 512' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String spdtIcon = "<svg><g transform='translate(-242.27,-143.68) scale(0.324324)'><path fill='none' stroke='currentColor' d=' M 752 480 L 768 480' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 800 464 L 816 464' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 800 496 L 816 496' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 768 480 L 800 464' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String acSrcIcon = "<svg><g transform='translate(-104.09,-66.93) scale(0.266667)'><path fill='none' stroke='currentColor' d=' M 432 336 L 432 313' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 432 279 L 432 256' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 448.66 296 A 16.66 16.66 0 1 1 448.6599916700007 295.98334000277663' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 422 296 L 423 294 L 424 292 L 425 290 L 426 289 L 427 289 L 428 289 L 429 290 L 430 292 L 431 294 L 432 296 L 433 298 L 434 300 L 435 302 L 436 303 L 437 303 L 438 303 L 439 302 L 440 300 L 441 298 L 442 296' stroke-linecap='round' stroke-width='3' /></g></svg>";
    static final String opAmpTopIcon = "<svg><g transform='translate(-169.33,-86.13) scale(0.266667)'><path fill='none' stroke='currentColor' d=' M 640 384 L 654 384' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 640 352 L 654 352' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 706 368 L 720 368' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 654 400 L 654 336 L 706 368 Z' stroke-linecap='round' stroke-width='3' /><g><text fill='currentColor' stroke='currentColor' font-family='sans-serif' font-size='14px' font-style='normal' font-weight='normal' text-decoration='normal' x='664' y='382' text-anchor='middle' dominant-baseline='central'>-</text></g><g><text fill='currentColor' stroke='currentColor' font-family='sans-serif' font-size='14px' font-style='normal' font-weight='normal' text-decoration='normal' x='664' y='352' text-anchor='middle' dominant-baseline='central'>+</text></g></g></svg>";
    static final String opAmpBotIcon = "<svg><g transform='translate(-169.33,-86.13) scale(0.266667)'><path fill='none' stroke='currentColor' d=' M 640 352 L 654 352' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 640 384 L 654 384' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 706 368 L 720 368' stroke-linecap='round' stroke-width='3' /><path fill='none' stroke='currentColor' d=' M 654 336 L 654 400 L 706 368 Z' stroke-linecap='round' stroke-width='3' /><g><text fill='currentColor' stroke='currentColor' font-family='sans-serif' font-size='14px' font-style='normal' font-weight='normal' text-decoration='normal' x='664' y='350' text-anchor='middle' dominant-baseline='central'>-</text></g><g><text fill='currentColor' stroke='currentColor' font-family='sans-serif' font-size='14px' font-style='normal' font-weight='normal' text-decoration='normal' x='664' y='384' text-anchor='middle' dominant-baseline='central'>+</text></g></g></svg>";
}
