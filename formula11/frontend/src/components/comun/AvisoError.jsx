export default function AvisoError({ error, className = '' }) {
  if (!error) return null

  return (
    <div className={`alert show ${className}`.trim()} role="alert">
      <span>{error.api?.mensaje || error.message}</span>
      {error.api?.correlationId && <code></code>}
    </div>
  )
}
