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
    label: 'Rare Books',
    description: 'First editions, signed volumes, and collectible print runs.',
    subcategories: [
      { code: 'FIRST_EDITIONS', label: 'First Editions' },
      { code: 'LIMITED_EDITIONS', label: 'Limited Editions' },
      { code: 'SIGNED_COPIES', label: 'Signed Copies' },
      { code: 'ANTIQUE_BOOKS', label: 'Antique Books' },
      { code: 'ART_ALBUMS', label: 'Art Albums' }
    ]
  },
  {
    code: 'MANUSCRIPTS',
    label: 'Manuscripts',
    description: 'Original texts, notes, and handwritten correspondence.',
    subcategories: [
      { code: 'LITERARY_MANUSCRIPTS', label: 'Literary Manuscripts' },
      { code: 'HISTORICAL_MANUSCRIPTS', label: 'Historical Manuscripts' },
      { code: 'PERSONAL_NOTES', label: 'Personal Notes' },
      { code: 'ORIGINAL_LETTERS', label: 'Original Letters' }
    ]
  },
  {
    code: 'HISTORICAL_DOCUMENTS',
    label: 'Historical Documents',
    description: 'Official acts, political papers, military records, and noble documents.',
    subcategories: [
      { code: 'OFFICIAL_ACTS', label: 'Official Acts' },
      { code: 'POLITICAL_DOCUMENTS', label: 'Political Documents' },
      { code: 'MILITARY_DOCUMENTS', label: 'Military Documents' },
      { code: 'ROYAL_NOBLE_DOCUMENTS', label: 'Royal / Noble Documents' }
    ]
  },
  {
    code: 'MAPS_AND_ATLASES',
    label: 'Maps & Atlases',
    description: 'Historic maps, atlases, and collectible urban plans.',
    subcategories: [
      { code: 'ANTIQUE_MAPS', label: 'Antique Maps' },
      { code: 'ATLASES', label: 'Atlases' },
      { code: 'URBAN_PLANS', label: 'Urban Plans' },
      { code: 'MILITARY_MAPS', label: 'Military Maps' }
    ]
  },
  {
    code: 'PHOTOGRAPHS_AND_VISUAL_ARCHIVES',
    label: 'Photographs & Visual Archives',
    description: 'Historic photography, portraits, albums, and postcards.',
    subcategories: [
      { code: 'HISTORICAL_PHOTOGRAPHS', label: 'Historical Photographs' },
      { code: 'PORTRAITS', label: 'Portraits' },
      { code: 'ARCHIVE_ALBUMS', label: 'Archive Albums' },
      { code: 'POSTCARDS', label: 'Postcards' }
    ]
  },
  {
    code: 'COLLECTIBLE_PRINTS',
    label: 'Collectible Prints',
    description: 'Newspapers, rare magazines, posters, and event programs.',
    subcategories: [
      { code: 'NEWSPAPERS', label: 'Newspapers' },
      { code: 'RARE_MAGAZINES', label: 'Rare Magazines' },
      { code: 'POSTERS', label: 'Posters' },
      { code: 'EVENT_PROGRAMS', label: 'Event Programs' }
    ]
  },
  {
    code: 'SCIENTIFIC_AND_ACADEMIC_DOCUMENTS',
    label: 'Scientific & Academic Documents',
    description: 'Academic papers, patents, and rare publications.',
    subcategories: [
      { code: 'ACADEMIC_PAPERS', label: 'Academic Papers' },
      { code: 'RESEARCH_NOTES', label: 'Research Notes' },
      { code: 'PATENTS', label: 'Patents' },
      { code: 'RARE_PUBLICATIONS', label: 'Rare Publications' }
    ]
  }
];

export const ITEM_CONDITIONS: AuctionTaxonomyOption[] = [
  { code: 'EXCELLENT', label: 'Excellent' },
  { code: 'VERY_GOOD', label: 'Very Good' },
  { code: 'GOOD', label: 'Good' },
  { code: 'FAIR', label: 'Fair' },
  { code: 'FRAGILE', label: 'Fragile' },
  { code: 'DETERIORATED', label: 'Deteriorated' }
];

export const AUTHENTICITY_STATUSES: AuctionTaxonomyOption[] = [
  { code: 'UNVERIFIED', label: 'Unverified' },
  { code: 'IN_REVIEW', label: 'In Review' },
  { code: 'VERIFIED', label: 'Verified' },
  { code: 'REJECTED', label: 'Rejected' }
];

export function findCategoryByCode(code: string | null | undefined): AuctionCategoryOption | undefined {
  return AUCTION_CATEGORIES.find((category) => category.code === code);
}

export function findOptionLabel(options: AuctionTaxonomyOption[], code: string | null | undefined): string | null {
  return options.find((option) => option.code === code)?.label ?? null;
}
