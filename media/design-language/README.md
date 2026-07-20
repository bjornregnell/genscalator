# genscalator's design language

Theme: blacksmith, forging, metallurgy and iron

## Colors

```css
--hot-iron-orange: #ee582b; /* sampled from axe-2.jpg */
--vivid-red-orange: #FA4616;  /*  https://colorcodes.io/orange/red-orange-color-codes/ */
--chio-light-blue: #11a7d4;
--cvro-bright-blue: #05b9e9;
--temper-blue: #095c75; /* link/accent blue for LIGHT surfaces; tempering blue, sv. blåanlöpning */

--tempered-iron-purple: #17193f; /* Tempered Iron Purple */
--ctip-bone-white: #e8e6c0; 

--anvil-coal-graphite: #322b25; /* anvil coal graphite */
--cacg-cold-gray: #cdd4da; /* complement to anvil coal graphite */

--neon-pink-purple: #c724b1; /* signal pair, base */
--vivid-bright-green: #38db4e; /* complementary of neon-pink-purple */
```

### Hot Iron Orange (HIO) #ee582b

hot-iron-orange, sampled from axe-2.jpg. Glowing iron at bright-orange working heat, roughly 900-950°C (about 1650-1750°F) *(approximate, read off the forge glow scale; verify against the temp-color chart in References before publishing)*
  * https://codeberg.org/bjornregnell/genscalator/src/branch/main/media/img/axe-2.jpg


#### Complementary color of HIO: #11a7d4


### Vivid Red Orange (VRO) #FA4616

  * https://sv.wikipedia.org/wiki/Smide#Gl%C3%B6dgning

#### Complementary color of VRO #05b9e9

### Tempered Iron Purple (TIP) #17193f

Tempered iron at 282°C (540°F)
  * https://sv.wikipedia.org/wiki/Anl%C3%B6pning#/media/Fil:Tempering_standards_used_in_blacksmithing.JPG
  * https://en.wikipedia.org/wiki/Tempering_(metallurgy)#/media/File:Tempering_colors_in_steel.jpg

#### Complementary color of TIP: #e8e6c0

### Temper Blue (TB): #095c75

Tempering blue: steel tempered at around 300°C turns blue (sv. *blåanlöpning*) — the same oxide
color chart that gives TIP its purple, one step hotter. Derived 2026-07-20 by darkening the CHIO
hue until it clears WCAG AA on ALL light surfaces (white 7.50, bone-white 5.90, cold-gray 5.01) —
the light-surface link/accent blue. On dark surfaces use CHIO/CVRO instead (TB fails there); no
single blue can serve both, so the blues come as a pair like the glow oranges vs the tempered darks.
  * https://en.wikipedia.org/wiki/Tempering_(metallurgy)#/media/File:Tempering_colors_in_steel.jpg
  * https://sv.wikipedia.org/wiki/Anl%C3%B6pning

### Anvil Coal Graphite (ACG): #322b25

A dark coal-stained cast anvil gray/brown sampled from axe-3
  * https://codeberg.org/bjornregnell/genscalator/src/branch/main/media/img/axe-3.jpg

#### Complementary color of ACG: #cdd4da

### Neon Pink Purple (NPP): #c724b1

The SIGNAL pair, base color — state semantics (chips, statusline, alerts) rather than material
iron-and-fire. Measured facts (see the contrast table): the one accent that is AA as TEXT on
white (4.87); on dark it is large-text-only (TIP 3.46) and fails on graphite; as a chip
BACKGROUND its text must be white (4.87) — TIP or bone text stay below AA at chip sizes.

#### Complementary color of NPP: #38db4e (Vivid Bright Green, VBG)

Pure dark-surface signal: AAA-class on the tempered darks (TIP 9.15, ACG 7.57), fails on all
light surfaces — the glow-orange profile, in green. Natural role: the SmartZone/healthy chip on
dark, with TIP text (9.15). Smithy hook if wanted: copper burns green in the forge.

### References:
  * https://colorcodes.io/
  * https://www.scribd.com/document/410753432/Steel-Temp-Color-Chart
  * https://rgbcolorpicker.com/complementary

## Contrast (WCAG 2.1)

Full generated matrix: **[contrast-table-GENERATED.md](contrast-table-GENERATED.md)** (written by
[`contrast.scala`](contrast.scala) — `scala-cli run media/design-language/contrast.scala`;
re-run when the palette changes, never edit the table by hand).

Reading of the table: **body text = bone-white or cold-gray on tempered purple or graphite** (all
four pairings are AAA both ways, so dark-on-light works equally well inverted); **the glow oranges
are accent/heading colors** — fine large on dark (AA on TIP, AA-large on ACG), never body text on
the light surfaces; **the complementary blues live on dark backgrounds only** (CVRO on TIP is even
AAA) and fail on white — which is why **temper blue** exists: the darker sibling that is AA-or-better
on all three light surfaces (and fails on dark; the blues are a pair, one per surface family).

## Fonts

* Fira Code (retina, regular, medium, bold)
* Fira Sans (regular, medium, bold)

## Logo

**Frozen (BR 2026-07-20): the wordmark.** All-lowercase `genscalator` in **Fira Code, weight 700**,
with the **g and s enlarged to 1.35em** — the big letters spell out the `gs` command prefix, so the
mark teaches the vocabulary. Chosen from the candidate grid in [logo-lab.html](logo-lab.html); seen
in context in [preview.html](preview.html).

- **Canonical color mark (dark surfaces only):** smaller letters in HIO `#ee582b`, the enlarged
  g and s one step hotter in VRO `#FA4616` — a heat gradient inside the word. Backgrounds: TIP or
  ACG (both oranges fail contrast on the light surfaces, even at logo sizes — see the contrast table).
- **Light-surface variant (CANDIDATE, not yet ratified):** letters in TIP `#17193f`, g and s in
  temper blue `#095c75` — shown on preview.html's light panel for judgment.
- **Small variant:** the enlarged `gs` pair extracted alone (favicon, statusline brand) — same
  colors, on an ACG rounded tile; sized specimens at 64/32/16 px in logo-lab.html.

Reference implementation (keep in sync with preview.html):

```html
<span class="logo"><span class="gs">g</span>en<span class="gs">s</span>calator</span>
<style>
  .logo { font-family: "Fira Code", ui-monospace, monospace; font-weight: 700; color: #ee582b; }
  .logo .gs { font-size: 1.35em; color: #FA4616; }
</style>
```

TODO for the SM155 logo family: outline the text to SVG paths (so the mark stops depending on Fira
being installed), export the color/black-white/big/small variants, and ratify or replace the
light-surface colors.

## Further reading

* Forging (en) == Smide (sv)
  * https://en.wikipedia.org/wiki/Forging
  * https://sv.wikipedia.org/wiki/Smide
* Tempering (en) == Anlöpning (sv)
  https://en.wikipedia.org/wiki/Tempering_(metallurgy)
  * https://sv.wikipedia.org/wiki/Anl%C3%B6pning

* Annealing (en) == Glödgning (sv)
  * 
  * https://sv.wikipedia.org/wiki/Gl%C3%B6dgning

* Anvil (En) == Städ (sv)
  * https://en.wikipedia.org/wiki/Anvil
  * https://sv.wikipedia.org/wiki/St%C3%A4d

