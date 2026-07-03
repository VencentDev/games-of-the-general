import NextAuth from 'next-auth';
import GitHub from 'next-auth/providers/github';
import Google from 'next-auth/providers/google';
import type { JWT } from 'next-auth/jwt';

export const protectedHomePath = '/lobby';

const ONE_MONTH_IN_SECONDS = 30 * 24 * 60 * 60;
const TOKEN_REFRESH_BUFFER_SECONDS = 60;

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Google({
      clientId: process.env.AUTH_GOOGLE_ID!,
      clientSecret: process.env.AUTH_GOOGLE_SECRET!,
      authorization: {
        params: {
          scope: 'openid profile email',
          prompt: 'consent',
          access_type: 'offline',
          response_type: 'code',
        },
      },
    }),
    GitHub({
      clientId: process.env.AUTH_GITHUB_ID!,
      clientSecret: process.env.AUTH_GITHUB_SECRET!,
      authorization: {
        params: {
          scope: 'read:user user:email',
        },
      },
    }),
  ],
  pages: {
    signIn: '/login',
  },
  session: {
    maxAge: ONE_MONTH_IN_SECONDS,
    strategy: 'jwt',
  },
  jwt: {
    maxAge: ONE_MONTH_IN_SECONDS,
  },
  callbacks: {
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token;
        token.accessTokenExpiresAt = account.expires_at;
        token.error = undefined;
        token.provider = account.provider;
        token.refreshToken = account.refresh_token ?? token.refreshToken;
        return token;
      }

      if (!token.accessTokenExpiresAt) {
        return token;
      }

      if (Date.now() < (token.accessTokenExpiresAt - TOKEN_REFRESH_BUFFER_SECONDS) * 1000) {
        return token;
      }

      if (token.provider === 'google') {
        return refreshGoogleAccessToken(token);
      }

      return { ...token, error: 'RefreshAccessTokenError' };
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.error = token.error;
      session.provider = token.provider;
      return session;
    },
  },
});

async function refreshGoogleAccessToken(token: JWT): Promise<JWT> {
  if (!token.refreshToken) {
    return { ...token, error: 'RefreshAccessTokenError' };
  }

  try {
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: process.env.AUTH_GOOGLE_ID!,
        client_secret: process.env.AUTH_GOOGLE_SECRET!,
        grant_type: 'refresh_token',
        refresh_token: token.refreshToken,
      }),
    });

    const refreshedToken = (await response.json()) as {
      access_token?: string;
      error?: string;
      expires_in?: number;
      refresh_token?: string;
    };

    if (!response.ok || !refreshedToken.access_token || !refreshedToken.expires_in) {
      return { ...token, error: 'RefreshAccessTokenError' };
    }

    return {
      ...token,
      accessToken: refreshedToken.access_token,
      accessTokenExpiresAt: Math.floor(Date.now() / 1000 + refreshedToken.expires_in),
      error: undefined,
      refreshToken: refreshedToken.refresh_token ?? token.refreshToken,
    };
  } catch {
    return { ...token, error: 'RefreshAccessTokenError' };
  }
}
