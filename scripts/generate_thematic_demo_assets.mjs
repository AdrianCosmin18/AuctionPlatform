import fs from 'node:fs';
import path from 'node:path';

const repoRoot = process.cwd();
const outputDir = path.join(repoRoot, 'auction-api', 'src', 'main', 'resources', 'static', 'demo-images');
const migrationPath = path.join(repoRoot, 'auction-api', 'src', 'main', 'resources', 'db', 'migration', 'V11__translate_seed_auctions_and_add_demo_images.sql');

const categoryThemes = {
  RARE_BOOKS: {
    label: 'Rare Books',
    palette: ['#8b5e34', '#f4e3c3', '#20150f'],
    motif: 'book'
  },
  MANUSCRIPTS: {
    label: 'Manuscripts',
    palette: ['#6f4e37', '#f2e8d8', '#1f1a17'],
    motif: 'quill'
  },
  HISTORICAL_DOCUMENTS: {
    label: 'Historical Documents',
    palette: ['#7c3f2f', '#efe3cf', '#241713'],
    motif: 'seal'
  },
  MAPS_AND_ATLASES: {
    label: 'Maps & Atlases',
    palette: ['#2f5f73', '#efe2c7', '#10212b'],
    motif: 'compass'
  },
  PHOTOGRAPHS_AND_VISUAL_ARCHIVES: {
    label: 'Photographs & Visual Archives',
    palette: ['#4b5563', '#f5efe5', '#111827'],
    motif: 'photo'
  },
  COLLECTIBLE_PRINTS: {
    label: 'Collectible Prints',
    palette: ['#8f3b2e', '#f6ead7', '#201415'],
    motif: 'poster'
  },
  SCIENTIFIC_AND_ACADEMIC_DOCUMENTS: {
    label: 'Scientific & Academic Documents',
    palette: ['#2e5a88', '#eef2f7', '#111827'],
    motif: 'diagram'
  }
};

const auctions = [
  {
    originalTitle: 'Prima editie Mihai Eminescu - Poezii',
    title: 'First Edition Mihai Eminescu - Poems',
    description: 'Private library copy with minimal wear and the original publisher binding.',
    provenance: 'Private library collection from Iasi.',
    category: 'RARE_BOOKS',
    creator: 'Mihai Eminescu',
    year: 1884,
    slug: 'first-edition-mihai-eminescu-poems'
  },
  {
    originalTitle: 'Editie limitata Tudor Arghezi',
    title: 'Limited Tudor Arghezi Edition',
    description: 'Numbered copy from a reduced print run, preserved in excellent condition.',
    provenance: 'Inherited documentary holding from a family library.',
    category: 'RARE_BOOKS',
    creator: 'Tudor Arghezi',
    year: 1931,
    slug: 'limited-tudor-arghezi-edition'
  },
  {
    originalTitle: 'Exemplar semnat de Mircea Eliade',
    title: 'Signed Copy by Mircea Eliade',
    description: 'Author signature on the title page with a short handwritten dedication.',
    provenance: 'Acquired from an antiquarian bookshop in Bucharest.',
    category: 'RARE_BOOKS',
    creator: 'Mircea Eliade',
    year: 1936,
    slug: 'signed-copy-mircea-eliade'
  },
  {
    originalTitle: 'Carte bisericeasca de secol XIX',
    title: '19th Century Church Book',
    description: 'Religious printed volume with woodcut illustrations and ownership notes.',
    provenance: 'Sourced from a rural parish archive.',
    category: 'RARE_BOOKS',
    creator: 'Metropolitan Press',
    year: 1827,
    slug: '19th-century-church-book'
  },
  {
    originalTitle: 'Album de arta interbelica - Bucuresti',
    title: 'Interwar Art Album - Bucharest',
    description: 'Illustrated album with curated reproductions and commentary from the period.',
    provenance: 'Family archive, Bucharest.',
    category: 'RARE_BOOKS',
    creator: 'Octav Bancila',
    year: 1929,
    slug: 'interwar-art-album-bucharest'
  },
  {
    originalTitle: 'Manuscris poetic simbolist',
    title: 'Symbolist Poetic Manuscript',
    description: 'Handwritten notebook with layered revisions and marginal annotations.',
    provenance: 'Recovered from a private literary collection.',
    category: 'MANUSCRIPTS',
    creator: 'Alexandru Macedonski',
    year: 1902,
    slug: 'symbolist-poetic-manuscript'
  },
  {
    originalTitle: 'Cronica manuscrisa de epoca',
    title: 'Period Handwritten Chronicle',
    description: 'Historical manuscript with an identifiable watermark and archival paper stock.',
    provenance: 'Private archival holding from Transylvania.',
    category: 'MANUSCRIPTS',
    creator: 'Anonymous',
    year: 1815,
    slug: 'period-handwritten-chronicle'
  },
  {
    originalTitle: 'Jurnal personal de front',
    title: 'Personal Frontline Diary',
    description: 'Daily wartime notes with rapid sketches executed in campaign conditions.',
    provenance: 'Inherited by the same family in Brasov.',
    category: 'MANUSCRIPTS',
    creator: 'Lt. Nicolae Popescu',
    year: 1917,
    slug: 'personal-frontline-diary'
  },
  {
    originalTitle: 'Scrisoare originala catre un editor francez',
    title: 'Original Letter to a French Publisher',
    description: 'Literary correspondence with signature and original mailing envelope.',
    provenance: 'From a Parisian private collection.',
    category: 'MANUSCRIPTS',
    creator: 'Panait Istrati',
    year: 1925,
    slug: 'original-letter-french-publisher'
  },
  {
    originalTitle: 'Caiet de note diplomatice',
    title: 'Diplomatic Notes Notebook',
    description: 'Pocket notebook with protocol notes and period proper names.',
    provenance: 'Acquired as part of a mixed documentary lot.',
    category: 'MANUSCRIPTS',
    creator: 'Anonymous Diplomat',
    year: 1938,
    slug: 'diplomatic-notes-notebook'
  },
  {
    originalTitle: 'Decret administrativ cu sigiliu uscat',
    title: 'Administrative Decree with Dry Seal',
    description: 'Official act with signature and a partially preserved embossed seal.',
    provenance: 'Collected from a boyar family archive.',
    category: 'HISTORICAL_DOCUMENTS',
    creator: 'United Principalities Administration',
    year: 1862,
    slug: 'administrative-decree-dry-seal'
  },
  {
    originalTitle: 'Proclamatie politica de campanie',
    title: 'Political Campaign Proclamation',
    description: 'Political broadside with handwritten amendments and contextual notes.',
    provenance: 'Recovered from a decommissioned party archive.',
    category: 'HISTORICAL_DOCUMENTS',
    creator: 'Local Electoral Committee',
    year: 1928,
    slug: 'political-campaign-proclamation'
  },
  {
    originalTitle: 'Ordin militar de mobilizare',
    title: 'Military Mobilization Order',
    description: 'Signed military order with registry numbering and unit stamp.',
    provenance: 'Private military-history collection, Cluj.',
    category: 'HISTORICAL_DOCUMENTS',
    creator: 'Ministry of War',
    year: 1916,
    slug: 'military-mobilization-order'
  },
  {
    originalTitle: 'Diploma nobiliara cu blazon',
    title: 'Noble Diploma with Heraldic Arms',
    description: 'Parchment charter with heraldic imagery and traces of red sealing wax.',
    provenance: 'Transylvanian family holding.',
    category: 'HISTORICAL_DOCUMENTS',
    creator: 'Royal Chancery',
    year: 1784,
    slug: 'noble-diploma-heraldic-arms'
  },
  {
    originalTitle: 'Autorizatie comerciala de inceput de secol XX',
    title: 'Early 20th Century Trade License',
    description: 'Municipal document with multiple signatures and the original letterhead.',
    provenance: 'Acquired from a notary archive lot.',
    category: 'HISTORICAL_DOCUMENTS',
    creator: 'City Hall of Bucharest',
    year: 1908,
    slug: 'early-20th-century-trade-license'
  },
  {
    originalTitle: 'Harta veche a Moldovei',
    title: 'Antique Map of Moldavia',
    description: 'Hand-colored engraving with wide margins and an ornamental cartouche.',
    provenance: 'Collected in Belgium and imported in 1998.',
    category: 'MAPS_AND_ATLASES',
    creator: 'Guillaume Delisle',
    year: 1718,
    slug: 'antique-map-of-moldavia'
  },
  {
    originalTitle: 'Atlas scolar de geografie istorica',
    title: 'School Atlas of Historical Geography',
    description: 'Complete atlas with color plates and the original binding.',
    provenance: 'Private library of a university professor.',
    category: 'MAPS_AND_ATLASES',
    creator: 'Geographical Institute',
    year: 1899,
    slug: 'school-atlas-historical-geography'
  },
  {
    originalTitle: 'Plan urbanistic al Bucurestiului',
    title: 'Urban Plan of Bucharest',
    description: 'Fold-out city plan with administrative marks and commercial zoning.',
    provenance: 'Archive of a former urban planner.',
    category: 'MAPS_AND_ATLASES',
    creator: 'Bucharest Technical Service',
    year: 1935,
    slug: 'urban-plan-of-bucharest'
  },
  {
    originalTitle: 'Harta militara a Dobrogei',
    title: 'Military Map of Dobruja',
    description: 'Campaign edition with topographic symbols and working folds.',
    provenance: 'From a military-history collection.',
    category: 'MAPS_AND_ATLASES',
    creator: 'General Staff',
    year: 1917,
    slug: 'military-map-of-dobruja'
  },
  {
    originalTitle: 'Harta turistica a Carpatilor',
    title: 'Tourist Map of the Carpathians',
    description: 'Early 20th century print with manually marked mountain routes.',
    provenance: 'Acquired from a mixed lot of antique stationery.',
    category: 'MAPS_AND_ATLASES',
    creator: 'Romanian Touring Club',
    year: 1932,
    slug: 'tourist-map-of-the-carpathians'
  },
  {
    originalTitle: 'Fotografie istorica a Calea Victoriei',
    title: 'Historic Photograph of Calea Victoriei',
    description: 'Albumen print mounted on board with photographer studio inscription.',
    provenance: 'Urban photography holding, Bucharest.',
    category: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    creator: 'Franz Duschek',
    year: 1889,
    slug: 'historic-photograph-calea-victoriei'
  },
  {
    originalTitle: 'Portret de atelier al unui avocat interbelic',
    title: 'Studio Portrait of an Interwar Lawyer',
    description: 'Sepia portrait with the studio stamp preserved on the reverse.',
    provenance: 'Family provenance, Ploiesti.',
    category: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    creator: 'Splendid Photo Studio',
    year: 1931,
    slug: 'studio-portrait-interwar-lawyer'
  },
  {
    originalTitle: 'Album de familie din perioada Regatului',
    title: 'Family Album from the Royal Era',
    description: 'Complete family album with 42 photographs and handwritten notes.',
    provenance: 'Kept by the same family to the present day.',
    category: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    creator: 'Ionescu Family',
    year: 1912,
    slug: 'family-album-royal-era'
  },
  {
    originalTitle: 'Carte postala ilustrata - Sinaia',
    title: 'Illustrated Postcard - Sinaia',
    description: 'Travelled postcard with stamp and readable postal cancellation.',
    provenance: 'Recent acquisition from a postcard collection.',
    category: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    creator: 'Socec Publishing',
    year: 1906,
    slug: 'illustrated-postcard-sinaia'
  },
  {
    originalTitle: 'Lot de fotografii feroviare',
    title: 'Lot of Railway Photographs',
    description: 'Set of eight period photographs featuring locomotives and regional stations.',
    provenance: 'Acquired from an earlier industrial memorabilia auction.',
    category: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    creator: 'Unknown Photographer',
    year: 1922,
    slug: 'lot-of-railway-photographs'
  },
  {
    originalTitle: 'Ziar despre Unirea Principatelor',
    title: 'Newspaper on the Union of the Principalities',
    description: 'Historic newspaper issue with political reports and period advertisements.',
    provenance: 'Collection acquired from a regional auction house.',
    category: 'COLLECTIBLE_PRINTS',
    creator: 'National Printing House',
    year: 1859,
    slug: 'newspaper-union-of-principalities'
  },
  {
    originalTitle: 'Revista literara avangardista',
    title: 'Avant-Garde Literary Magazine',
    description: 'Complete issue with illustrations and distinctive advertisements of the period.',
    provenance: 'Private library focused on literary history.',
    category: 'COLLECTIBLE_PRINTS',
    creator: 'Integral Editorial Board',
    year: 1926,
    slug: 'avant-garde-literary-magazine'
  },
  {
    originalTitle: 'Afis de teatru - premiera interbelica',
    title: 'Theatre Poster - Interwar Premiere',
    description: 'Original color-printed poster with minor edge losses from display use.',
    provenance: 'Rescued from a props warehouse.',
    category: 'COLLECTIBLE_PRINTS',
    creator: 'National Theatre',
    year: 1934,
    slug: 'theatre-poster-interwar-premiere'
  },
  {
    originalTitle: 'Program de opera cu distributie originala',
    title: 'Opera Program with Original Cast',
    description: 'Performance booklet with annotations from a contemporary attendee.',
    provenance: 'Private collection of cultural memorabilia.',
    category: 'COLLECTIBLE_PRINTS',
    creator: 'Romanian Opera',
    year: 1921,
    slug: 'opera-program-original-cast'
  },
  {
    originalTitle: 'Supliment ilustrat de expozitie universala',
    title: 'Illustrated Universal Exposition Supplement',
    description: 'Promotional publication with fold-outs and technical engravings.',
    provenance: 'From the archive of an engineering family.',
    category: 'COLLECTIBLE_PRINTS',
    creator: 'Exposition Committee',
    year: 1900,
    slug: 'illustrated-universal-exposition-supplement'
  },
  {
    originalTitle: 'Teza universitara de medicina',
    title: 'University Medical Thesis',
    description: 'Hand-bound academic work with approvals and official signatures.',
    provenance: 'Decommissioned university medical holding.',
    category: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    creator: 'Dr. Petre Georgescu',
    year: 1898,
    slug: 'university-medical-thesis'
  },
  {
    originalTitle: 'Caiet de laborator chimic',
    title: 'Chemistry Laboratory Notebook',
    description: 'Research notebook with formulas, observations, and authentic work stains.',
    provenance: 'Recovered from a technical institute archive.',
    category: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    creator: 'Prof. C. I. Istrati',
    year: 1904,
    slug: 'chemistry-laboratory-notebook'
  },
  {
    originalTitle: 'Brevet de inventie pentru aparat optic',
    title: 'Patent for an Optical Device',
    description: 'Technical patent document with appended plates and filing stamps.',
    provenance: 'Secondary lot from a technical-document auction.',
    category: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    creator: 'Eng. Gheorghe Marinescu',
    year: 1937,
    slug: 'patent-optical-device'
  },
  {
    originalTitle: 'Publicatie stiintifica rara despre geologie',
    title: 'Rare Scientific Publication on Geology',
    description: 'Complete fascicle with fold-out plates and reading annotations.',
    provenance: 'Acquired from a private university library.',
    category: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    creator: 'Geological Society',
    year: 1887,
    slug: 'rare-scientific-publication-geology'
  },
  {
    originalTitle: 'Lot de prelegeri academice dactilografiate',
    title: 'Lot of Typed Academic Lectures',
    description: 'Set of interwar course notes and seminar material in typed form.',
    provenance: 'Archive inherited from a university professor.',
    category: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    creator: 'Faculty of Letters',
    year: 1933,
    slug: 'lot-of-typed-academic-lectures'
  }
];

const escapeXml = (value) => value
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;');

const escapeSql = (value) => value.replaceAll("'", "''");

const motifMarkup = (motif, accent, ink) => {
  switch (motif) {
    case 'book':
      return `
        <rect x="160" y="180" width="300" height="420" rx="26" fill="${accent}" opacity="0.9"/>
        <rect x="190" y="210" width="240" height="360" rx="18" fill="#fff8ec" opacity="0.9"/>
        <line x1="250" y1="210" x2="250" y2="570" stroke="${ink}" stroke-opacity="0.14" stroke-width="6"/>
        <line x1="215" y1="300" x2="405" y2="300" stroke="${ink}" stroke-opacity="0.2" stroke-width="4"/>
        <line x1="215" y1="350" x2="405" y2="350" stroke="${ink}" stroke-opacity="0.2" stroke-width="4"/>
      `;
    case 'quill':
      return `
        <path d="M178 540c118-60 226-176 292-334 49 31 72 89 62 144-24 127-145 249-291 312l-95 18 32-81z" fill="${accent}" opacity="0.9"/>
        <path d="M227 596c78-71 159-171 246-305" stroke="#fff6e5" stroke-width="14" stroke-linecap="round" opacity="0.78"/>
        <path d="M182 622l160-36" stroke="${ink}" stroke-opacity="0.25" stroke-width="8" stroke-linecap="round"/>
      `;
    case 'seal':
      return `
        <rect x="160" y="180" width="330" height="410" rx="24" fill="#fff7ea" opacity="0.92"/>
        <circle cx="430" cy="530" r="76" fill="${accent}" opacity="0.88"/>
        <path d="M430 476l15 30 34 5-24 24 6 34-31-16-31 16 6-34-24-24 34-5z" fill="#fff4dc"/>
        <line x1="210" y1="260" x2="440" y2="260" stroke="${ink}" stroke-opacity="0.22" stroke-width="5"/>
        <line x1="210" y1="318" x2="392" y2="318" stroke="${ink}" stroke-opacity="0.22" stroke-width="5"/>
      `;
    case 'compass':
      return `
        <circle cx="332" cy="386" r="154" fill="#fff7e8" opacity="0.92"/>
        <circle cx="332" cy="386" r="112" fill="none" stroke="${accent}" stroke-width="18" opacity="0.65"/>
        <path d="M332 238l33 112 112 36-112 36-33 112-33-112-112-36 112-36z" fill="${accent}" opacity="0.9"/>
        <circle cx="332" cy="386" r="18" fill="${ink}" opacity="0.72"/>
      `;
    case 'photo':
      return `
        <rect x="162" y="168" width="330" height="430" rx="20" fill="#fff7eb" opacity="0.98"/>
        <rect x="192" y="198" width="270" height="290" rx="18" fill="${accent}" opacity="0.88"/>
        <circle cx="282" cy="286" r="42" fill="#fff4d8" opacity="0.75"/>
        <path d="M208 450l78-96 56 63 58-52 62 85z" fill="#fff1dc" opacity="0.82"/>
      `;
    case 'poster':
      return `
        <rect x="154" y="160" width="340" height="440" rx="26" fill="#fff6e7" opacity="0.96"/>
        <rect x="184" y="196" width="280" height="120" rx="16" fill="${accent}" opacity="0.88"/>
        <line x1="208" y1="364" x2="440" y2="364" stroke="${ink}" stroke-opacity="0.22" stroke-width="6"/>
        <line x1="208" y1="418" x2="440" y2="418" stroke="${ink}" stroke-opacity="0.22" stroke-width="6"/>
        <line x1="208" y1="472" x2="400" y2="472" stroke="${ink}" stroke-opacity="0.22" stroke-width="6"/>
      `;
    case 'diagram':
      return `
        <rect x="154" y="160" width="344" height="444" rx="24" fill="#f8fbff" opacity="0.98"/>
        <circle cx="262" cy="300" r="52" fill="none" stroke="${accent}" stroke-width="16" opacity="0.86"/>
        <circle cx="390" cy="344" r="76" fill="none" stroke="${accent}" stroke-width="12" opacity="0.56"/>
        <line x1="220" y1="456" x2="446" y2="456" stroke="${ink}" stroke-opacity="0.24" stroke-width="6"/>
        <line x1="220" y1="510" x2="398" y2="510" stroke="${ink}" stroke-opacity="0.24" stroke-width="6"/>
      `;
    default:
      return '';
  }
};

const buildSvg = (auction) => {
  const theme = categoryThemes[auction.category];
  const [accent, paper, ink] = theme.palette;
  const titleLines = wrapText(auction.title, 24, 3);
  const creatorLine = `${auction.creator} · ${auction.year}`;

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1200" viewBox="0 0 1600 1200" role="img" aria-labelledby="title desc">
  <title>${escapeXml(auction.title)}</title>
  <desc>${escapeXml(auction.description)}</desc>
  <defs>
    <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0%" stop-color="${paper}"/>
      <stop offset="100%" stop-color="#ffffff"/>
    </linearGradient>
    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
      <feDropShadow dx="0" dy="18" stdDeviation="24" flood-color="#0f172a" flood-opacity="0.12"/>
    </filter>
  </defs>
  <rect width="1600" height="1200" fill="url(#bg)"/>
  <circle cx="1360" cy="210" r="260" fill="${accent}" fill-opacity="0.08"/>
  <circle cx="180" cy="1080" r="280" fill="${accent}" fill-opacity="0.12"/>
  <rect x="76" y="76" width="1448" height="1048" rx="44" fill="#ffffff" fill-opacity="0.8" stroke="${ink}" stroke-opacity="0.08"/>
  <g filter="url(#shadow)">
    <rect x="110" y="110" width="1380" height="980" rx="36" fill="#fffefb"/>
  </g>
  <g transform="translate(130 160)">
    <text x="0" y="0" fill="${accent}" font-size="34" font-family="Georgia, 'Times New Roman', serif" font-weight="700" letter-spacing="6">${escapeXml(theme.label.toUpperCase())}</text>
    <text x="0" y="72" fill="${ink}" font-size="92" font-family="Georgia, 'Times New Roman', serif" font-weight="700">${escapeXml(titleLines[0] ?? '')}</text>
    <text x="0" y="172" fill="${ink}" font-size="92" font-family="Georgia, 'Times New Roman', serif" font-weight="700">${escapeXml(titleLines[1] ?? '')}</text>
    <text x="0" y="272" fill="${ink}" font-size="92" font-family="Georgia, 'Times New Roman', serif" font-weight="700">${escapeXml(titleLines[2] ?? '')}</text>
    <text x="0" y="362" fill="${ink}" fill-opacity="0.62" font-size="38" font-family="'Segoe UI', Arial, sans-serif">${escapeXml(creatorLine)}</text>
    <text x="0" y="432" fill="${ink}" fill-opacity="0.72" font-size="34" font-family="'Segoe UI', Arial, sans-serif">${escapeXml(auction.description)}</text>
    <text x="0" y="842" fill="${ink}" fill-opacity="0.56" font-size="28" font-family="'Segoe UI', Arial, sans-serif">Provenance</text>
    <text x="0" y="892" fill="${ink}" fill-opacity="0.8" font-size="34" font-family="'Segoe UI', Arial, sans-serif">${escapeXml(auction.provenance)}</text>
  </g>
  <g transform="translate(930 258)">
    <rect width="450" height="560" rx="40" fill="${accent}" fill-opacity="0.14"/>
    ${motifMarkup(theme.motif, accent, ink)}
  </g>
  <rect x="130" y="1010" width="360" height="52" rx="26" fill="${accent}" fill-opacity="0.12"/>
  <text x="160" y="1044" fill="${accent}" font-size="28" font-family="'Segoe UI', Arial, sans-serif" font-weight="700">ArchiveBid Curated Demo Lot</text>
</svg>`;
};

const wrapText = (value, maxLineLength, maxLines) => {
  const words = value.split(' ');
  const lines = [];
  let current = '';
  let consumed = 0;

  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word;
    if (candidate.length <= maxLineLength || !current) {
      current = candidate;
      consumed += 1;
      continue;
    }

    lines.push(current);
    current = word;
    consumed += 1;

    if (lines.length === maxLines - 1) {
      break;
    }
  }

  const remainingWords = lines.length === maxLines - 1 ? words.slice(consumed - 1).join(' ') : current;
  if (remainingWords) {
    lines.push(remainingWords);
  }

  return lines.slice(0, maxLines).map((line, index, source) => {
    if (index === source.length - 1 && line.length > maxLineLength + 10) {
      return `${line.slice(0, maxLineLength + 7).trimEnd()}...`;
    }
    return line;
  });
};

const sqlStatements = [
  '-- Generated by scripts/generate_thematic_demo_assets.mjs',
  "delete from auction_images where image_url like '/demo-images/%';"
];

fs.mkdirSync(outputDir, { recursive: true });

for (const auction of auctions) {
  const fileName = `${auction.slug}.svg`;
  const filePath = path.join(outputDir, fileName);
  fs.writeFileSync(filePath, buildSvg(auction), 'utf8');

  sqlStatements.push(`update auctions set\n  title = '${escapeSql(auction.title)}',\n  description = '${escapeSql(auction.description)}',\n  provenance = '${escapeSql(auction.provenance)}'\nwhere title = '${escapeSql(auction.originalTitle)}';`);

  sqlStatements.push(`insert into auction_images (auction_id, image_url, display_order)\nselect a.id, '/demo-images/${fileName}', 0\nfrom auctions a\nwhere a.title = '${escapeSql(auction.title)}'\n  and not exists (\n    select 1\n    from auction_images ai\n    where ai.auction_id = a.id\n      and ai.image_url = '/demo-images/${fileName}'\n  );`);
}

fs.writeFileSync(migrationPath, `${sqlStatements.join('\n\n')}\n`, 'utf8');
console.log(`Generated ${auctions.length} demo images.`);
console.log(`Migration written to ${migrationPath}`);

