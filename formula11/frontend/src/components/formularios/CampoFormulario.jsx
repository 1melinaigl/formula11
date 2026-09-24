export default function CampoFormulario({ id, label, error, ...props }) {
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input aria-invalid={Boolean(error)} className={error ? 'invalid' : ''} id={id} {...props} />
      <div className="field-error" role={error ? 'alert' : undefined}>{error || ''}</div>
    </div>
  )
}
