import React from 'react';
import { useSelector } from 'react-redux';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { TrendingUp } from 'lucide-react';

export default function SpendingChart() {
  const { list: payments } = useSelector(state => state.payments);

  // Group or format payments for chart (last 10 settled or all recent payments)
  const chartData = payments
    .slice(0, 10)
    .reverse()
    .map((p, idx) => ({
      name: `#${p.id.substring(0, 6)}`,
      amount: parseFloat(p.amount),
      status: p.status,
      time: new Date(p.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }));

  const totalVolume = payments
    .filter(p => p.status === 'SETTLED')
    .reduce((sum, p) => sum + parseFloat(p.amount), 0);

  return (
    <div className="glass-panel" style={{ padding: 20, marginBottom: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <div>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: 6 }}>
            <TrendingUp size={16} color="var(--cyan)" /> Transaction Volume &amp; Spending
          </h3>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: 0 }}>
            Recent payments flow across microservices
          </p>
        </div>
        <div style={{ textAlign: 'right' }}>
          <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>
            Settled Volume
          </span>
          <div className="mono" style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--success)' }}>
            ₹{totalVolume.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
        </div>
      </div>

      <div style={{ height: 160, width: '100%' }}>
        {chartData.length === 0 ? (
          <div style={{
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '0.8rem',
            color: 'var(--text-dim)'
          }}>
            No transaction data available for chart
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="spendingGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="var(--primary)" stopOpacity={0.6}/>
                  <stop offset="95%" stopColor="var(--primary)" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
              <XAxis dataKey="time" stroke="var(--text-dim)" fontSize={11} tickLine={false} />
              <YAxis stroke="var(--text-dim)" fontSize={11} tickLine={false} />
              <Tooltip
                contentStyle={{
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.75rem',
                  color: 'var(--text-main)',
                  boxShadow: 'var(--shadow-md)'
                }}
                formatter={(val) => [`₹${Number(val).toLocaleString()}`, 'Amount']}
              />
              <Area
                type="monotone"
                dataKey="amount"
                stroke="var(--primary)"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#spendingGrad)"
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}
