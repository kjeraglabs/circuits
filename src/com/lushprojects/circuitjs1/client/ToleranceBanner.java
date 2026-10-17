/*
    Copyright (C) Kjerag Labs

    This file is part of CircuitJS1 (Kjerag Labs edition).

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

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;

// Bar across the top of the canvas while tolerance testing is on:
// says what to do, and has "All nominal" and "Exit" buttons.
class ToleranceBanner extends FlowPanel {
    CirSim sim;
    InlineLabel text;
    Button nominal, exit;

    ToleranceBanner(CirSim s) {
	sim = s;
	Style st = getElement().getStyle();
	st.setProperty("position", "absolute");
	st.setProperty("zIndex", "500");
	st.setProperty("transform", "translateX(-50%)");
	st.setProperty("background", "#fbf7ee");
	st.setProperty("color", "#3b352d");
	st.setProperty("border", "1px solid #d6cbb4");
	st.setProperty("borderRadius", "8px");
	st.setProperty("boxShadow", "0 3px 10px rgba(0,0,0,.15)");
	st.setProperty("padding", "6px 8px 6px 12px");
	st.setProperty("font", "13px sans-serif");
	st.setProperty("whiteSpace", "nowrap");
	st.setProperty("maxWidth", "92vw");
	st.setProperty("overflow", "hidden");
	st.setProperty("textOverflow", "ellipsis");

	text = new InlineLabel();
	text.getElement().getStyle().setProperty("marginRight", "10px");
	add(text);
	nominal = new Button("All nominal");
	nominal.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent e) {
		sim.resetToleranceSwitches();
		update();
	    }
	});
	add(nominal);
	exit = new Button("Exit tolerance testing");
	exit.getElement().getStyle().setProperty("marginLeft", "6px");
	exit.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent e) {
		sim.setToleranceMode(false);
	    }
	});
	add(exit);
	setVisible(false);
    }

    void update() {
	setVisible(sim.toleranceMode);
	if (!sim.toleranceMode)
	    return;
	int n = sim.countToleranceParts();
	if (n == 0)
	    text.setText("Tolerance testing: no parts have a tolerance yet. Set Tolerance (%) in a part's settings.");
	else
	    text.setText("Tolerance testing: click ▲ or ▼ next to a part to try its worst-case value.");
	nominal.setVisible(n > 0);
	position();
    }

    void position() {
	if (sim.cv == null)
	    return;
	// the canvas sits under the menu bar and (if shown) the toolbar, at the left edge
	int top = CirSim.MENUBARHEIGHT + (sim.isToolbarShown() ? Toolbar.HEIGHT : 0) + 10;
	int left = sim.cv.getCoordinateSpaceWidth()/2;
	getElement().getStyle().setProperty("top", top + "px");
	getElement().getStyle().setProperty("left", left + "px");
    }
}
