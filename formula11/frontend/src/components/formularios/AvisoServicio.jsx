import AvisoError from '../comun/AvisoError'

export default function AvisoServicio({ error }) {
  return <AvisoError error={error} />
}
