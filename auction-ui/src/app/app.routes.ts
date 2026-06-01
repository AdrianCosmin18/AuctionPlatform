import { Routes } from '@angular/router';
import { AuctionDetailsPageComponent } from './features/auction-details/auction-details.page';
import { AuctionListPageComponent } from './features/auction-list/auction-list.page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'auctions'
  },
  {
    path: 'auctions',
    component: AuctionListPageComponent
  },
  {
    path: 'auctions/:id',
    component: AuctionDetailsPageComponent
  },
  {
    path: '**',
    redirectTo: 'auctions'
  }
];
