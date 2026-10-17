# CircuitJS1 – Kjerag Labs edition

This fork adds **component ratings**. Resistors, capacitors, diodes, LEDs and zeners
can be given a max power, max voltage and max current in their edit dialog
(0 = not rated).

* At 80 % of any rating the part gets a yellow warning triangle.
* Above 100 % the part burns: it gets a red X and is replaced in the simulation by an
  open circuit or a short (set per part under "Fails as").
* Power and current are averaged with a thermal time constant (default 1 ms) so short
  spikes don't kill parts. Voltage is checked instantly.
* Reset repairs everything. Editing a part repairs that part.
* The toolbar button with the X and warning triangle (or Options > Components Can Break) turns
  breaking off. Parts are then repaired and never burn; overstressed parts get a red-orange
  warning triangle instead. The setting is saved with the circuit (`909 0`/`909 1`); circuits
  without it use the browser's last setting.
* Ratings are saved as an extra `900 P V I tau mode` line after the part. The original
  CircuitJS ignores these lines, so files work in both versions.
* New resistors default to 1/16 W (0402). New LEDs default to 30 mA and 5 V reverse.

**I/O pins and loads** (Draw > I/O Pins and Loads)

* **Generic load** (`L`): runs between a min and max supply voltage and then draws a set
  current. Green when on. Below the minimum it is off (brownout), above the maximum it shuts
  off. A weak supply leaves it partly on. Pin 1 is marked +.
* **Digital input** (`I`): reads its pin against VIL/VIH and shows H, L or ? (in between).
  Optional internal pull-up (to VDD) or pull-down with a resistor value; 0 ohms means none.
  New inputs are rated 3.6 V, so 5 V burns a 3.3 V pin.
* **Digital output** (`O`): click to switch ON/OFF. For each state choose drive high, drive low
  or high-Z, so push-pull, open drain and open source are all possible. Has an output resistance
  (25 ohms) and a 25 mA rating by default. Right-click > Edit to change settings.
* **Analog input**: shows the pin voltage and the ADC reading for the set reference voltage and
  resolution (default 3.3 V, 12-bit), e.g. `1.65 V [2048]`. Turns amber outside 0..Vref. Optional
  input resistance (0 = infinite). Rated 3.6 V like the digital input.
* **Analog output** (`U`): drives a set voltage through an output resistance (0 = ideal), with
  the voltage shown beside it. Edit > Sliders can put the voltage on a slider.

All four support component ratings.

**Tolerance testing** (Edit > Tolerance Testing, or the toolbar button with the resistor and ±)

Resistors, capacitors and inductors have a Tolerance (%) setting (0 = none). New resistors
default to 5% and new capacitors to 20%. In tolerance testing
mode each part with a tolerance gets an up/down switch beside it: up uses the value + tolerance,
down the value - tolerance, and clicking the lit half again goes back to nominal. The part's
value label then shows the value in use, in red (up) or blue (down), and so does the info box. "All nominal" resets every switch; "Exit tolerance testing" leaves the
mode. The mode and switch positions are saved with the circuit, so an exported link opens in
tolerance testing mode. Saved as `906 percent position` after each part and `907 1` for the mode.

**Toolbar**

The toolbar under the menu has edit and zoom buttons and one button per common part. Click a
part, then drag on the canvas to place it; click the button again to go back to select mode.
Buttons with several variants (sources, switches, op amps, transistors, gates) show them on
hover or tap. Options > Toolbar hides it. Part icons come from Paul Falstad's toolbar in
[pfalstad/circuitjs1](https://github.com/pfalstad/circuitjs1).

**Keyboard**

| Key | Does |
|---|---|
| `v` | voltage source (1-terminal) |
| `V` | voltmeter (2-terminal probe) |
| `b` | battery (2-terminal voltage source) |
| `a` | op amp |
| `A` | op amp with V+ / V- supply pins |
| `l` | net label: labels with the same name are connected |
| `D` | LED (moved from `l`) |
| `i` | inductor (moved from `L`) |
| `L` | generic load |
| `I` | digital input pin |
| `O` | digital output pin |
| `U` | analog output pin |
| `r` | rotate 90 degrees |
| `x` | mirror left/right |
| `y` | mirror up/down |

`r`, `x` and `y` act on the selection, or on the part under the mouse when nothing is
selected. With nothing selected or hovered, `r` still places a resistor. The same actions are in
the Edit menu and the right-click menu. Mirroring an op amp up/down swaps its inputs but keeps
V+ on top.

The supply-pin op amp limits its output to the rails (minus an optional swing loss) and draws
its output current from V+ or returns it to V-, so supplies carry the load current.

**Paper theme**

Paper is the default theme. Options > Paper Theme gives a beige paper background with light red for positive voltage,
gray for neutral and light blue for negative. The choice is remembered per browser, and
`?paper=true` in the URL turns it on for a link.

**LEDs**

LEDs are drawn as a diode with two emission arrows. The triangle, bar and arrows take the
LED's colour and get brighter with current. Set the colour in the edit dialog as a hex code,
with the colour picker next to it, or with one of the preset swatches.

**Sharing circuits**

* File > Export As Link puts the whole circuit in the URL: `https://circuits.kjeraglabs.no/?ctz=...`
* Premade circuits: save a circuit as `war/shared/<name>.txt` and push. It opens at
  `https://circuits.kjeraglabs.no/<name>`. This works through `404.html`, which `build.sh`
  makes from the main page, so the site must be served from the domain root.

**Branches and deploying**

| Branch | Site |
|---|---|
| `main` | https://circuits.kjeraglabs.no/ (live) |
| `dev`  | https://circuits.kjeraglabs.no/dev/ (staging, marked DEV BUILD) |

Work on `dev`, push, and check the staging site about two minutes later. When it works,
merge `dev` into `main` (a pull request on GitHub, or `git checkout main && git merge dev && git push`)
to put it live. Every push to either branch rebuilds both sites (`.github/workflows/deploy.yml`).

To build locally: `./build.sh` (needs Java 17+; on Windows run it from Git Bash). The site ends up in `dist/`.

# CircuitJS1

## Introduction

CircuitJS1 is an electronic circuit simulator that runs in the browser. It was originally written by Paul Falstad as a Java Applet. It was adapted by Iain Sharp to run in the browser using GWT.

For a hosted version of the application see:

* Paul's Page: [http://www.falstad.com/circuit/](http://www.falstad.com/circuit/)
* Iain's Page: [http://lushprojects.com/circuitjs/](http://lushprojects.com/circuitjs/)

Thanks to: Edward Calver for 15 new components and other improvements; Rodrigo Hausen for file import/export and many other UI improvements; J. Mike Rollins for the Zener diode code; Julius Schmidt for the spark gap code and some examples; Dustin Soodak for help with the user interface improvements; Jacob Calvert for the T Flip Flop; Ben Hayden for scope spectrum; Thomas Reitinger, Krystian Sławiński, Usevalad Khatkevich, Lucio Sciamanna, Mauro Hemerly Gazzani, J. Miguel Silva, and Franck Viard for translations; Andre Adrian for improved emitter coupled oscillator; Felthry for many examples; Colin Howell for code improvements. LZString (c) 2013 pieroxy.

## Building the web application

The tools you will need to build the project are:

* Eclipse, Oxygen version.
* GWT plugin for Eclipse.

Install "Eclipse for Java developers" from [here](https://www.eclipse.org/downloads/packages/). To add the GWT plugin for Eclipse follow the instructions [here](https://gwt-plugins.github.io/documentation/gwt-eclipse-plugin/Download.html).

This repository is a project folder for your Eclipse project space. Once you have a local copy you can then build and run in development mode or build for deployment. Running in super development mode is done by clicking on the "run" icon on the toolbar and choosing http://127.0.0.1:8888/circuitjs.html from the "Development Mode" tab which appears. Building for deployment is done by selecting the project root node and using the GWT button on the Eclipse taskbar and choosing "GWT Compile Project...".

GWT will build its output in to the "war" directory. In the "war" directory the file "iframe.html" is loaded as an iFrame in to the spare space at the bottom of the right hand pannel. It can be used for branding etc.

## Deployment of the web application

* "GWT Compile Project..." as explained above. This will put the outputs in to the "war" directory in the Eclipse project folder. You then need to copy everything in the "war" directory, except the "WEB-INF" directory, on to your web server.
* Customize the header of the file "circuitjs1.html" to include your tracking, favicon etc.
* Customize the "iframe.html" file to include any branding you want in the right hand panel of the application
* The optional file "shortrelay.php" is a server-side script to act as a relay to a URL shortening service to avoid cross-origin problems with a purely client solution. You may want to customize this for your site. If you don't want to use this feature edit the circuitjs1.java file before compiling.
* If you wish to enable dropbox loading and saving a dropbox API app-key is needed. This should be edited in to the circuitjs.html file where needed. If this is not included the relevant features will be disabled.


The link for the full-page version of the application is now:
`http://<your host>/<your path>/circuitjs1.html`
(you can rename the "circuitjs1.html" file if you want too though you should also update "shortrelay.php" if you do).

Just for reference the files should look like this

```
-+ Directory containing the front page (eg "circuitjs")
  +- circuitjs.html - full page version of application
  +- iframe.html - see notes above
  +- shortrelay.php - see notes above
  ++ circuitjs1 (directory)
   +- various files built by GWT
   +- circuits (directory, containing example circuits)
   +- setuplist.txt (index in to example circuit directory)
```
   
## Embedding

You can link to the full page version of the application using the link shown above.

If you want to embed the application in another page then use an iframe with the src being the full-page version.

You can add query parameters to link to change the applications startup behaviour. The following are supported:
```
.../circuitjs.html?cct=<string> // Load the circuit from the URL (like the # in the Java version)
.../circuitjs.html?ctz=<string> // Load the circuit from compressed data in the URL
.../circuitjs.html?startCircuit=<filename> // Loads the circuit named "filename" from the "Circuits" directory
.../circuitjs.html?startCircuitLink=<URL> // Loads the circuit from the specified URL. CURRENTLY THE URL MUST BE A DROPBOX SHARED FILE OR ANOTHER URL THAT SUPPORTS CORS ACCESS FROM THE CLIENT
.../circuitjs.html?euroResistors=true // Set to true to force "Euro" style resistors. If not specified the resistor style will be based on the user's browser's language preferences
.../circuitjs.html?usResistors=true // Set to true to force "US" style resistors. If not specified the resistor style will be based on the user's browser's language preferences
.../circuitjs.html?whiteBackground=<true|false>
.../circuitjs.html?conventionalCurrent=<true|false>
.../circuitjs.html?running=<true|false> // Start the app without the simulation running, default true
.../circuitjs.html?hideSidebar=<true|false> // Hide the sidebar, default false
.../circuitjs.html?hideMenu=<true|false> // Hide the menu, default false
.../circuitjs.html?editable=<true|false> // Allow circuit editing, default true
.../circuitjs.html?positiveColor=%2300ff00 // change positive voltage color (rrggbb)
.../circuitjs.html?negativeColor=%23ff0000 // change negative voltage color
.../circuitjs.html?hideInfoBox=<true|false>
```
## Building an Electron application

The [Electron](https://electronjs.org/) project allows web applications to be distributed as local executables for a variety of platforms. This repository contains the additional files needed to build circuitJS1 as an Electron application.

The general approach to building an Electron application for a particular platform is documented [here](https://electronjs.org/docs/tutorial/application-distribution). The following instructions apply this approach to circuit JS.

To build the Electron application:
* Compile the application using GWT, as above.
* Download and unpack a [pre-built Electron binary directory](https://github.com/electron/electron/releases) version 9.3.2 for the target platform.
* Copy the "app" directory from this repository to the location specified [here](https://electronjs.org/docs/tutorial/application-distribution) in the Electron binary directory structure.
* Copy the "war" directory, containing the compiled CircuitJS1 application, in to the "app" directory the Electron binary directory structure.
* Run the "Electron" executable file. It should automatically load CircuitJS1.

Known limitations of the Electron application:
* "Create short URL" on "Export as URL" doesn't work as it relies on server support.
* For diodes, "Create Simple Model" doesn't work as it relies on a javascript feature that is not supported.

Thanks to @Immortalin for the initial work in applying Electron to CircuitJS1.

## License

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
