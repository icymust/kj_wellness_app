import { ResponsiveContainer, RadialBarChart, RadialBar, PolarAngleAxis } from "recharts";

export default function WellnessGauge({ value = 0 }) {
  const v = Math.max(0, Math.min(100, Number(value) || 0));
  const data = [{ name: "wellness", value: v, fill: "#8884d8" }];

  return (
    <div style={{ width: "100%", height: 220 }}>
      <ResponsiveContainer>
        <RadialBarChart
          innerRadius="70%"
          outerRadius="100%"
          data={data}
          startAngle={180}
          endAngle={0}
        >
          <PolarAngleAxis
            type="number"
            domain={[0, 100]}
            angleAxisId={0}
            tick={false}
          />
          <RadialBar
            minAngle={15}
            background
            clockWise
            dataKey="value"
            cornerRadius={10}
          />
        </RadialBarChart>
      </ResponsiveContainer>
      <div style={{ textAlign: "center", marginTop: -40, fontSize: 24, fontWeight: 700 }}>
        {v}
      </div>
      <div style={{ textAlign: "center", color: "#666" }}>Wellness</div>
    </div>
  );
}
