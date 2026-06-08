import { NotificationType } from './notification-type.type';

export interface Notification {
  id: number;
  auctionId: number | null;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}
