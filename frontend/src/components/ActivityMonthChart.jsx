import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from "recharts";

function daysInMonthUTC(year, month) {
  // month: 1..12
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

export default function ActivityMonthChart({ byDayMinutes = {}, year, month }) {
  const dim = daysInMonthUTC(year, month);
  const data = Array.from({ length: dim }, (_, i) => {
    const day = i + 1;
    const minutes = byDayMinutes[day] || 0;
    return { day, minutes };
  });

  const max = Math.max(...data.map(d => d.minutes), 60);

  return (
    <div style={{ width: "100%", height: 300 }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="day" tickCount={Math.min(dim, 10)} />
          <YAxis domain={[0, Math.ceil(max / 30) * 30]} unit=" min" />
          <Tooltip formatter={(v) => [`${v} min`, "Minutes"]} />
          <Bar dataKey="minutes" name="Minutes" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
