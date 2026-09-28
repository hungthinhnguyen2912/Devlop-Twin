import { useCallback, useEffect, useState } from 'react'

type HealthState =
  | { type: 'loading' }
  | { type: 'success'; status: string }
  | { type: 'error'; message: string }

function App() {
  const [health, setHealth] = useState<HealthState>({ type: 'loading' })

  const fetchHealth = useCallback(async (signal?: AbortSignal) => {
    try {
      const response = await fetch('/api/health', { signal })

      if (!response.ok) {
        throw new Error(`Backend trả về HTTP ${response.status}`)
      }

      const data = (await response.json()) as { status?: string }

      if (!data.status) {
        throw new Error('Phản hồi không có trường status')
      }

      setHealth({ type: 'success', status: data.status })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }

      setHealth({
        type: 'error',
        message: error instanceof Error ? error.message : 'Không thể kết nối backend',
      })
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    // The callback only updates state after the network request settles.
    // oxlint-disable-next-line react/set-state-in-effect
    void fetchHealth(controller.signal)

    return () => controller.abort()
  }, [fetchHealth])

  const handleRetry = () => {
    setHealth({ type: 'loading' })
    void fetchHealth()
  }

  const isOnline = health.type === 'success'

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 px-6 py-16 text-slate-100">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(14,165,233,0.16),transparent_34%),radial-gradient(circle_at_bottom_right,rgba(99,102,241,0.14),transparent_32%)]" />

      <section className="relative w-full max-w-2xl rounded-3xl border border-white/10 bg-slate-900/80 p-8 shadow-2xl shadow-sky-950/30 backdrop-blur sm:p-12">
        <div className="mb-10 flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-2xl bg-sky-400 font-black text-slate-950">
            DT
          </div>
          <div>
            <p className="font-semibold text-white">Developer Twin</p>
            <p className="text-sm text-slate-400">Integration status</p>
          </div>
        </div>

        <p className="mb-3 text-sm font-semibold uppercase tracking-[0.22em] text-sky-400">
          System health
        </p>
        <h1 className="max-w-xl text-4xl font-bold tracking-tight text-white sm:text-5xl">
          Frontend đã sẵn sàng kết nối với backend.
        </h1>
        <p className="mt-5 max-w-lg leading-7 text-slate-400">
          Vite chuyển tiếp mọi request{' '}
          <code className="rounded bg-white/10 px-2 py-1 text-slate-200">/api</code> đến Spring Boot trên port
          8900.
        </p>

        <div className="mt-10 rounded-2xl border border-white/10 bg-slate-950/60 p-5">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
              <span
                className={`size-3 rounded-full ${
                  health.type === 'loading'
                    ? 'animate-pulse bg-amber-400'
                    : isOnline
                      ? 'bg-emerald-400 shadow-[0_0_16px_rgba(52,211,153,0.7)]'
                      : 'bg-rose-400'
                }`}
              />
              <div>
                <p className="text-sm text-slate-400">Backend API</p>
                <p className="font-semibold text-white" aria-live="polite">
                  {health.type === 'loading' && 'Đang kiểm tra...'}
                  {health.type === 'success' && `Status: ${health.status}`}
                  {health.type === 'error' && health.message}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={handleRetry}
              disabled={health.type === 'loading'}
              className="rounded-xl bg-sky-400 px-5 py-3 font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-wait disabled:opacity-60"
            >
              Kiểm tra lại
            </button>
          </div>
        </div>
      </section>
    </main>
  )
}

export default App
