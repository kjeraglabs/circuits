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


import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;

// class EditDialog extends Dialog implements AdjustmentListener, ActionListener, ItemListener {
class SliderDialog extends DialogBox  {
	CircuitElm elm;
	// parts the dialog applies to: elm plus any other selected parts of the same kind
	java.util.Vector<CircuitElm> group;
	CirSim sim;
	Button applyButton, okButton, cancelButton;
	EditInfo einfos[];
	int einfocount;
	final int barmax = 1000;
	VerticalPanel vp;
	HorizontalPanel hp;
	NumberFormat noCommaFormat;

	SliderDialog(CircuitElm ce, java.util.Vector<CircuitElm> g, CirSim f) {
		super(); // Do we need this?
		setText(CirSim.LS("Add Sliders") + (g.size() > 1 ? " (" + g.size() + " parts)" : ""));
		sim = f;
		elm = ce;
		group = g;
		vp=new VerticalPanel();
		setWidget(vp);
		einfos = new EditInfo[10];
		noCommaFormat=NumberFormat.getFormat("####.##########");
		hp=new HorizontalPanel();
		hp.setWidth("100%");
		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		hp.setStyleName("topSpace");
		vp.add(hp);
		hp.add(applyButton = new Button(CirSim.LS("Apply")));
		applyButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				apply();
			}
		});
		hp.add(okButton = new Button(CirSim.LS("OK")));
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				apply();
				closeDialog();
			}
		});
		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		hp.add(cancelButton = new Button(CirSim.LS("Cancel")));
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				closeDialog();
			}
		});
		buildDialog();
		this.center();
	}
	
	void buildDialog() {
		int i;
		int idx;
		for (i = 0; ; i++) {
			einfos[i] = elm.getEditInfo(i);
			if (einfos[i] == null)
				break;
			EditInfo ei = einfos[i];
			if (!ei.canCreateAdjustable())
			    continue;
			Adjustable adj = findAdjustable(i);
			String name = CirSim.LS(ei.name);
			idx = vp.getWidgetIndex(hp);

			// remove HTML
			name = name.replaceAll("<[^>]*>", "");
			vp.insert(new Label(name), idx++);

			// one dropdown: no slider, a new slider, or share a slider another part of the same kind already has
			final java.util.Vector<Adjustable> masters = new java.util.Vector<Adjustable>();
			for (Adjustable a : sim.adjustables)
			    if (a.sharedSlider == null && a.editItem == i && a.elm.getClass() == elm.getClass() &&
				    a.elm != elm && !(group.contains(a.elm) && group.size() > 1))
				masters.add(a);
			final Choice c = new Choice();
			c.addItem(CirSim.LS("No slider"));
			c.addItem(CirSim.LS(adj != null && adj.sharedSlider == null ? "Own slider" : "New slider"));
			int sel = (adj == null) ? 0 : 1;
			int k;
			for (k = 0; k != masters.size(); k++) {
			    Adjustable m = masters.get(k);
			    c.addItem(CirSim.LS("Share slider") + " \"" + m.sliderText + "\"");
			    if (adj != null && adj.sharedSlider == m)
				sel = k + 2;
			}
			c.setSelectedIndex(sel);
			final int item = i;
			c.addChangeHandler(new ChangeHandler() {
			    public void onChange(ChangeEvent e) {
				int n = c.getSelectedIndex();
				choose(item, n < 2 ? null : masters.get(n-2), n);
			    }
			});
			vp.insert(c, idx++);

			if (adj == null || adj.sharedSlider != null) {
			    // nothing more to set
			} else {
			    vp.insert(new Label("Min Value"), idx++);
			    ei.minBox = new TextBox();
			    vp.insert(ei.minBox, idx++);
			    vp.insert(new Label("Max Value"), idx++);
			    ei.maxBox = new TextBox();
			    vp.insert(ei.maxBox, idx++);
			    vp.insert(new Label("Label"), idx++);
			    ei.labelBox = new TextBox();
			    ei.labelBox.setText(adj.sliderText);
			    vp.insert(ei.labelBox, idx++);
			    ei.minBox.setText(EditDialog.unitString(ei, adj.minValue));
			    ei.maxBox.setText(EditDialog.unitString(ei, adj.maxValue));
			}
			    
		}
		einfocount = i;
	}
	
	Adjustable findAdjustable(int item) {
	    return sim.findAdjustable(elm, item);
	}

	void apply() {
		int i;
		for (i = 0; i != einfocount; i++) {
		    Adjustable adj = findAdjustable(i);
		    if (adj == null || adj.sharedSlider != null)
			continue;
		    EditInfo ei = einfos[i];
		    if (ei.labelBox == null)
			continue;
//		    if (ei.labelBox == null)  // haven't created UI yet?
//			continue;
		    try {
			adj.sliderText = ei.labelBox.getText();
			adj.label.setText(adj.sliderText);
			double d = EditDialog.parseUnits(ei.minBox.getText());
			adj.minValue = d;
			d = EditDialog.parseUnits(ei.maxBox.getText());
			adj.maxValue = d;
			adj.setSliderValue(ei.value);
		    } catch (Exception e) { }
		}
	}

	// n: 0 = no slider, 1 = own slider, 2+ = share master
	void choose(int i, Adjustable master, int n) {
	    apply();
	    EditInfo ei = einfos[i];
	    if (n == 0) {
		for (CircuitElm g : group) {
		    Adjustable adj = sim.findAdjustable(g, i);
		    if (adj != null)
			sim.removeAdjustable(adj);
		}
	    } else if (n == 1) {
		Adjustable adj = findAdjustable(i);
		if (adj == null || adj.sharedSlider != null) {
		    if (adj != null)
			sim.removeAdjustable(adj);
		    adj = new Adjustable(elm, i);
		    adj.sliderText = ei.name.replaceAll("<[^>]*>", "").replaceAll(" \\(.*\\)$", "");
		    adj.createSlider(sim, ei.value);
		    sim.adjustables.add(adj);
		}
		// the other selected parts follow it
		for (CircuitElm g : group)
		    if (g != elm)
			addFollower(g, i, adj, ei.value);
	    } else {
		follow(i, master);
		return;
	    }
	    clearDialog();
	    buildDialog();
	}

	// make every part in the group follow an existing slider
	void follow(int item, Adjustable master) {
		double v = master.getSliderValue();
		for (CircuitElm g : group)
		    addFollower(g, item, master, v);
		sim.needAnalyze();
		clearDialog();
		buildDialog();
	}

	void addFollower(CircuitElm g, int item, Adjustable master, double value) {
		Adjustable old = sim.findAdjustable(g, item);
		if (old != null)
		    sim.removeAdjustable(old);
		sim.adjustables.add(new Adjustable(g, item, master));
		Adjustable.setValue(g, item, value);
		sim.needAnalyze();
	}

	public void clearDialog() {
		while (vp.getWidget(0)!=hp)
			vp.remove(0);
	}
	
	protected void closeDialog()
	{
		SliderDialog.this.hide();
		sim.sliderDialog = null;
	}
}

