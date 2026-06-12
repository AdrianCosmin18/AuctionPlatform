import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';

type MyActivityTab = 'auctions' | 'bids' | 'watchlist';

@Component({
  selector: 'app-my-activity-header',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonModule],
  templateUrl: './my-activity-header.component.html',
  styleUrl: './my-activity-header.component.scss'
})
export class MyActivityHeaderComponent {
  @Input({ required: true }) sectionKicker = '';
  @Input({ required: true }) title = '';
  @Input({ required: true }) subtitle = '';
  @Input({ required: true }) activeTab!: MyActivityTab;

  isActive(tab: MyActivityTab): boolean {
    return this.activeTab === tab;
  }
}
