import { useMemo } from "react";
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip, ReferenceLine, ReferenceDot, Legend
} from "recharts";

// (форматирование дат делаем на месте через toLocaleString)

/**
 * props:
 *  - entries: [{ id, at, weightKg }]
 *  - targetWeightKg: number | null
 *  - initialWeightKg: number | null
 */
export default function WeightChart({ entries = [], targetWeightKg = null, initialWeightKg = null }) {
  // подготовим данные для Recharts и найдём milestone-точки каждые 5%
  const { data, milestones } = useMemo(() => {
    if (!entries?.length) return { data: [], milestones: [] };

    const sorted = [...entries].sort((a, b) => new Date(a.at) - new Date(b.at));

    // initial/target
    const initW = (typeof initialWeightKg === "number")
      ? initialWeightKg
      : (sorted[0]?.weightKg ?? null);
    const tgtW  = (typeof targetWeightKg  === "number") ? targetWeightKg : null;

    // вставим baseline-точку за 1 минуту до самой ранней записи, если initW известен
    const firstAt = new Date(sorted[0].at);
    const baselineAt = new Date(firstAt.getTime() - 60 * 1000).toISOString();
    const withBaseline = (initW != null)
      ? [{ id: "baseline", at: baselineAt, weightKg: initW }, ...sorted]
      : sorted;

    const progressPercent = (w) => {
      if (initW == null || tgtW == null) return null;
      const total = Math.abs(initW - tgtW);
      const covered = Math.abs(initW - w);
      return total === 0 ? 100 : Math.max(0, Math.min(100, (covered / total) * 100));
    };

    let lastBucket = -1;
    const mstones = [];

    const mapped = withBaseline.map((e) => {
      const p = progressPercent(e.weightKg);
      if (p != null) {
        const bucket = Math.floor(p / 5);
        if (bucket > lastBucket) {
          lastBucket = bucket;
          if (bucket > 0) {
            mstones.push({ at: e.at, label: `${bucket * 5}%`, weightKg: e.weightKg });
          }
        }
      }
      return { atISO: e.at, weight: e.weightKg, progress: p };
    });

    return { data: mapped, milestones: mstones };
  }, [entries, targetWeightKg, initialWeightKg]);

  if (!data.length) {
    return <div style={{ padding: 12, border: "1px solid #eee", borderRadius: 8 }}>Нет данных веса</div>;
  }

  // границы оси Y c небольшим отступом
  const minW = Math.min(...data.map(d => d.weight));
  const maxW = Math.max(...data.map(d => d.weight));
  const pad  = Math.max(1, Math.round((maxW - minW) * 0.1));

  return (
    <div style={{ width: "100%", height: 320 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 10 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="atISO"
            tickFormatter={(iso) => new Date(iso).toLocaleString()}
          />
          <YAxis domain={[Math.floor(minW - pad), Math.ceil(maxW + pad)]} unit=" kg" />
          <Tooltip
            labelFormatter={(iso) => new Date(iso).toLocaleString()}
            formatter={(val, name) => {
              // name у линии задан "Weight" (с заглавной) — учитываем оба варианта
              if (name === "Weight" || name === "weight") return [`${val} kg`, "Weight"];
              if (name === "progress") return [val != null ? `${val.toFixed(1)}%` : "—", "Progress"];
              return [val, name];
            }}
          />
          <Legend />

          {/* линия веса */}
          <Line type="monotone" dataKey="weight" name="Weight" dot={true} />

          {/* вехи 5% */}
          {milestones.map(ms => (
            <ReferenceDot key={ms.at} x={ms.at} y={ms.weightKg} r={5}
                          label={{ value: ms.label, position: "top" }} />
          ))}

          {/* линия цели */}
          {typeof targetWeightKg === "number" && (
            <ReferenceLine y={targetWeightKg} ifOverflow="extendDomain" strokeDasharray="4 4" label="Target" />
          )}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
