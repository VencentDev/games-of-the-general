import type { NextRequest } from 'next/server';

const sessionCookieNames = ['authjs.session-token', '__Secure-authjs.session-token'];

export default function middleware(req: NextRequest) {
  const pathname = req.nextUrl.pathname;
  const hasSessionCookie = sessionCookieNames.some((name) => req.cookies.has(name));

  if ((pathname.startsWith('/lobby') || pathname.startsWith('/matches')) && !hasSessionCookie) {
    const url = new URL('/login', req.url);
    url.searchParams.set('callbackUrl', req.url);
    return Response.redirect(url);
  }
}

export const config = {
  matcher: ['/lobby/:path*', '/matches/:path*'],
};
