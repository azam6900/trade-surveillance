import { CartesianGrid, Legend, ResponsiveContainer, Scatter, ScatterChart, Tooltip, XAxis, YAxis } from 'recharts';
import type { EvidenceEvent } from '../types';

const COLORS = { NEW: '#0d6efd', CANCEL: '#dc3545', FILL: '#198754' } as const;   // Bootstrap blue, red, green
const SHAPES = { NEW: 'circle', CANCEL: 'cross', FILL: 'diamond' } as const;     // shape as well as colour
const ACTIONS = ['NEW', 'CANCEL', 'FILL'] as const;

const clock = (ms: number) => new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

export default function EvidenceChart({ events }: { events: EvidenceEvent[] }) {
    // Convert ISO strings to numbers once; a numeric axis needs numbers
    const points = events.map((e) => ({ t: Date.parse(e.ts), qty: e.qty, action: e.action, side: e.side }));

    return (
        <div role="img" aria-label={`Scatter chart of ${events.length} order events by time and quantity`}>
            <ResponsiveContainer width="100%" height={260}>
                <ScatterChart margin={{ top: 10, right: 20, bottom: 10, left: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="t" type="number" name="Time" domain={['dataMin - 500', 'dataMax + 500']} tickFormatter={clock} />
                    <YAxis dataKey="qty" type="number" name="Quantity" />
                    <Tooltip formatter={(value, name) => (name === 'Time' ? clock(Number(value)) : Number(value).toLocaleString())} />
                    <Legend />
                    {ACTIONS.map((action) => (
                        <Scatter key={action} name={action} data={points.filter((p) => p.action === action)}
                                 fill={COLORS[action]} shape={SHAPES[action]} />
                    ))}
                </ScatterChart>
            </ResponsiveContainer>
        </div>
    );
}