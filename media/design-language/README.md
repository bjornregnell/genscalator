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

