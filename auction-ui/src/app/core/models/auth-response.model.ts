import { AuthenticatedUser } from './authenticated-user.model';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: AuthenticatedUser;
}
