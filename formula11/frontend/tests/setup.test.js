import { describe, expect, it } from 'vitest'

describe('configuración de tests', () => {
  it('ejecuta con Vitest y jsdom', () => {
    expect(document.body).toBeInTheDocument()
  })
})
