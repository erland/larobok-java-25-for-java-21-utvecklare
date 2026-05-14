# Exportguide

All export ska utgå från `docs/export-metadata.yaml` och kapitelordningen där.

## Markdown
Rendera rubriker, listor, tabeller, fetstil, kursiv stil och kodblock som riktig formatering.

## EPUB
- Använd metadata för titel, författare, språk och identifierare.
- Skapa inte en separat innehållsförteckning som textkapitel.
- Använd luftig CSS för brödtext, rubriker, tabeller och kod.

## PDF
- Skapa innehållsförteckning i början, före inledningen.
- Innehållsförteckningen ska genereras från rubrikstrukturen.
- Markdown ska renderas, inte visas rått.

## DOCX
- Rubriker, listor, tabeller och kodblock ska renderas med dokumentstilar.
