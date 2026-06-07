export interface AuctionTaxonomyOption {
  code: string;
  label: string;
}

export interface AuctionCategoryOption extends AuctionTaxonomyOption {
  description: string;
  subcategories: AuctionTaxonomyOption[];
}

export const AUCTION_CATEGORIES: AuctionCategoryOption[] = [
  {
    code: 'RARE_BOOKS',
    label: 'Carti rare',
    description: 'Prime editii, volume semnate si editii de colectie.',
    subcategories: [
      { code: 'FIRST_EDITIONS', label: 'Prime editii' },
      { code: 'LIMITED_EDITIONS', label: 'Editii limitate' },
      { code: 'SIGNED_COPIES', label: 'Exemplare semnate' },
      { code: 'ANTIQUE_BOOKS', label: 'Carti vechi' },
      { code: 'ART_ALBUMS', label: 'Albume de arta' }
    ]
  },
  {
    code: 'MANUSCRIPTS',
    label: 'Manuscrise',
    description: 'Texte originale, note si corespondenta manuscrisa.',
    subcategories: [
      { code: 'LITERARY_MANUSCRIPTS', label: 'Manuscrise literare' },
      { code: 'HISTORICAL_MANUSCRIPTS', label: 'Manuscrise istorice' },
      { code: 'PERSONAL_NOTES', label: 'Note personale' },
      { code: 'ORIGINAL_LETTERS', label: 'Scrisori originale' }
    ]
  },
  {
    code: 'HISTORICAL_DOCUMENTS',
    label: 'Documente istorice',
    description: 'Acte oficiale, documente politice, militare sau nobiliare.',
    subcategories: [
      { code: 'OFFICIAL_ACTS', label: 'Acte oficiale' },
      { code: 'POLITICAL_DOCUMENTS', label: 'Documente politice' },
      { code: 'MILITARY_DOCUMENTS', label: 'Documente militare' },
      { code: 'ROYAL_NOBLE_DOCUMENTS', label: 'Documente regale / nobiliare' }
    ]
  },
  {
    code: 'MAPS_AND_ATLASES',
    label: 'Harti si atlase',
    description: 'Harti istorice, atlase si planuri urbane de colectie.',
    subcategories: [
      { code: 'ANTIQUE_MAPS', label: 'Harti vechi' },
      { code: 'ATLASES', label: 'Atlase' },
      { code: 'URBAN_PLANS', label: 'Planuri urbane' },
      { code: 'MILITARY_MAPS', label: 'Harti militare' }
    ]
  },
  {
    code: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    label: 'Fotografii si arhive vizuale',
    description: 'Fotografii istorice, portrete, albume si carti postale.',
    subcategories: [
      { code: 'HISTORICAL_PHOTOGRAPHS', label: 'Fotografii istorice' },
      { code: 'PORTRAITS', label: 'Portrete' },
      { code: 'ARCHIVE_ALBUMS', label: 'Albume de arhiva' },
      { code: 'POSTCARDS', label: 'Carti postale' }
    ]
  },
  {
    code: 'COLLECTIBLE_PRINTS',
    label: 'Tiparituri de colectie',
    description: 'Ziare, reviste, afise si programe de eveniment.',
    subcategories: [
      { code: 'NEWSPAPERS', label: 'Ziare vechi' },
      { code: 'RARE_MAGAZINES', label: 'Reviste rare' },
      { code: 'POSTERS', label: 'Afise' },
      { code: 'EVENT_PROGRAMS', label: 'Programe de eveniment' }
    ]
  },
  {
    code: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    label: 'Documente stiintifice si academice',
    description: 'Lucrari academice, brevete si publicatii rare.',
    subcategories: [
      { code: 'ACADEMIC_PAPERS', label: 'Lucrari academice' },
      { code: 'RESEARCH_NOTES', label: 'Note de cercetare' },
      { code: 'PATENTS', label: 'Brevete' },
      { code: 'RARE_PUBLICATIONS', label: 'Publicatii rare' }
    ]
  }
];

export const ITEM_CONDITIONS: AuctionTaxonomyOption[] = [
  { code: 'EXCELLENT', label: 'Excelenta' },
  { code: 'VERY_GOOD', label: 'Foarte buna' },
  { code: 'GOOD', label: 'Buna' },
  { code: 'FAIR', label: 'Satisfacatoare' },
  { code: 'FRAGILE', label: 'Fragila' },
  { code: 'DETERIORATED', label: 'Deteriorata' }
];

export const AUTHENTICITY_STATUSES: AuctionTaxonomyOption[] = [
  { code: 'UNVERIFIED', label: 'Neverificata' },
  { code: 'IN_REVIEW', label: 'In curs de verificare' },
  { code: 'VERIFIED', label: 'Verificata' },
  { code: 'REJECTED', label: 'Respinsa' }
];

export function findCategoryByCode(code: string | null | undefined): AuctionCategoryOption | undefined {
  return AUCTION_CATEGORIES.find((category) => category.code === code);
}

export function findOptionLabel(options: AuctionTaxonomyOption[], code: string | null | undefined): string | null {
  return options.find((option) => option.code === code)?.label ?? null;
}
