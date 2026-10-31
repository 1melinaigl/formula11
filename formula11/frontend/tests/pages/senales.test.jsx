import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const rebbach = { id: 1467, nombre: 'Abde Rebbach', equipo: 'Deportivo Alavés', liga: 'La Liga', posicion: 'DEL' }
const koski = { id: 1450, nombre: 'Ville Koski', equipo: 'Deportivo Alavés', liga: 'La Liga', posicion: 'DEF' }

// Senal del contrato: brecha en porcentaje y fecha date del domingo de la semana.
const entreEstrategias = { id: 269, jugador: rebbach, fecha: '2026-10-25', tipo: 'ENTRE_ESTRATEGIAS', brecha: 252.6, precio: 0.6791, precioEstimado: 2.3945 }
const precioAtrasado = { id: 270, jugador: koski, fecha: '2026-10-25', tipo: 'PRECIO_ATRASADO', brecha: 18, precio: 0.9278, precioEstimado: 1.0948 }
const desconocida = { id: 271, jugador: koski, fecha: '2026-10-25', tipo: 'OTRO', brecha: 10, precio: 1, precioEstimado: 1.1 }
const vieja = { id: 12, jugador: rebbach, fecha: '2026-08-23', tipo: 'PRECIO_ATRASADO', brecha: 30, precio: 0.5, precioEstimado: 0.65 }

const USUARIO = { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' }
const SUPERUSUARIO = { id: 1, nombre: 'Super', rol: 'SUPERUSUARIO' }

function renderSenales(ruta = '/senales', usuario = USUARIO) {
  window.history.pushState({}, '', ruta)
  if (usuario) window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'senales-token', usuario }))
  return render(<SesionProvider><App /></SesionProvider>)
}

function pagina(content, page = 0, totalPages = content.length ? 1 : 0, totalElements = content.length) {
  return { content, page, size: 20, totalElements, totalPages }
}

// La alerta (size=100) y la lista (size=20) usan la misma ruta; acá solo se registran los pedidos de la lista.
function usarLista(responder) {
  const pedidos = []
  server.use(http.get('/api/senales', async ({ request }) => {
    const url = new URL(request.url)
    if (url.searchParams.get('size') === '100') return HttpResponse.json(pagina([]))
    pedidos.push(url)
    return responder(url, pedidos.length)
  }))
  return pedidos
}

function error(status, mensaje, correlationId) {
  return HttpResponse.json({ mensaje, timestamp: '2026-10-31T12:00:00Z', correlationId }, { status })
}

describe('pantalla de señales', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('el menú muestra "Señales" con sesión, también al superusuario, y no sin sesión', async () => {
    const user = userEvent.setup()
    usarLista(() => HttpResponse.json(pagina([])))
    const { unmount } = renderSenales('/', USUARIO)
    await user.click(screen.getByRole('button', { name: /Señales.*Jugadores subvaluados/ }))
    expect(window.location.pathname).toBe('/senales')
    expect(await screen.findByRole('heading', { name: 'Señales' })).toBeInTheDocument()
    unmount()
    window.sessionStorage.clear()

    const superusuario = renderSenales('/', SUPERUSUARIO)
    expect(screen.getByRole('button', { name: /Señales/ })).toBeInTheDocument()
    superusuario.unmount()
    window.sessionStorage.clear()

    renderSenales('/', null)
    expect(screen.queryByRole('button', { name: /Señales/ })).not.toBeInTheDocument()
  })

  it('sin sesión muestra el acceso denegado y no consulta la API', async () => {
    const pedidos = usarLista(() => HttpResponse.json(pagina([entreEstrategias])))
    renderSenales('/senales', null)

    expect(await screen.findByRole('heading', { name: 'Acceso denegado' })).toBeInTheDocument()
    expect(pedidos).toHaveLength(0)
  })

  it('un 401 en la lista limpia la sesión y muestra el acceso denegado sin exponer el token', async () => {
    usarLista(() => error(401, 'La sesión venció.', 'corr-401'))
    renderSenales()

    expect(await screen.findByRole('heading', { name: 'Acceso denegado' })).toBeInTheDocument()
    expect(screen.getByText('La sesión venció.')).toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toBeNull()
    expect(document.body).not.toHaveTextContent('senales-token')
  })

  it('lista las señales vigentes con su formato y descarta tipos desconocidos', async () => {
    const pedidos = usarLista(() => HttpResponse.json(pagina([entreEstrategias, desconocida, precioAtrasado])))
    renderSenales()

    const tabla = await screen.findByRole('table', { name: 'Señales de jugadores subvaluados' })
    const url = pedidos[0]
    expect(url.searchParams.get('vigentes')).toBe('true')
    expect(url.searchParams.get('page')).toBe('0')
    expect(url.searchParams.get('size')).toBe('20')
    expect(url.searchParams.has('sort')).toBe(false)

    const [, filaRebbach, filaKoski, sobrante] = within(tabla).getAllByRole('row')
    expect(sobrante).toBeUndefined()
    expect(within(filaRebbach).getByRole('link', { name: 'Abde Rebbach' })).toHaveAttribute('href', '/jugadores/1467')
    expect(within(filaRebbach).getByText('Deportivo Alavés')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('La Liga')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('DEL')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('Entre estrategias')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('Otra estrategia lo valora más')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('+252,60%')).toHaveClass('up')
    expect(within(filaRebbach).getByText('0,68')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('2,39')).toBeInTheDocument()
    expect(within(filaRebbach).getByText('al 25/10/2026')).toBeInTheDocument()
    expect(within(filaKoski).getByText('Precio atrasado')).toBeInTheDocument()
    expect(within(filaKoski).getByText('+18,00%')).toBeInTheDocument()
  })

  it('sin señales vigentes muestra el estado vacío', async () => {
    usarLista(() => HttpResponse.json(pagina([])))
    renderSenales()

    expect(await screen.findByRole('heading', { name: 'No hay señales esta semana' })).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Señales de jugadores subvaluados' })).not.toBeInTheDocument()
  })

  it('alterna entre vigentes e historial y pagina conservando la vista', async () => {
    const user = userEvent.setup()
    const pedidos = usarLista((url) => {
      if (url.searchParams.get('vigentes') === 'true') return HttpResponse.json(pagina([entreEstrategias]))
      const page = Number(url.searchParams.get('page'))
      return HttpResponse.json(pagina(page === 0 ? [entreEstrategias, precioAtrasado] : [vieja], page, 2, 21))
    })
    renderSenales()

    const grupo = await screen.findByRole('group', { name: 'Ver señales' })
    await screen.findByRole('table', { name: 'Señales de jugadores subvaluados' })
    expect(within(grupo).getByRole('button', { name: 'Vigentes' })).toHaveAttribute('aria-pressed', 'true')

    await user.click(within(grupo).getByRole('button', { name: 'Vigentes' }))
    expect(pedidos).toHaveLength(1)

    await user.click(within(grupo).getByRole('button', { name: 'Historial' }))
    expect(within(grupo).getByRole('button', { name: 'Historial' })).toHaveAttribute('aria-pressed', 'true')
    const paginacion = await screen.findByRole('navigation', { name: 'Paginación de señales' })
    expect(pedidos[1].searchParams.get('vigentes')).toBe('false')
    expect(pedidos[1].searchParams.get('page')).toBe('0')
    expect(within(paginacion).getByText('Página 1 de 2')).toBeInTheDocument()

    await user.click(within(paginacion).getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByText('al 23/08/2026')).toBeInTheDocument()
    expect(pedidos[2].searchParams.get('vigentes')).toBe('false')
    expect(pedidos[2].searchParams.get('page')).toBe('1')
  })

  it('una página fuera de rango vuelve una sola vez a la última válida', async () => {
    const user = userEvent.setup()
    // Historial: primero 3 páginas; al pedir la 2.ª el total bajó a una sola página (fuera de rango).
    const pedidos = usarLista((url, numero) => {
      if (url.searchParams.get('vigentes') === 'true') return HttpResponse.json(pagina([entreEstrategias], 0, 1, 1))
      if (numero === 2) return HttpResponse.json(pagina([entreEstrategias], 0, 3, 41))
      if (numero === 3) return HttpResponse.json(pagina([], 1, 1, 20))
      return HttpResponse.json(pagina([vieja], 0, 1, 20))
    })
    renderSenales()

    await screen.findByRole('table', { name: 'Señales de jugadores subvaluados' })
    await user.click(screen.getByRole('button', { name: 'Historial' }))
    await user.click(within(await screen.findByRole('navigation', { name: 'Paginación de señales' })).getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByText('al 23/08/2026')).toBeInTheDocument()
    expect(pedidos.map((url) => url.searchParams.get('page'))).toEqual(['0', '0', '1', '0'])
  })

  it('una respuesta vieja no reemplaza a la vigente', async () => {
    const user = userEvent.setup()
    let liberarVigentes
    usarLista((url) => {
      if (url.searchParams.get('vigentes') === 'true') {
        return new Promise((resolve) => { liberarVigentes = () => resolve(HttpResponse.json(pagina([entreEstrategias]))) })
      }
      return HttpResponse.json(pagina([vieja]))
    })
    renderSenales()

    await user.click(await screen.findByRole('button', { name: 'Historial' }))
    expect(await screen.findByText('al 23/08/2026')).toBeInTheDocument()
    liberarVigentes()
    // Se da tiempo a que la respuesta vieja llegue; no tiene que pisar el historial.
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(screen.queryByText('al 25/10/2026')).not.toBeInTheDocument()
    expect(screen.getByText('al 23/08/2026')).toBeInTheDocument()
  })

  it('compra 1 token en un click y muestra la confirmación con el link al historial', async () => {
    const user = userEvent.setup()
    usarLista(() => HttpResponse.json(pagina([entreEstrategias, precioAtrasado])))
    let cuerpo
    let liberar
    server.use(http.post('/api/ordenes/compra', async ({ request }) => {
      cuerpo = await request.json()
      await new Promise((resolve) => { liberar = resolve })
      return HttpResponse.json({
        id: 9, tipo: 'COMPRA', jugador: rebbach, cantidad: 1, precioUnitario: 0.68, total: 0.68, saldoPosterior: 49.32,
        contraparte: 'superusuario', fecha: '2026-10-31T12:00:00Z', estrategia: 'RENDIMIENTO_POSICION', version: 1,
      }, { status: 201 })
    }))
    renderSenales()

    const boton = await screen.findByRole('button', { name: 'Comprar 1 token de Abde Rebbach' })
    await user.click(boton)
    expect(boton).toBeDisabled()
    expect(boton).toHaveTextContent('Comprando...')
    expect(screen.getByRole('button', { name: 'Comprar 1 token de Ville Koski' })).toBeDisabled()
    await waitFor(() => expect(cuerpo).toEqual({ jugadorId: 1467, cantidad: 1 }))
    liberar()

    const confirmacion = await screen.findByText(/Compraste 1 token a 0,68 créditos\. Total: 0,68 créditos\. Saldo: 49,32 créditos\./)
    expect(within(confirmacion).getByRole('link', { name: 'Ver historial' })).toHaveAttribute('href', '/operaciones')
    expect(boton).toBeEnabled()
  })

  it('muestra el error de saldo insuficiente o de tokens agotados con su correlationId', async () => {
    const user = userEvent.setup()
    usarLista(() => HttpResponse.json(pagina([entreEstrategias, precioAtrasado])))
    let intento = 0
    server.use(http.post('/api/ordenes/compra', () => {
      intento += 1
      return intento === 1 ? error(422, 'Saldo insuficiente.', 'corr-422') : error(409, 'No quedan tokens disponibles.', 'corr-409')
    }))
    renderSenales()

    await user.click(await screen.findByRole('button', { name: 'Comprar 1 token de Abde Rebbach' }))
    const alerta422 = await screen.findByRole('alert')
    expect(alerta422).toHaveTextContent('Saldo insuficiente.')
    expect(alerta422).toHaveTextContent('correlationId: corr-422')

    await user.click(screen.getByRole('button', { name: 'Comprar 1 token de Ville Koski' }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('No quedan tokens disponibles.'))
    expect(screen.getByRole('alert')).toHaveTextContent('correlationId: corr-409')
    expect(screen.getByRole('button', { name: 'Comprar 1 token de Ville Koski' })).toBeEnabled()
  })

  it('el superusuario ve las señales pero no puede comprar', async () => {
    usarLista(() => HttpResponse.json(pagina([entreEstrategias])))
    renderSenales('/senales', SUPERUSUARIO)

    expect(await screen.findByRole('link', { name: 'Abde Rebbach' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Comprar 1 token/ })).not.toBeInTheDocument()
  })

  it('el panel muestra la tasa, la diferencia y los aciertos por semana', async () => {
    usarLista(() => HttpResponse.json(pagina([entreEstrategias])))
    let consultas = 0
    server.use(http.get('/api/senales/aciertos', () => {
      consultas += 1
      return HttpResponse.json({
        semanas: [
          { fecha: '2026-08-16', senales: 6, variacionSenalados: -1.43, variacionMercado: -0.06, aciertos: 4 },
          { fecha: '2026-08-23', senales: 23, variacionSenalados: 0.32, variacionMercado: -0.1, aciertos: 17 },
          { fecha: '2026-09-20', senales: 40, variacionSenalados: 0, variacionMercado: 0, aciertos: 0 },
        ],
        tasaAcierto: 50,
        diferenciaPromedio: -0.12,
      })
    }))
    renderSenales()

    const resumen = await screen.findByRole('list', { name: 'Resumen de aciertos' })
    expect(within(resumen).getByText('50,00%')).toBeInTheDocument()
    expect(within(resumen).getByText('-0,12%')).toHaveClass('down')

    const tabla = screen.getByRole('table', { name: 'Aciertos por semana' })
    const [, primera, segunda, tercera] = within(tabla).getAllByRole('row')
    expect(within(primera).getByText('16/08/2026')).toBeInTheDocument()
    expect(within(primera).getByText('-1,43%')).toHaveClass('down')
    expect(within(primera).getByText('4 de 6')).toBeInTheDocument()
    expect(within(segunda).getByText('+0,32%')).toHaveClass('up')
    expect(within(segunda).getByText('-0,10%')).toHaveClass('down')
    expect(within(segunda).getByText('17 de 23')).toBeInTheDocument()
    expect(within(tercera).getAllByText('0,00%')[0]).toHaveClass('flat')
    expect(consultas).toBe(1)
  })

  it('sin semanas medidas el panel muestra el estado vacío y no los indicadores en 0', async () => {
    usarLista(() => HttpResponse.json(pagina([entreEstrategias])))
    renderSenales()

    expect(await screen.findByText('Todavía no hay semanas para medir: las señales se evalúan una semana después de generarse.')).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Resumen de aciertos' })).not.toBeInTheDocument()
    expect(screen.queryByText('Tasa de acierto')).not.toBeInTheDocument()
  })

  it('un error en el panel no oculta la lista y uno en la lista no oculta el panel', async () => {
    usarLista(() => HttpResponse.json(pagina([entreEstrategias])))
    server.use(http.get('/api/senales/aciertos', () => error(500, 'No se pudieron medir los aciertos.', 'corr-aciertos')))
    const { unmount } = renderSenales()

    const aviso = await screen.findByRole('alert')
    expect(aviso).toHaveTextContent('No se pudieron medir los aciertos.')
    expect(aviso).toHaveTextContent('correlationId: corr-aciertos')
    expect(screen.getByRole('link', { name: 'Abde Rebbach' })).toBeInTheDocument()
    unmount()

    server.resetHandlers()
    usarLista(() => error(500, 'No se pudieron cargar las señales.', 'corr-lista'))
    renderSenales()
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar las señales.')
    expect(await screen.findByText('Todavía no hay semanas para medir: las señales se evalúan una semana después de generarse.')).toBeInTheDocument()
  })

  it('las vigentes que se muestran quedan vistas y no alertan al salir de la lista', async () => {
    const user = userEvent.setup()
    server.use(http.get('/api/senales', () => HttpResponse.json(pagina([entreEstrategias, precioAtrasado]))))
    renderSenales()

    const enlace = await screen.findByRole('link', { name: 'Abde Rebbach' })
    await waitFor(() => expect(JSON.parse(window.sessionStorage.getItem('formula11.senales.vistas.11'))).toEqual([269, 270]))
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()

    await user.click(enlace)
    expect(await screen.findByRole('heading', { name: 'Detalle del jugador' })).toBeInTheDocument()
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()
  })
})
