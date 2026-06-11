import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { AuctionDetailsPageComponent } from './features/auction-details/auction-details.page';
import { AuctionListPageComponent } from './features/auction-list/auction-list.page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'auctions'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.page').then((m) => m.LoginPageComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/register/register.page').then((m) => m.RegisterPageComponent)
  },
  {
    path: 'auctions',
    component: AuctionListPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'auctions/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/auction-create/auction-create.page').then((m) => m.AuctionCreatePageComponent)
  },
  {
    path: 'dashboard',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.page').then((m) => m.DashboardPageComponent)
  },
  {
    path: 'fraud-signals',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/fraud-signals/fraud-signals.page').then((m) => m.FraudSignalsPageComponent)
  },
  {
    path: 'my-auctions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-auctions/my-auctions.page').then((m) => m.MyAuctionsPageComponent)
  },
  {
    path: 'my-bids',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-bids/my-bids.page').then((m) => m.MyBidsPageComponent)
  },
  {
    path: 'my-watchlist',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-watchlist/my-watchlist.page').then((m) => m.MyWatchlistPageComponent)
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/notifications/notifications.page').then((m) => m.NotificationsPageComponent)
  },
  {
    path: 'auctions/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/auction-edit/auction-edit.page').then((m) => m.AuctionEditPageComponent)
  },
  {
    path: 'auctions/:id',
    component: AuctionDetailsPageComponent,
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
