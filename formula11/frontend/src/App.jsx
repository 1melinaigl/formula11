import { BrowserRouter } from 'react-router-dom'
import Encabezado from './components/navegacion/Encabezado'
import CanchaFondo from './components/comun/CanchaFondo'
import AlertaSenales from './components/senales/AlertaSenales'
import AppRouter from './router/AppRouter'

function App() {
  return (
    <BrowserRouter>
      <CanchaFondo />
      <div id="app-shell">
        <Encabezado />
        <AlertaSenales />
        <main className="app-main">
          <AppRouter />
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
