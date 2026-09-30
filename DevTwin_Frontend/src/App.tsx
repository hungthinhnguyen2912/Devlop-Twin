import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'

type Evidence = { evidenceType: string; repoFullName: string; detail: string; url: string | null }
type Skill = { techName: string; category: string; repoCount: number; firstUsed: string | null; lastUsed: string | null; evidence: Evidence[] }
type Repository = { name: string; fullName: string; description: string | null; primaryLanguage: string | null; stars: number; pushedAt: string | null; technologies: Array<{ name: string; category: string }> }
type Profile = { login?: string; name?: string; avatarUrl?: string; htmlUrl?: string; bio?: string; location?: string; company?: string; blog?: string; followers?: number; following?: number; publicRepos?: number }
type DeveloperTwin = { platform: string; username: string; displayName: string; profile: Profile; aiSummary: string | null; builtAt: string; skills: Skill[]; topRepositories: Repository[] }
type RequestState = { type: 'idle' } | { type: 'loading'; phase: number } | { type: 'success'; twin: DeveloperTwin } | { type: 'error'; message: string }

const phases = [
  ['Syncing public GitHub data', 'Profile, repositories and activity'],
  ['Normalizing repositories', 'Languages, dependencies and containers'],
  ['Building evidence graph', 'Skills, timeline and source links'],
  ['Writing bilingual summary', 'Vietnamese and English'],
]

const categoryStyles: Record<string, string> = {
  language: 'border-sky-400/25 bg-sky-400/10 text-sky-200',
  framework: 'border-violet-400/25 bg-violet-400/10 text-violet-200',
  database: 'border-emerald-400/25 bg-emerald-400/10 text-emerald-200',
  platform: 'border-amber-400/25 bg-amber-400/10 text-amber-200',
  tool: 'border-slate-400/25 bg-slate-400/10 text-slate-200',
}

function Icon({ name, className = 'size-5' }: { name: string; className?: string }) {
  const paths: Record<string, ReactNode> = {
    github: <path d="M12 2a10 10 0 0 0-3.16 19.49c.5.09.68-.22.68-.48v-1.87c-2.78.6-3.37-1.18-3.37-1.18-.45-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.61.07-.61 1 .07 1.53 1.03 1.53 1.03.9 1.53 2.35 1.09 2.92.83.09-.65.35-1.09.64-1.34-2.22-.25-4.56-1.11-4.56-4.94 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.64 0 0 .84-.27 2.75 1.02A9.6 9.6 0 0 1 12 6.82a9.6 9.6 0 0 1 2.5.34c1.91-1.3 2.75-1.02 2.75-1.02.55 1.37.2 2.39.1 2.64.64.7 1.03 1.59 1.03 2.68 0 3.84-2.34 4.68-4.57 4.93.36.31.68.92.68 1.86v2.76c0 .27.18.58.69.48A10 10 0 0 0 12 2Z" />,
    arrow: <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>,
    spark: <><path d="m12 3-1.5 4.5L6 9l4.5 1.5L12 15l1.5-4.5L18 9l-4.5-1.5L12 3Z" /><path d="m5 15-.75 2.25L2 18l2.25.75L5 21l.75-2.25L8 18l-2.25-.75L5 15Z" /></>,
    code: <><path d="m8 9-3 3 3 3" /><path d="m16 9 3 3-3 3" /><path d="m14 5-4 14" /></>,
    location: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></>,
    star: <path d="m12 2.5 2.94 5.96 6.58.96-4.76 4.64 1.12 6.55L12 17.52l-5.88 3.09 1.12-6.55-4.76-4.64 6.58-.96L12 2.5Z" />,
    external: <><path d="M15 3h6v6" /><path d="M10 14 21 3" /><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></>,
    refresh: <><path d="M20 11a8.1 8.1 0 0 0-15.5-2M4 4v5h5" /><path d="M4 13a8.1 8.1 0 0 0 15.5 2M20 20v-5h-5" /></>,
    check: <path d="m5 12 4 4L19 6" />,
  }
  return <svg className={className} viewBox="0 0 24 24" fill={name === 'github' ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en', { month: 'short', year: 'numeric' }).format(new Date(value))
}

function splitSummary(summary: string | null) {
  if (!summary) return null
  const vietnamese = summary.match(/##\s*Tiếng Việt\s*([\s\S]*?)(?=##\s*English|$)/i)?.[1]?.trim()
  const english = summary.match(/##\s*English\s*([\s\S]*)/i)?.[1]?.trim()
  return { vietnamese: vietnamese || summary, english: english || '' }
}

function App() {
  const [username, setUsername] = useState('')
  const [request, setRequest] = useState<RequestState>({ type: 'idle' })
  const [backendOnline, setBackendOnline] = useState<boolean | null>(null)
  const [selectedSkill, setSelectedSkill] = useState<string | null>(null)

  const checkHealth = useCallback(async () => {
    try { setBackendOnline((await fetch('/api/health')).ok) } catch { setBackendOnline(false) }
  }, [])

  useEffect(() => {
    // The state update happens only after the health request settles.
    // oxlint-disable-next-line react/set-state-in-effect
    void checkHealth()
  }, [checkHealth])
  useEffect(() => {
    if (request.type !== 'loading') return
    const timer = window.setInterval(() => setRequest((current) => current.type === 'loading' ? { type: 'loading', phase: Math.min(current.phase + 1, phases.length - 1) } : current), 2600)
    return () => window.clearInterval(timer)
  }, [request.type])

  const twin = request.type === 'success' ? request.twin : null
  const summary = useMemo(() => splitSummary(twin?.aiSummary ?? null), [twin?.aiSummary])
  const activeSkill = twin?.skills.find((skill) => skill.techName === selectedSkill) ?? null

  const runBuild = async (requestedUsername: string) => {
    const normalized = requestedUsername.trim().replace(/^@/, '')
    if (!normalized) return
    setUsername(normalized)
    setRequest({ type: 'loading', phase: 0 })
    setSelectedSkill(null)
    try {
      const response = await fetch(`/api/twin/${encodeURIComponent(normalized)}/build`, { method: 'POST' })
      const body = await response.json()
      if (!response.ok) throw new Error(body.message || `Backend returned HTTP ${response.status}`)
      setRequest({ type: 'success', twin: body as DeveloperTwin })
    } catch (error) {
      setRequest({ type: 'error', message: error instanceof Error ? error.message : 'Could not build this Developer Twin.' })
    }
  }

  const submit = (event: FormEvent) => { event.preventDefault(); void runBuild(username) }

  return (
    <main className="min-h-screen bg-[#07110f] text-stone-100 selection:bg-emerald-300 selection:text-emerald-950">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_12%_5%,rgba(52,211,153,0.10),transparent_28%),radial-gradient(circle_at_88%_20%,rgba(251,191,36,0.07),transparent_25%)]" />
      <header className="relative z-10 border-b border-white/8 bg-[#07110f]/85 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4 sm:px-8">
          <button type="button" className="flex items-center gap-3 text-left" onClick={() => setRequest({ type: 'idle' })}>
            <span className="grid size-10 place-items-center rounded-xl border border-emerald-300/20 bg-emerald-300 text-sm font-black text-emerald-950 shadow-[0_0_30px_rgba(52,211,153,0.12)]">DT</span>
            <span><span className="block font-semibold tracking-tight">Developer Twin</span><span className="block text-[11px] uppercase tracking-[0.22em] text-stone-500">Evidence, not guesses</span></span>
          </button>
          <div className="flex items-center gap-3 text-xs text-stone-400">
            <span className={`size-2 rounded-full ${backendOnline === null ? 'animate-pulse bg-amber-300' : backendOnline ? 'bg-emerald-300 shadow-[0_0_12px_rgba(110,231,183,.7)]' : 'bg-rose-400'}`} />
            <span className="hidden sm:inline">API {backendOnline ? 'connected' : backendOnline === false ? 'offline' : 'checking'}</span>
            <a href="https://github.com/hungthinhnguyen2912/Devlop-Twin" target="_blank" rel="noreferrer" className="ml-2 rounded-lg border border-white/10 p-2 text-stone-300 transition hover:border-emerald-300/30 hover:text-emerald-200" aria-label="Open GitHub repository"><Icon name="github" className="size-4" /></a>
          </div>
        </div>
      </header>

      <div className="relative z-0 mx-auto max-w-7xl px-5 py-10 sm:px-8 lg:py-14">
        {!twin && request.type !== 'loading' && (
          <section className="grid min-h-[calc(100vh-12rem)] items-center gap-14 lg:grid-cols-[1.08fr_.92fr]">
            <div>
              <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-emerald-300/15 bg-emerald-300/5 px-4 py-2 text-xs font-medium uppercase tracking-[0.18em] text-emerald-200"><Icon name="spark" className="size-4" />Public GitHub intelligence</div>
              <h1 className="max-w-3xl text-5xl font-semibold leading-[1.02] tracking-[-0.045em] text-stone-50 sm:text-6xl lg:text-7xl">Turn public work into a profile that can <span className="font-serif italic text-emerald-300">prove itself.</span></h1>
              <p className="mt-7 max-w-xl text-base leading-7 text-stone-400 sm:text-lg">Developer Twin detects technologies, builds a timeline, and connects every skill to inspectable evidence—then summarizes it in Vietnamese and English.</p>
              <form onSubmit={submit} className="mt-10 max-w-2xl rounded-2xl border border-white/10 bg-white/[0.035] p-2 shadow-2xl shadow-black/20 backdrop-blur">
                <div className="flex flex-col gap-2 sm:flex-row">
                  <label className="flex min-w-0 flex-1 items-center gap-3 px-4 py-3"><Icon name="github" className="size-5 shrink-0 text-stone-500" /><span className="sr-only">GitHub username</span><input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="GitHub username" autoComplete="off" className="min-w-0 flex-1 bg-transparent text-base text-white outline-none placeholder:text-stone-600" /></label>
                  <button type="submit" disabled={!username.trim()} className="group flex items-center justify-center gap-3 rounded-xl bg-emerald-300 px-6 py-3.5 font-semibold text-emerald-950 transition hover:bg-emerald-200 disabled:cursor-not-allowed disabled:opacity-40">Build my Twin<Icon name="arrow" className="size-4 transition group-hover:translate-x-1" /></button>
                </div>
              </form>
              {request.type === 'error' && <div className="mt-4 max-w-2xl rounded-xl border border-rose-400/20 bg-rose-400/8 px-4 py-3 text-sm text-rose-200">{request.message}</div>}
              <p className="mt-4 flex items-center gap-2 text-xs text-stone-600"><span className="size-1.5 rounded-full bg-stone-600" />Only public data. No repository write access.</p>
            </div>
            <div className="relative hidden lg:block">
              <div className="absolute -inset-8 rounded-full bg-emerald-300/5 blur-3xl" />
              <div className="relative rotate-2 rounded-3xl border border-white/10 bg-[#0c1815] p-5 shadow-2xl shadow-black/30">
                <div className="flex items-center justify-between border-b border-white/8 pb-4"><div className="flex items-center gap-3"><div className="size-10 rounded-full bg-gradient-to-br from-emerald-200 to-emerald-600" /><div><div className="h-2.5 w-28 rounded bg-white/20" /><div className="mt-2 h-2 w-20 rounded bg-white/8" /></div></div><div className="rounded-full border border-emerald-300/20 px-3 py-1 text-[10px] uppercase tracking-widest text-emerald-300">Verified</div></div>
                <div className="mt-6 grid grid-cols-3 gap-3">{['Java', 'Spring Boot', 'PostgreSQL', 'React', 'Docker', 'TypeScript'].map((tech, index) => <div key={tech} className={`rounded-xl border p-3 ${index < 2 ? 'border-emerald-300/20 bg-emerald-300/8' : 'border-white/8 bg-white/[0.025]'}`}><div className="mb-4 text-xs text-stone-300">{tech}</div><div className="h-1 rounded-full bg-white/8"><div className="h-full rounded-full bg-emerald-300" style={{ width: `${82 - index * 7}%` }} /></div></div>)}</div>
                <div className="mt-4 rounded-xl border border-white/8 bg-black/10 p-4 font-mono text-[11px] leading-6 text-stone-500"><span className="text-emerald-300">evidence</span>.source = <span className="text-amber-200">&quot;pom.xml&quot;</span><br /><span className="text-emerald-300">timeline</span>.firstUsed = <span className="text-sky-200">&quot;2024-03&quot;</span></div>
              </div>
            </div>
          </section>
        )}

        {request.type === 'loading' && (
          <section className="mx-auto flex min-h-[calc(100vh-12rem)] max-w-2xl flex-col justify-center">
            <div className="mb-10 flex items-end justify-between"><div><p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-300">Building @{username.replace(/^@/, '')}</p><h1 className="mt-3 text-4xl font-semibold tracking-tight">Following the evidence trail.</h1></div><span className="font-mono text-sm text-stone-500">0{request.phase + 1} / 04</span></div>
            <div className="space-y-3">{phases.map(([title, detail], index) => { const done = index < request.phase; const active = index === request.phase; return <div key={title} className={`flex items-center gap-4 rounded-2xl border p-5 transition-all ${active ? 'border-emerald-300/25 bg-emerald-300/8' : 'border-white/7 bg-white/[0.02]'} ${index > request.phase ? 'opacity-35' : ''}`}><span className={`grid size-10 shrink-0 place-items-center rounded-full border ${done ? 'border-emerald-300 bg-emerald-300 text-emerald-950' : active ? 'animate-pulse border-emerald-300/40 text-emerald-300' : 'border-white/10 text-stone-600'}`}>{done ? <Icon name="check" className="size-5" /> : <span className="font-mono text-xs">0{index + 1}</span>}</span><div><p className="font-medium text-stone-100">{title}</p><p className="mt-1 text-sm text-stone-500">{detail}</p></div></div> })}</div>
            <p className="mt-7 text-center text-xs text-stone-600">Large public profiles may take a minute. Keep this tab open.</p>
          </section>
        )}

        {twin && (
          <section className="pb-20">
            <div className="mb-8 flex flex-wrap items-center justify-between gap-4"><button type="button" onClick={() => setRequest({ type: 'idle' })} className="flex items-center gap-2 text-sm text-stone-500 transition hover:text-stone-200"><span className="rotate-180"><Icon name="arrow" className="size-4" /></span>Build another Twin</button><button type="button" onClick={() => void runBuild(twin.username)} className="flex items-center gap-2 rounded-lg border border-white/10 px-4 py-2 text-xs font-medium text-stone-300 transition hover:border-emerald-300/25 hover:text-emerald-200"><Icon name="refresh" className="size-3.5" />Rebuild data</button></div>
            <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
              <aside className="space-y-6">
                <div className="rounded-3xl border border-white/9 bg-white/[0.035] p-6">
                  <div className="flex items-center gap-4 lg:block">{twin.profile.avatarUrl ? <img src={twin.profile.avatarUrl} alt="" className="size-20 rounded-2xl object-cover ring-1 ring-white/10 lg:size-24" /> : <div className="grid size-20 place-items-center rounded-2xl bg-emerald-300 text-2xl font-black text-emerald-950">{twin.displayName.slice(0, 2).toUpperCase()}</div>}<div className="min-w-0 lg:mt-5"><h1 className="truncate text-2xl font-semibold tracking-tight">{twin.displayName}</h1><p className="mt-1 text-sm text-emerald-300">@{twin.username}</p></div></div>
                  {twin.profile.bio && <p className="mt-5 text-sm leading-6 text-stone-400">{twin.profile.bio}</p>}
                  <div className="mt-6 space-y-3 border-t border-white/8 pt-5 text-sm text-stone-400">{twin.profile.location && <p className="flex items-center gap-3"><Icon name="location" className="size-4 text-stone-600" />{twin.profile.location}</p>}<p className="flex items-center gap-3"><Icon name="users" className="size-4 text-stone-600" />{twin.profile.followers ?? 0} followers</p><p className="flex items-center gap-3"><Icon name="code" className="size-4 text-stone-600" />{twin.profile.publicRepos ?? twin.topRepositories.length} public repos</p></div>
                  {twin.profile.htmlUrl && <a href={twin.profile.htmlUrl} target="_blank" rel="noreferrer" className="mt-6 flex items-center justify-center gap-2 rounded-xl border border-white/10 py-3 text-sm font-medium transition hover:border-emerald-300/25 hover:text-emerald-200">View GitHub <Icon name="external" className="size-3.5" /></a>}
                </div>
                <div className="rounded-3xl border border-white/9 bg-white/[0.025] p-6"><p className="text-xs uppercase tracking-[0.18em] text-stone-500">Twin snapshot</p><div className="mt-5 grid grid-cols-2 gap-4"><div><p className="text-2xl font-semibold text-emerald-300">{twin.skills.length}</p><p className="mt-1 text-xs text-stone-500">skills found</p></div><div><p className="text-2xl font-semibold">{twin.skills.reduce((sum, skill) => sum + skill.evidence.length, 0)}</p><p className="mt-1 text-xs text-stone-500">evidence points</p></div></div><p className="mt-5 border-t border-white/8 pt-4 text-xs text-stone-600">Built {new Date(twin.builtAt).toLocaleString()}</p></div>
              </aside>

              <div className="min-w-0 space-y-6">
                <div className="rounded-3xl border border-emerald-300/12 bg-gradient-to-br from-emerald-300/[0.07] to-transparent p-6 sm:p-8">
                  <div className="flex items-center justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-300">AI developer summary</p><h2 className="mt-2 text-2xl font-semibold tracking-tight">One profile, two languages.</h2></div><div className="grid size-11 shrink-0 place-items-center rounded-xl border border-emerald-300/15 bg-emerald-300/10 text-emerald-300"><Icon name="spark" /></div></div>
                  {summary ? <div className="mt-7 grid gap-7 md:grid-cols-2 md:gap-10"><div><span className="text-[11px] font-semibold uppercase tracking-[0.18em] text-stone-500">Tiếng Việt</span><p className="mt-3 whitespace-pre-line text-sm leading-7 text-stone-300">{summary.vietnamese}</p></div><div className="border-t border-white/8 pt-7 md:border-l md:border-t-0 md:pl-10 md:pt-0"><span className="text-[11px] font-semibold uppercase tracking-[0.18em] text-stone-500">English</span><p className="mt-3 whitespace-pre-line text-sm leading-7 text-stone-300">{summary.english}</p></div></div> : <div className="mt-6 rounded-xl border border-amber-300/15 bg-amber-300/5 px-4 py-3 text-sm text-amber-100/70">Twin data is ready. Add <code className="text-amber-200">GOOGLE_API_KEY</code> and rebuild to generate the bilingual summary.</div>}
                </div>

                <div className="rounded-3xl border border-white/9 bg-white/[0.025] p-6 sm:p-8">
                  <div className="flex items-end justify-between gap-4"><div><p className="text-xs uppercase tracking-[0.18em] text-stone-500">Evidence-backed capabilities</p><h2 className="mt-2 text-2xl font-semibold tracking-tight">Skill map</h2></div><p className="text-xs text-stone-600">Select a skill for evidence</p></div>
                  <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{twin.skills.map((skill) => <button key={skill.techName} type="button" onClick={() => setSelectedSkill(skill.techName === selectedSkill ? null : skill.techName)} className={`rounded-2xl border p-4 text-left transition ${skill.techName === selectedSkill ? 'border-emerald-300/35 bg-emerald-300/8' : 'border-white/8 bg-black/10 hover:border-white/15'}`}><div className="flex items-start justify-between gap-3"><span className="font-medium text-stone-100">{skill.techName}</span><span className={`rounded-full border px-2 py-1 text-[9px] uppercase tracking-wider ${categoryStyles[skill.category] ?? categoryStyles.tool}`}>{skill.category}</span></div><div className="mt-5 flex items-end justify-between"><span className="text-xs text-stone-500">{skill.repoCount} {skill.repoCount === 1 ? 'repository' : 'repositories'}</span><span className="font-mono text-[10px] text-stone-600">{formatDate(skill.firstUsed)} → {formatDate(skill.lastUsed)}</span></div></button>)}</div>
                  {activeSkill && <div className="mt-5 overflow-hidden rounded-2xl border border-emerald-300/15 bg-[#07110f]"><div className="flex items-center justify-between border-b border-white/8 px-5 py-4"><div><span className="font-medium text-emerald-200">{activeSkill.techName}</span><span className="ml-2 text-xs text-stone-600">{activeSkill.evidence.length} evidence points</span></div><button type="button" onClick={() => setSelectedSkill(null)} className="text-xs text-stone-500 hover:text-white">Close</button></div><div className="divide-y divide-white/6">{activeSkill.evidence.map((item, index) => <div key={`${item.repoFullName}-${item.evidenceType}-${index}`} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"><div className="min-w-0"><div className="flex items-center gap-2"><span className="rounded bg-white/6 px-2 py-1 text-[10px] uppercase tracking-wider text-stone-500">{item.evidenceType}</span><span className="truncate text-sm text-stone-300">{item.repoFullName}</span></div><p className="mt-2 truncate font-mono text-xs text-stone-600">{item.detail}</p></div>{item.url && <a href={item.url} target="_blank" rel="noreferrer" className="flex shrink-0 items-center gap-2 text-xs text-emerald-300 hover:text-emerald-200">Inspect source <Icon name="external" className="size-3" /></a>}</div>)}</div></div>}
                </div>

                <div className="rounded-3xl border border-white/9 bg-white/[0.025] p-6 sm:p-8">
                  <p className="text-xs uppercase tracking-[0.18em] text-stone-500">Repository signal</p><h2 className="mt-2 text-2xl font-semibold tracking-tight">Top public work</h2>
                  <div className="mt-6 divide-y divide-white/7">{twin.topRepositories.map((repository) => <a key={repository.fullName} href={`https://github.com/${repository.fullName}`} target="_blank" rel="noreferrer" className="group grid gap-3 py-5 first:pt-0 last:pb-0 sm:grid-cols-[1fr_auto] sm:items-center"><div className="min-w-0"><div className="flex items-center gap-2"><h3 className="truncate font-medium text-stone-200 transition group-hover:text-emerald-200">{repository.name}</h3><Icon name="external" className="size-3 text-stone-700" /></div><p className="mt-1 truncate text-sm text-stone-600">{repository.description || 'Public repository with detected technical evidence.'}</p><div className="mt-3 flex flex-wrap gap-2">{repository.technologies.slice(0, 5).map((tech) => <span key={`${repository.name}-${tech.name}`} className="rounded-md bg-white/5 px-2 py-1 text-[10px] text-stone-500">{tech.name}</span>)}</div></div><div className="flex items-center gap-4 text-xs text-stone-600"><span className="flex items-center gap-1"><Icon name="star" className="size-3" />{repository.stars}</span><span>{formatDate(repository.pushedAt)}</span></div></a>)}</div>
                </div>
              </div>
            </div>
          </section>
        )}
      </div>
    </main>
  )
}

export default App
