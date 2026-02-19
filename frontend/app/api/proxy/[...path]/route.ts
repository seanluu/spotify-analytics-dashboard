import { NextRequest } from 'next/server';

type RouteContext = {
  // in newer Next versions, dynamic route params may be provided as a Promise.
  // see build error expecting: { params: Promise<{ path: string[] }> }
  params: Promise<{
    path: string[];
  }>;
};

function getProxyTarget(): string {
  // fallback to NEXT_PUBLIC_BACKEND_URL for convenience in local/prod setups
  return (
    process.env.API_PROXY_TARGET ||
    process.env.NEXT_PUBLIC_BACKEND_URL ||
    'http://localhost:8080'
  );
}

async function proxy(request: NextRequest, { params }: RouteContext): Promise<Response> {
  const targetBase = getProxyTarget();
  const incomingUrl = new URL(request.url);

  const resolvedParams = await params;
  const pathParts = resolvedParams.path ?? [];
  const targetUrl = new URL(targetBase);
  targetUrl.pathname = `/${pathParts.join('/')}`;
  targetUrl.search = incomingUrl.search;

  const headers = new Headers(request.headers);
  // ensure fetch sets correct Host automatically
  headers.delete('host');
  // avoid passing an incorrect content-length when re-sending the body
  headers.delete('content-length');

  const method = request.method.toUpperCase();
  const body =
    method === 'GET' || method === 'HEAD' ? undefined : await request.arrayBuffer();

  const upstream = await fetch(targetUrl, {
    method,
    headers,
    body,
    redirect: 'manual',
    cache: 'no-store',
  });

  const responseHeaders = new Headers(upstream.headers);
  // avoid mismatched compression headers when streaming through Next
  responseHeaders.delete('content-encoding');

  return new Response(upstream.body, {
    status: upstream.status,
    headers: responseHeaders,
  });
}

const handler = async (request: NextRequest, ctx: RouteContext) => proxy(request, ctx);

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;
export const OPTIONS = handler;