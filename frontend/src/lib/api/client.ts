const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export type ApiRequestOptions = Omit<RequestInit, 'headers'> & {
  headers?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly retryAfterSeconds?: number

  constructor(
    message: string,
    status: number,
    code?: string,
    retryAfterSeconds?: number,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.retryAfterSeconds = retryAfterSeconds
  }
}

type ApiErrorPayload = {
  code?: string
  message?: string
  retryAfterSeconds?: number
}

export async function apiRequest<TResponse>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const payload = await parseErrorPayload(response)

    throw new ApiError(
      payload?.message ?? `API request failed with status ${response.status}`,
      response.status,
      payload?.code,
      payload?.retryAfterSeconds,
    )
  }

  return response.json() as Promise<TResponse>
}

async function parseErrorPayload(response: Response): Promise<ApiErrorPayload | null> {
  const contentType = response.headers.get('content-type')
  if (!contentType?.includes('application/json')) {
    return null
  }

  try {
    return (await response.json()) as ApiErrorPayload
  } catch {
    return null
  }
}
