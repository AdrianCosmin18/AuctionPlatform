import { UserRole } from './user-role.type';

export interface AuthenticatedUser {
  id: number;
  email: string;
  role: UserRole;
}
